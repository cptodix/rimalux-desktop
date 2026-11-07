package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.BuildInfo
import com.cptodix.rimalux.desktop.data.SettingsFileDto
import com.cptodix.rimalux.desktop.data.SettingsMapper
import com.cptodix.rimalux.desktop.data.SettingsStore
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

/**
 * Writes a support bundle with system information, redacted settings, monitor/backend state, and
 * the tail of the application log. No secrets are included; custom commands are redacted.
 */
object DiagnosticsBundle {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun write(
        paths: PlatformPaths,
        store: SettingsStore,
        target: Path = paths.stateDir.resolve(
            "rimalux-diagnostics-${Instant.now().epochSecond}.zip"
        ),
        runner: ProcessRunner = ProcessRunner()
    ): Path {
        paths.ensureDirectories()
        target.parent?.let { Files.createDirectories(it) }
        val settings = store.snapshot()
        val monitors = runCatching { DefaultMonitorProvider(runner).listMonitors() }
            .getOrDefault(emptyList())
        val backend = runCatching { WallpaperBackendFactory(runner, paths).detect(settings) }
            .getOrNull()

        ZipOutputStream(Files.newOutputStream(target)).use { zip ->
            put(zip, "system.txt", systemReport(paths))
            put(zip, "settings.redacted.json", redactedSettings(settings))
            put(zip, "monitors.txt", monitorReport(monitors))
            put(zip, "backend.txt", backendReport(backend))
            if (Files.exists(paths.logFile)) {
                val text = runCatching { Files.readString(paths.logFile) }.getOrDefault("")
                put(zip, "rimalux.log", text.takeLast(MAX_LOG_CHARS))
            }
        }
        return target
    }

    private fun systemReport(paths: PlatformPaths): String = buildString {
        appendLine("Rimalux Desktop ${BuildInfo.VERSION}")
        appendLine("Engine commit: ${BuildInfo.engineCommit}")
        appendLine("Timestamp: ${Instant.now()}")
        appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
        appendLine("Arch: ${System.getProperty("os.arch")}")
        appendLine("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
        appendLine("Locale: ${Locale.getDefault().toLanguageTag()}")
        appendLine("XDG_CURRENT_DESKTOP: ${System.getenv("XDG_CURRENT_DESKTOP").orEmpty()}")
        appendLine("DESKTOP_SESSION: ${System.getenv("DESKTOP_SESSION").orEmpty()}")
        appendLine("XDG_SESSION_TYPE: ${System.getenv("XDG_SESSION_TYPE").orEmpty()}")
        appendLine("WAYLAND_DISPLAY: ${System.getenv("WAYLAND_DISPLAY").orEmpty()}")
        appendLine("DISPLAY: ${System.getenv("DISPLAY").orEmpty()}")
        appendLine("Config: ${paths.configFile}")
        appendLine("Cache: ${paths.cacheDir}")
        appendLine("Pictures: ${paths.picturesDir}")
        appendLine("Logs: ${paths.logFile}")
        appendLine("Autostart: ${paths.autostartFile}")
    }

    private fun redactedSettings(settings: com.cptodix.rimalux.desktop.model.DesktopSettings): String {
        val dto = SettingsMapper.toDto(settings).let { file: SettingsFileDto ->
            file.copy(
                ui = file.ui.copy(
                    customWallpaperCommand = if (file.ui.customWallpaperCommand.isBlank()) {
                        ""
                    } else {
                        "[redacted]"
                    }
                )
            )
        }
        return json.encodeToString(SettingsFileDto.serializer(), dto)
    }

    private fun monitorReport(monitors: List<com.cptodix.rimalux.desktop.model.MonitorInfo>): String =
        buildString {
            appendLine("Monitors: ${monitors.size}")
            monitors.forEachIndexed { index, monitor ->
                appendLine(
                    "[$index] ${monitor.name} ${monitor.widthPx}x${monitor.heightPx} " +
                        "bounds=${monitor.bounds.x},${monitor.bounds.y}," +
                        "${monitor.bounds.width},${monitor.bounds.height} " +
                        "scale=${monitor.scale} primary=${monitor.isPrimary} " +
                        "plasmaScreenIndex=${monitor.plasmaScreenIndex}"
                )
            }
        }

    private fun backendReport(backend: WallpaperBackend?): String = buildString {
        if (backend == null) {
            appendLine("Backend: unavailable")
        } else {
            appendLine("Backend: ${backend.info.displayName} (${backend.info.id})")
            appendLine("Available: ${backend.info.available}")
            appendLine("Per-monitor: ${backend.info.supportsPerMonitor}")
            appendLine("Detail: ${backend.info.detail}")
        }
    }

    private fun put(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private const val MAX_LOG_CHARS = 512_000
}
