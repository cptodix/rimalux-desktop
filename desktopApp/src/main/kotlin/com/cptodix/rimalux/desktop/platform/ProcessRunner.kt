package com.cptodix.rimalux.desktop.platform

import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

data class ProcessResult(
    val command: List<String>,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean
) {
    val success: Boolean get() = !timedOut && exitCode == 0
}

/**
 * Safe subprocess runner. Commands are always passed as argument lists; no shell interpolation.
 */
class ProcessRunner(
    private val timeoutSeconds: Long = 20L
) {
    fun run(command: List<String>): ProcessResult {
        if (command.isEmpty()) {
            return ProcessResult(command, -1, "", "Empty command", false)
        }
        AppLog.info("Running command: ${command.joinToString(" ")}")
        return runCatching {
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

            val stdoutFuture = CompletableFuture.supplyAsync {
                process.inputStream.bufferedReader().use { it.readText() }
            }
            val stderrFuture = CompletableFuture.supplyAsync {
                process.errorStream.bufferedReader().use { it.readText() }
            }

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                process.waitFor(2, TimeUnit.SECONDS)
            }
            val stdout = runCatching { stdoutFuture.get(2, TimeUnit.SECONDS) }.getOrDefault("")
            val stderr = runCatching { stderrFuture.get(2, TimeUnit.SECONDS) }.getOrDefault("")
            val result = ProcessResult(
                command = command,
                exitCode = if (finished) process.exitValue() else -1,
                stdout = stdout,
                stderr = stderr,
                timedOut = !finished
            )
            if (result.success) {
                AppLog.info("Command succeeded: ${command.firstOrNull()}")
            } else {
                AppLog.warning(
                    "Command failed (${result.exitCode}${if (result.timedOut) ", timeout" else ""}): " +
                        command.joinToString(" ") + "\n" + stderr.take(2000)
                )
            }
            result
        }.getOrElse { error ->
            AppLog.error("Could not run command: ${command.joinToString(" ")}", error)
            ProcessResult(command, -1, "", error.localizedMessage ?: "Unknown error", false)
        }
    }

    fun findExecutable(name: String): String? {
        val path = System.getenv("PATH").orEmpty()
        if (path.isBlank()) return null
        val separators = File.pathSeparatorChar
        val suffixes = if (isWindows()) {
            listOf("", ".exe", ".cmd", ".bat")
        } else {
            listOf("")
        }
        for (directory in path.split(separators)) {
            if (directory.isBlank()) continue
            for (suffix in suffixes) {
                val candidate = File(directory, name + suffix)
                if (candidate.isFile && candidate.canExecute()) {
                    return candidate.absolutePath
                }
            }
        }
        return null
    }

    fun hasExecutable(name: String): Boolean = findExecutable(name) != null

    private fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")
}
