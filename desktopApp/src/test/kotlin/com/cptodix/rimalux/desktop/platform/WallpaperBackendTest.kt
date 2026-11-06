package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.WallpaperFile
import com.cptodix.rimalux.desktop.model.WallpaperItem
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperBackendTest {

    @Test
    fun kdePlasmaScript_mapsScreenIndexesAndEscapesUris() {
        val backend = KdeWallpaperBackend(ProcessRunner())
        val item0 = testItem(screenIndex = 0, monitorName = "Display One")
        val item1 = testItem(screenIndex = 1, monitorName = "Display Two")
        val script = backend.buildPlasmaScript(
            files = listOf(
                WallpaperFile(item0, Paths.get("/tmp/a b/one.png"), 85),
                WallpaperFile(item1, Paths.get("/tmp/two.png"), 85)
            )
        )
        assertTrue(script.contains("images[0]"))
        assertTrue(script.contains("images[1]"))
        assertTrue(script.contains("file:///tmp/a%20b/one.png"))
        assertTrue(script.contains("FillMode\", 2"))
        assertTrue(script.contains("d.wallpaperPlugin"))
    }

    @Test
    fun customCommandTokenizer_supportsQuotesAndPlaceholders() {
        val backend = CustomCommandBackend(ProcessRunner(), "feh --bg-fill \"{file}\"")
        val tokens = backend.tokenize("feh --bg-fill \"{file}\" --output {monitor}")
        assertTrue(tokens.contains("{file}"))
        assertTrue(tokens.contains("{monitor}"))
        assertTrue(tokens.contains("--bg-fill"))
    }

    private fun testItem(screenIndex: Int, monitorName: String) = WallpaperItem(
        monitorId = "monitor-$screenIndex",
        monitorName = monitorName,
        screenIndex = screenIndex,
        seed = 1L,
        pattern = ArtPattern.SILK_RIBBONS,
        paletteColors = listOf(0xFF00F5D4),
        colorFillMode = ColorFillMode.GRADIENT,
        pitchBlackRatio = 0.85f,
        intricacy = 0.5f,
        patternWeightMode = PatternWeightMode.NORMAL,
        widthPx = 1920,
        heightPx = 1080
    )
}
