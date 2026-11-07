package com.cptodix.rimalux.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.data.SettingsStore
import com.cptodix.rimalux.desktop.model.AppLanguage
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.platform.AppLog
import com.cptodix.rimalux.desktop.platform.DefaultMonitorProvider
import com.cptodix.rimalux.desktop.platform.DesktopTray
import com.cptodix.rimalux.desktop.platform.DiagnosticsBundle
import com.cptodix.rimalux.desktop.platform.OsScheduler
import com.cptodix.rimalux.desktop.platform.PlatformPaths
import com.cptodix.rimalux.desktop.platform.ProcessRunner
import com.cptodix.rimalux.desktop.platform.SingleInstanceManager
import com.cptodix.rimalux.desktop.platform.TrayLabels
import com.cptodix.rimalux.desktop.platform.UpdateChecker
import com.cptodix.rimalux.desktop.platform.WallpaperBackendFactory
import com.cptodix.rimalux.desktop.render.SkiaWallpaperRenderer
import com.cptodix.rimalux.desktop.resources.Res
import com.cptodix.rimalux.desktop.resources.rimalux_icon
import com.cptodix.rimalux.desktop.ui.RimaluxDesktopApp
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.desktop.ui.options.OptionsPage
import com.cptodix.rimalux.desktop.ui.theme.RimaluxTheme
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.StarterPalettes
import com.cptodix.rimalux.oled.wallpaper.engine.WallpaperGeneratorEngine
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import javax.imageio.ImageIO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.painterResource

