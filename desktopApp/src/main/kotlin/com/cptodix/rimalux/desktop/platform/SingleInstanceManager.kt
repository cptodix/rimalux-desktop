package com.cptodix.rimalux.desktop.platform

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-instance guard. The first process owns a file lock and a localhost socket; later
 * launches send SHOW to the running instance and exit.
 */
class SingleInstanceManager(
    private val paths: PlatformPaths = PlatformPaths()
) {
    private val _showSignal = MutableStateFlow(0L)
    val showSignal: StateFlow<Long> = _showSignal.asStateFlow()

    private var lockChannel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null
    private var acceptThread: Thread? = null

    fun acquire(): Boolean {
        paths.ensureDirectories()
        val channel = runCatching {
            FileChannel.open(
                paths.instanceLockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
        }.getOrElse { error ->
            AppLog.error("Could not open single-instance lock file", error)
            return false
        }
        val acquired = runCatching { channel.tryLock() }.getOrNull()
        if (acquired == null) {
            runCatching { channel.close() }
            return false
        }
        lockChannel = channel
        lock = acquired
        return runCatching {
            val socket = ServerSocket(0, 16, InetAddress.getLoopbackAddress())
            server = socket
            Files.writeString(paths.instancePortFile, socket.localPort.toString())
            acceptThread = Thread({ acceptLoop(socket) }, "rimalux-single-instance").apply {
                isDaemon = true
                start()
            }
            true
        }.getOrElse { error ->
            AppLog.error("Could not start single-instance socket", error)
            close()
            false
        }
    }

    fun notifyExisting(): Boolean {
        val port = runCatching {
            Files.readString(paths.instancePortFile).trim().toIntOrNull()
        }.getOrNull() ?: return false
        return runCatching {
            Socket(InetAddress.getLoopbackAddress(), port).use { socket ->
                socket.getOutputStream().bufferedWriter().use { writer ->
                    writer.write("SHOW\n")
                    writer.flush()
                }
            }
            true
        }.getOrDefault(false)
    }

    fun close() {
        runCatching { acceptThread?.interrupt() }
        runCatching { server?.close() }
        runCatching { lock?.release() }
        runCatching { lockChannel?.close() }
        runCatching { Files.deleteIfExists(paths.instancePortFile) }
        runCatching { Files.deleteIfExists(paths.instanceLockFile) }
        acceptThread = null
        server = null
        lock = null
        lockChannel = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            runCatching {
                socket.accept().use { client ->
                    val line = BufferedReader(InputStreamReader(client.getInputStream()))
                        .readLine()
                    if (line?.trim() == "SHOW") {
                        _showSignal.value += 1L
                    }
                }
            }.onFailure { error ->
                if (!socket.isClosed) {
                    AppLog.warning("Single-instance accept failed", error)
                }
            }
        }
    }
}
