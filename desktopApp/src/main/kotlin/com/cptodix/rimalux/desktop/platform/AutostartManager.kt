package com.cptodix.rimalux.desktop.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.nio.file.Files
import java.nio.file.Path

/**
 * Start-at-login integration.
 *
 * Linux: XDG autostart `.desktop` file.
 * Windows: HKCU Run registry value.
 */
class AutostartManager(
    private val paths: PlatformPaths = PlatformPaths(),
    private val executablePath: String? = null,
    private val osName: String = System.getProperty("os.name").orEmpty()
) {
    private val isWindows = osName.lowercase().contains("win")
    private val isLinux = osName.lowercase().contains("linux")

    fun isSupported(): Boolean = isLinux || isWindows

    fun isEnabled(): Boolean = when {
        isLinux -> Files.exists(paths.autostartFile)
        isWindows -> runCatching {
            Advapi32Util.registryValueExists(
                WinReg.HKEY_CURRENT_USER,
                WINDOWS_RUN_KEY,
                WINDOWS_VALUE_NAME
            )
        }.getOrDefault(false)
        else -> false
    }

    fun setEnabled(enabled: Boolean): Result<Unit> = runCatching {
        if (!isSupported()) error("Start at Login is not supported on this platform")
        if (isLinux) setLinuxEnabled(enabled) else setWindowsEnabled(enabled)
    }

    fun ensureEnabled(): Result<Unit> = runCatching {
        if (isEnabled()) return@runCatching
        setEnabled(true).getOrThrow()
    }

    private fun setLinuxEnabled(enabled: Boolean) {
        val file = paths.autostartFile
        if (!enabled) {
            Files.deleteIfExists(file)
            return
        }
        val executable = resolveExecutable()
            ?: error("Start at Login is available in installed builds")
        Files.createDirectories(file.parent)
        Files.writeString(file, linuxDesktopEntry(executable))
    }

    private fun setWindowsEnabled(enabled: Boolean) {
        val executable = resolveExecutable()
            ?: error("Start at Login is available in installed builds")
        if (enabled) {
            Advapi32Util.registrySetStringValue(
                WinReg.HKEY_CURRENT_USER,
                WINDOWS_RUN_KEY,
                WINDOWS_VALUE_NAME,
                "\"$executable\" --background"
            )
        } else {
            runCatching {
                Advapi32Util.registryDeleteValue(
                    WinReg.HKEY_CURRENT_USER,
                    WINDOWS_RUN_KEY,
                    WINDOWS_VALUE_NAME
                )
            }
        }
    }

    private fun resolveExecutable(): String? {
        val explicit = executablePath?.takeIf { it.isNotBlank() }
        if (explicit != null) return explicit
        val jpackagePath = System.getProperty("jpackage.app-path")
            ?.takeIf { it.isNotBlank() }
        if (jpackagePath != null) return jpackagePath
        return null
    }

    private fun linuxDesktopEntry(executable: String): String = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=Rimalux")
        appendLine("Comment=Procedural OLED wallpaper studio")
        appendLine("Exec=${quoteExec(executable)} --background")
        appendLine("Icon=rimalux")
        appendLine("Terminal=false")
        appendLine("X-GNOME-Autostart-enabled=true")
    }

    private fun quoteExec(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "\"$escaped\""
    }

    companion object {
        private const val WINDOWS_RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        private const val WINDOWS_VALUE_NAME = "Rimalux"
    }
}