fun main(args: Array<String>) {
    val paths = PlatformPaths()
    AppLog.initialize(paths)
    val store = SettingsStore(paths)
    val initialLanguage = AppLanguage.fromTagOrDefault(store.snapshot().ui.languageTag)
    Locale.setDefault(initialLanguage.toLocale())

    when {
        args.contains("--version") -> {
            println("Rimalux Desktop ${BuildInfo.VERSION}")
            println("Engine ${BuildInfo.engineCommit}")
            return
        }

        args.contains("--list-monitors") -> {
            printDiagnostics(paths, store)
            return
        }

        args.contains("--diagnostics") -> {
            printDiagnostics(paths, store)
            return
        }

        args.contains("--test-backend") -> {
            printBackendTest(paths, store)
            return
        }

        args.contains("--diagnostics-bundle") -> {
            val index = args.indexOf("--diagnostics-bundle")
            val target = args.getOrNull(index + 1)
                ?.takeIf { !it.startsWith("--") }
                ?.let { Paths.get(it) }
            val bundle = if (target != null) {
                DiagnosticsBundle.write(paths, store, target)
            } else {
                DiagnosticsBundle.write(paths, store)
            }
            println("Diagnostics bundle: ${bundle.toAbsolutePath()}")
            return
        }

        args.contains("--check-updates") -> {
            val result = runBlocking { UpdateChecker().check() }
            result.onSuccess { info ->
                if (info.updateAvailable) {
                    println(
                        "Update available: ${info.latestVersion} " +
                            "(current ${info.currentVersion}) ${info.releaseUrl}"
                    )
                } else {
                    println("Rimalux Desktop is up to date (${info.currentVersion}).")
                }
            }.onFailure { error ->
                println("Update check failed: ${error.localizedMessage ?: "Unknown error"}")
            }
            return
        }

        args.contains("--install-os-scheduler") -> {
            val index = args.indexOf("--install-os-scheduler")
            val minutes = args.getOrNull(index + 1)
                ?.toIntOrNull()
                ?: store.snapshot().autoChange.intervalMinutes
            OsScheduler(paths).install(minutes)
                .onSuccess { println("OS scheduler installed every $minutes minute(s).") }
                .onFailure { println("Could not install OS scheduler: ${it.localizedMessage}") }
            return
        }

        args.contains("--remove-os-scheduler") -> {
            OsScheduler(paths).remove()
                .onSuccess { println("OS scheduler removed.") }
                .onFailure { println("Could not remove OS scheduler: ${it.localizedMessage}") }
            return
        }

        args.contains("--render-test") -> {
            val index = args.indexOf("--render-test")
            val output = args.getOrNull(index + 1) ?: "build/render-test"
            runRenderTest(Paths.get(output))
            return
        }

        args.contains("--rotate-now") -> {
            val controller = StudioController(paths = paths, store = store)
            val result = runBlocking {
                withTimeout(60_000L) {
                    controller.uiState.first { it.current != null }
                }
                controller.performAutoRotation(force = true)
            }
            if (result == null) {
                println("Rimalux: no rotation result")
            } else {
                println(
                    "Rimalux rotation: success=${result.success} " +
                        "applied=${result.appliedCount} message=${result.message}"
                )
                result.errors.forEach { println("  $it") }
            }
            controller.shutdown()
            return
        }
    }

    val startBackground = args.contains("--background")
    val singleInstance = SingleInstanceManager(paths)
    if (!singleInstance.acquire()) {
        singleInstance.notifyExisting()
        return
    }

    val controller = StudioController(paths = paths, store = store)
    application {
        val state by controller.uiState.collectAsState()
        var optionsOpen by remember { mutableStateOf(false) }
        var optionsPage by remember { mutableStateOf(OptionsPage.NONE) }
        var windowVisible by remember { mutableStateOf(!startBackground) }
        var focusRequest by remember { mutableStateOf(0) }
        var fullscreen by remember { mutableStateOf(false) }
        var tray by remember { mutableStateOf<DesktopTray?>(null) }
        val trayLabels = TrayLabels(
            tooltip = "Rimalux",
            open = localizedStringOr("desktop_tray_open", "Open Rimalux Desktop"),
            autoChange = localizedStringOr("auto_master_toggle", "Auto-Change Wallpaper"),
            quit = localizedStringOr("desktop_tray_quit", "Quit Rimalux")
        )
        val quitWarning = localizedStringOr(
            "desktop_quit_warning",
            "Auto-change is enabled. Quit Rimalux and stop background rotation?"
        )
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(
                state.ui.windowWidth.coerceAtLeast(1024).dp,
                state.ui.windowHeight.coerceAtLeast(700).dp
            )
        )

        fun quitApp(confirmIfAutoChange: Boolean = false) {
            if (confirmIfAutoChange && controller.uiState.value.autoChange.enabled) {
                val choice = javax.swing.JOptionPane.showConfirmDialog(
                    null,
                    quitWarning,
                    "Rimalux",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE
                )
                if (choice != javax.swing.JOptionPane.YES_OPTION) return
            }
            tray?.remove()
            controller.shutdown()
            singleInstance.close()
            exitApplication()
        }

        fun showWindow() {
            windowState.isMinimized = false
            windowVisible = true
            focusRequest += 1
        }

        LaunchedEffect(Unit) {
            singleInstance.showSignal.collect {
                showWindow()
            }
        }
        LaunchedEffect(trayLabels, state.autoChange.enabled) {
            val current = tray
            if (current == null) {
                val bytes = runCatching { Res.readBytes("drawable/rimalux_icon.png") }.getOrNull()
                val image = bytes?.let {
                    runCatching { ImageIO.read(ByteArrayInputStream(it)) }.getOrNull()
                }
                if (image != null) {
                    val created = DesktopTray(
                        image = image,
                        onOpen = { showWindow() },
                        onToggleAutoChange = {
                            val auto = controller.uiState.value.autoChange
                            controller.setAutoChangeEnabled(!auto.enabled)
                        },
                        onQuit = { quitApp(confirmIfAutoChange = true) }
                    )
                    created.install(trayLabels, state.autoChange.enabled)
                    tray = created
                    if (startBackground) {
                        windowVisible = !created.isInstalled
                    }
                } else if (startBackground) {
                    windowVisible = true
                }
            } else {
                current.update(trayLabels, state.autoChange.enabled)
            }
        }
        DisposableEffect(Unit) {
            onDispose { tray?.remove() }
        }

        Window(
            onCloseRequest = {
                val keepRunning = state.autoChange.enabled || state.autoChange.keepInTray
                if (keepRunning && tray?.isInstalled == true) {
                    windowVisible = false
                } else {
                    quitApp()
                }
            },
            state = windowState,
            visible = windowVisible,
            title = "Rimalux",
            icon = painterResource(Res.drawable.rimalux_icon),
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    val modifier = event.isCtrlPressed || event.isMetaPressed
                    when {
                        event.key == Key.F11 -> {
                            fullscreen = !fullscreen
                            windowState.placement = if (fullscreen) {
                                WindowPlacement.Fullscreen
                            } else {
                                WindowPlacement.Floating
                            }
                            true
                        }
                        event.key == Key.Escape && fullscreen -> {
                            fullscreen = false
                            windowState.placement = WindowPlacement.Floating
                            true
                        }
                        event.key == Key.Escape && optionsOpen -> {
                            optionsOpen = false
                            optionsPage = OptionsPage.NONE
                            true
                        }
                        event.key == Key.Spacebar -> {
                            controller.next()
                            true
                        }
                        event.key == Key.DirectionLeft -> {
                            controller.previous()
                            true
                        }
                        event.key == Key.DirectionRight -> {
                            controller.next()
                            true
                        }
                        modifier && event.key == Key.S -> {
                            controller.savePngs(ApplyTarget.ALL_DISPLAYS)
                            true
                        }
                        modifier && event.key == Key.Enter -> {
                            controller.applyWallpaper(ApplyTarget.ALL_DISPLAYS)
                            true
                        }
                        modifier && event.key == Key.Comma -> {
                            optionsOpen = true
                            optionsPage = OptionsPage.SETTINGS
                            true
                        }
                        else -> false
                    }
                }
            }
        ) {
            LaunchedEffect(focusRequest, windowVisible) {
                if (windowVisible) {
                    window.toFront()
                    window.requestFocus()
                }
            }
            key(state.languageTag) {
                RimaluxTheme(
                    paletteColors = state.selectedRenderedItem?.item?.paletteColors
                        ?: state.current?.set?.items?.firstOrNull()?.paletteColors
                ) {
                    RimaluxDesktopApp(
                        state = state,
                        controller = controller,
                        optionsOpen = optionsOpen,
                        optionsPage = optionsPage,
                        fullscreen = fullscreen,
                        onOptionsOpenChange = { optionsOpen = it },
                        onOptionsPageChange = { optionsPage = it },
                        onToggleFullscreen = {
                            fullscreen = !fullscreen
                            windowState.placement = if (fullscreen) {
                                WindowPlacement.Fullscreen
                            } else {
                                WindowPlacement.Floating
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun printBackendTest(paths: PlatformPaths, store: SettingsStore) {
    val settings = store.snapshot()
    val runner = ProcessRunner()
    val monitors = DefaultMonitorProvider(runner).listMonitors()
    val backend = WallpaperBackendFactory(runner, paths).detect(settings)
    println("Backend: ${backend.info.displayName} (${backend.info.id})")
    println("Available: ${backend.info.available}")
    println("Per-monitor: ${backend.info.supportsPerMonitor}")
    println("Detail: ${backend.info.detail}")
    println("Monitors: ${monitors.size}")
    monitors.forEachIndexed { index, monitor ->
        println(
            "  [$index] ${monitor.name} ${monitor.widthPx}x${monitor.heightPx} " +
                "primary=${monitor.isPrimary}"
        )
    }
    println(
        if (backend.info.available) {
            "Backend looks usable. No wallpaper was changed by this check."
        } else {
            "Backend is not available. PNG export still works."
        }
    )
}

private fun printDiagnostics(paths: PlatformPaths, store: SettingsStore) {
    val settings = store.snapshot()
    val runner = ProcessRunner()
    val monitors = DefaultMonitorProvider(runner).listMonitors()
    val backend = WallpaperBackendFactory(runner, paths).detect(settings)
    println("Rimalux Desktop ${BuildInfo.VERSION}")
    println("Engine commit: ${BuildInfo.engineCommit}")
    println("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
    println("Arch: ${System.getProperty("os.arch")}")
    println("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
    println("Locale: ${Locale.getDefault().toLanguageTag()}")
    println("XDG_CURRENT_DESKTOP: ${System.getenv("XDG_CURRENT_DESKTOP").orEmpty()}")
    println("Session: ${System.getenv("XDG_SESSION_TYPE").orEmpty()}")
    println("Config: ${paths.configFile}")
    println("Cache: ${paths.cacheDir}")
    println("Pictures: ${paths.picturesDir}")
    println("Logs: ${paths.logFile}")
    println("Backend: ${backend.info.displayName} (available=${backend.info.available}, perMonitor=${backend.info.supportsPerMonitor})")
    println("Monitors: ${monitors.size}")
    monitors.forEachIndexed { index, monitor ->
        println(
            "  [$index] ${monitor.name} ${monitor.widthPx}x${monitor.heightPx} " +
                "at ${monitor.bounds.x},${monitor.bounds.y} primary=${monitor.isPrimary} " +
                "plasmaScreen=${monitor.plasmaScreenIndex}"
        )
    }
}

private fun runRenderTest(outputDir: Path) {
    Files.createDirectories(outputDir)
    val palette = StarterPalettes.defaults.first().colors
    val patterns = listOf(
        ArtPattern.SILK_RIBBONS,
        ArtPattern.PENROSE_QUASICRYSTAL,
        ArtPattern.SACRED_MANDALA,
        ArtPattern.CRYSTAL_VORONOI
    )
    println("Rendering ${patterns.size} test wallpapers to ${outputDir.toAbsolutePath()}")
    for (pattern in patterns) {
        val synthesized = WallpaperGeneratorEngine.synthesize(
            seed = 42_000L + pattern.ordinal,
            spec = com.cptodix.rimalux.oled.wallpaper.engine.CoreGenerationSpec(
                patternsRandomized = false,
                selectedPatterns = setOf(pattern),
                patternWeightMode = PatternWeightMode.NORMAL,
                colorFillMode = ColorFillMode.GRADIENT,
                pitchBlackRatio = 0.85f
            ),
            availablePalettes = StarterPalettes.defaults,
            aspectRatio = 9f / 16f
        )
        val rendered = SkiaWallpaperRenderer.renderAndMeasure(
            synthesized = synthesized,
            widthPx = 1920,
            heightPx = 1080
        )
        val target = outputDir.resolve("rimalux_${pattern.id}_1920x1080.png")
        SkiaWallpaperRenderer.writePng(rendered.image, target)
        runCatching { rendered.image.close() }
        println(
            "  ${pattern.id}: ${rendered.widthPx}x${rendered.heightPx} " +
                "measuredBlack=${rendered.measuredBlackPercent}% -> $target"
        )
    }
    println("Render test complete.")
}
