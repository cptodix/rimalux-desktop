package com.cptodix.rimalux.desktop.platform

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Cross-platform application paths.
 *
 * Linux follows the XDG base directory spec where possible.
 * Windows uses APPDATA / LOCALAPPDATA / USERPROFILE.
 */
class PlatformPaths(
    val osName: String = System.getProperty("os.name").orEmpty(),
    private val environment: Map<String, String> = System.getenv(),
    userHome: Path = Paths.get(System.getProperty("user.home"))
) {
    val isWindows: Boolean = osName.lowercase().contains("win")
    val isLinux: Boolean = osName.lowercase().contains("linux")
    val isMac: Boolean = osName.lowercase().contains("mac")

    private fun env(name: String): String? = environment[name]?.takeIf { it.isNotBlank() }

    val configDir: Path = when {
        isWindows -> Paths.get(env("APPDATA") ?: userHome.resolve("AppData/Roaming").toString(), "Rimalux")
        else -> Paths.get(env("XDG_CONFIG_HOME") ?: userHome.resolve(".config").toString(), "rimalux")
    }

    val cacheDir: Path = when {
        isWindows -> Paths.get(env("LOCALAPPDATA") ?: userHome.resolve("AppData/Local").toString(), "Rimalux", "cache")
        else -> Paths.get(env("XDG_CACHE_HOME") ?: userHome.resolve(".cache").toString(), "rimalux")
    }

    val stateDir: Path = when {
        isWindows -> Paths.get(env("LOCALAPPDATA") ?: userHome.resolve("AppData/Local").toString(), "Rimalux", "state")
        else -> Paths.get(env("XDG_STATE_HOME") ?: userHome.resolve(".local/state").toString(), "rimalux")
    }

    val logsDir: Path = stateDir.resolve("logs")

    val picturesDir: Path = when {
        isWindows -> Paths.get(env("USERPROFILE") ?: userHome.toString(), "Pictures", "Rimalux")
        else -> Paths.get(env("XDG_PICTURES_DIR") ?: userHome.resolve("Pictures").toString(), "Rimalux")
    }

    val configFile: Path = configDir.resolve("settings.json")
    val cacheWallpapersDir: Path = cacheDir.resolve("wallpapers")
    val cachePreviewsDir: Path = cacheDir.resolve("previews")

    val logFile: Path = logsDir.resolve("rimalux.log")

    fun ensureDirectories() {
        listOf(configDir, cacheDir, stateDir, logsDir, picturesDir, cacheWallpapersDir, cachePreviewsDir)
            .forEach { dir -> runCatching { Files.createDirectories(dir) } }
    }

    fun displayPath(path: Path): String = path.toAbsolutePath().normalize().toString()
}
