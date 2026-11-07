package com.cptodix.rimalux.desktop

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
import com.cptodix.rimalux.desktop.platform.PlatformPaths
import com.cptodix.rimalux.desktop.platform.ProcessRunner
import com.cptodix.rimalux.desktop.platform.WallpaperBackendFactory
import com.cptodix.rimalux.desktop.render.SkiaWallpaperRenderer
import com.cptodix.rimalux.desktop.resources.Res
import com.cptodix.rimalux.desktop.resources.rimalux_icon
import com.cptodix.rimalux.desktop.ui.RimaluxDesktopApp
import com.cptodix.rimalux.desktop.ui.options.OptionsPage
import com.cptodix.rimalux.desktop.ui.theme.RimaluxTheme
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.StarterPalettes
import com.cptodix.rimalux.oled.wallpaper.engine.WallpaperGeneratorEngine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
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

        args.contains("--render-test") -> {
            val index = args.indexOf("--render-test")
            val output = args.getOrNull(index + 1) ?: "build/render-test"
            runRenderTest(Paths.get(output))
            return
        }
    }

    val controller = StudioController(paths = paths, store = store)
    application {
        val state by controller.uiState.collectAsState()
        var optionsOpen by remember { mutableStateOf(false) }
        var optionsPage by remember { mutableStateOf(OptionsPage.NONE) }
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(
                state.ui.windowWidth.coerceAtLeast(1024).dp,
                state.ui.windowHeight.coerceAtLeast(700).dp
            )
        )

        Window(
            onCloseRequest = {
                controller.shutdown()
                exitApplication()
            },
            state = windowState,
            title = "Rimalux",
            icon = painterResource(Res.drawable.rimalux_icon),
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    val modifier = event.isCtrlPressed || event.isMetaPressed
                    when {
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
                        onOptionsOpenChange = { optionsOpen = it },
                        onOptionsPageChange = { optionsPage = it }
                    )
                }
            }
        }
    }
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
