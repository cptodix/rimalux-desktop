package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.WallpaperItem
import com.cptodix.rimalux.desktop.model.WallpaperSet
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.CoreGenerationSpec
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.StarterPalettes
import com.cptodix.rimalux.oled.wallpaper.engine.WallpaperGeneratorEngine
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperFileStoreTest {

    @Test
    fun renderSetAndSaveToPictures_producesLosslessPngs() {
        val root = Files.createTempDirectory("rimalux-export-test")
        val paths = PlatformPaths(
            osName = "Linux",
            environment = mapOf(
                "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                "XDG_STATE_HOME" to root.resolve("state").toString(),
                "XDG_PICTURES_DIR" to root.resolve("pictures").toString()
            ),
            userHome = root
        )
        val synthesized = WallpaperGeneratorEngine.synthesize(
            seed = 424242L,
            spec = CoreGenerationSpec(
                patternsRandomized = false,
                selectedPatterns = setOf(com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern.SILK_RIBBONS),
                colorFillMode = ColorFillMode.GRADIENT,
                pitchBlackRatio = 0.85f
            ),
            availablePalettes = StarterPalettes.defaults,
            aspectRatio = 9f / 16f
        )
        val item = WallpaperItem(
            monitorId = "test-monitor",
            monitorName = "Test Display",
            screenIndex = 0,
            seed = synthesized.seed,
            pattern = synthesized.pattern,
            paletteColors = synthesized.paletteColors,
            colorFillMode = synthesized.colorFillMode,
            pitchBlackRatio = synthesized.pitchBlackRatio,
            intricacy = synthesized.intricacy,
            patternWeightMode = PatternWeightMode.NORMAL,
            widthPx = 1920,
            heightPx = 1080
        )
        val set = WallpaperSet(
            id = "test_set",
            timestampMillis = 1_700_000_000_000L,
            items = listOf(item)
        )
        val store = WallpaperFileStore(paths)
        val files = store.renderSet(set)
        assertEquals(1, files.size)
        assertTrue(Files.exists(files.first().path))
        assertTrue(Files.size(files.first().path) > 10_000)
        assertTrue(files.first().measuredBlackPercent in 78..92)

        val saved = store.saveToPictures(files)
        assertEquals(1, saved.size)
        assertTrue(Files.exists(saved.first()))
        assertTrue(saved.first().fileName.toString().startsWith("Rimalux_silk_ribbons_"))
    }
}
