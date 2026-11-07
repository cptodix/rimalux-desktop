package com.cptodix.rimalux.desktop.platform

import java.nio.file.Files
import java.nio.file.Path

/**
 * Optional OS-level scheduler. The in-app tray scheduler remains the primary mechanism; this is
 * for users who want rotation even when Rimalux is not running.
 *
 * Linux uses a systemd user service + timer. Windows uses Task Scheduler.
 */
class OsScheduler(
    private val paths: PlatformPaths = PlatformPaths(),
    private val runner: ProcessRunner = ProcessRunner(),
    private val executablePath: String? = null,
    private val osName: String = System.getProperty("os.name").orEmpty()
) {
    private val isLinux = osName.lowercase().contains("linux")
    private val isWindows = osName.lowercase().contains("win")

    val systemdServiceFile: Path =
        paths.configDir.parent.resolve("systemd").resolve("user").resolve("rimalux-rotate.service")
    val systemdTimerFile: Path =
        paths.configDir.parent.resolve("systemd").resolve("user").resolve("rimalux-rotate.timer")

    fun isSupported(): Boolean = isLinux || isWindows

    fun isInstalled(): Boolean = when {
        isLinux -> Files.exists(systemdServiceFile) && Files.exists(systemdTimerFile)
        isWindows -> {
            val schtasks = runner.findExecutable("schtasks")
            schtasks != null && runner.run(
                listOf(schtasks, "/Query", "/TN", WINDOWS_TASK_NAME)
            ).success
        }
        else -> false
    }

    fun install(intervalMinutes: Int): Result<Unit> = runCatching {
        if (!isSupported()) error("OS scheduler is not supported on this platform")
        val minutes = intervalMinutes.coerceIn(1, 10_080)
        val executable = resolveExecutable()
            ?: error("OS scheduler is available in installed builds")
        if (isLinux) installLinux(executable, minutes) else installWindows(executable, minutes)
    }

    fun remove(): Result<Unit> = runCatching {
        if (isLinux) removeLinux() else if (isWindows) removeWindows()
    }

    private fun installLinux(executable: String, minutes: Int) {
        Files.createDirectories(systemdServiceFile.parent)
        Files.writeString(systemdServiceFile, buildService(executable))
        Files.writeString(systemdTimerFile, buildTimer(minutes))

        val systemctl = runner.findExecutable("systemctl")
            ?: error("systemctl is not available; unit files were written to ${systemdTimerFile.parent}")
        runner.run(listOf(systemctl, "--user", "daemon-reload"))
        val enable = runner.run(
            listOf(systemctl, "--user", "enable", "--now", "rimalux-rotate.timer")
        )
        if (!enable.success) {
            error("systemctl could not enable the timer: ${enable.stderr.ifBlank { enable.stdout }}")
        }
    }

    private fun removeLinux() {
        runner.findExecutable("systemctl")?.let { systemctl ->
            runner.run(listOf(systemctl, "--user", "disable", "--now", "rimalux-rotate.timer"))
        }
        Files.deleteIfExists(systemdTimerFile)
        Files.deleteIfExists(systemdServiceFile)
        runner.findExecutable("systemctl")?.let { systemctl ->
            runner.run(listOf(systemctl, "--user", "daemon-reload"))
        }
    }

    private fun installWindows(executable: String, minutes: Int) {
        val schtasks = runner.findExecutable("schtasks")
            ?: error("schtasks is not available")
        val result = runner.run(
            listOf(
                schtasks,
                "/Create",
                "/SC", "MINUTE",
                "/MO", minutes.toString(),
                "/TN", WINDOWS_TASK_NAME,
                "/TR", "\"$executable\" --rotate-now",
                "/F"
            )
        )
        if (!result.success) {
            error("schtasks failed: ${result.stderr.ifBlank { result.stdout }}")
        }
    }

    private fun removeWindows() {
        val schtasks = runner.findExecutable("schtasks") ?: return
        runner.run(listOf(schtasks, "/Delete", "/TN", WINDOWS_TASK_NAME, "/F"))
    }

    private fun resolveExecutable(): String? {
        val explicit = executablePath?.takeIf { it.isNotBlank() }
        if (explicit != null) return explicit
        return System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val WINDOWS_TASK_NAME = "RimaluxRotate"

        internal fun buildService(executable: String): String = buildString {
            appendLine("[Unit]")
            appendLine("Description=Rimalux wallpaper rotation")
            appendLine()
            appendLine("[Service]")
            appendLine("Type=oneshot")
            appendLine("ExecStart=${quote(executable)} --rotate-now")
        }

        internal fun buildTimer(intervalMinutes: Int): String = buildString {
            appendLine("[Unit]")
            appendLine("Description=Rimalux wallpaper rotation timer")
            appendLine()
            appendLine("[Timer]")
            appendLine("OnBootSec=2min")
            appendLine("OnUnitActiveSec=${intervalMinutes.coerceIn(1, 10_080)}min")
            appendLine("Persistent=true")
            appendLine("Unit=rimalux-rotate.service")
            appendLine()
            appendLine("[Install]")
            appendLine("WantedBy=timers.target")
        }

        private fun quote(value: String): String =
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
