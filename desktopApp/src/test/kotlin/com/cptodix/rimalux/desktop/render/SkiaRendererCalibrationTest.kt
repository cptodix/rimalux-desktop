package com.cptodix.rimalux.desktop.render

import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.MathPatternGenerator
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.RandomPaletteGenerator
import com.cptodix.rimalux.oled.wallpaper.engine.SynthesizedWallpaper
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pixel-level renderer calibration test. This is the guard that the desktop Skia renderer
 * matches the Android Canvas renderer's true-black behavior.
 */
class SkiaRendererCalibrationTest {

    @Test
    fun allEightyPatterns_skiaRendering_respectsTrueBlackRatioInBothWeightModes() {
        val palette = RandomPaletteGenerator.generate(987654321L)
        val targets = floatArrayOf(0.25f, 0.65f, 0.85f)
        val failures = ArrayList<String>()

        for (pattern in ArtPattern.entries) {
            val seed = 123456789L + pattern.ordinal * 7919L
            val fillMode = if (pattern.ordinal % 2 == 0) {
                ColorFillMode.GRADIENT
            } else {
                ColorFillMode.SOLID
            }
            for (weightMode in PatternWeightMode.entries) {
                val scene = MathPatternGenerator.generate(
                    seed = seed,
                    pattern = pattern,
                    intricacy = 0.60f,
                    aspectRatio = 2.0f,
                    patternWeightMode = weightMode
                )
                var previous = -1
                for (target in targets) {
                    val synthesized = SynthesizedWallpaper(
                        seed = seed,
                        pattern = pattern,
                        paletteColors = palette,
                        colorFillMode = fillMode,
                        pitchBlackRatio = target,
                        intricacy = 0.60f,
                        aspectRatio = scene.aspectRatio,
                        scene = scene,
                        ratioStats = scene.computeStats(target),
                        patternWeightMode = weightMode
                    )
                    val rendered = SkiaWallpaperRenderer.renderAndMeasure(
                        synthesized = synthesized,
                        widthPx = 420,
                        heightPx = 840
                    )
                    val measured = rendered.measuredBlackPercent
                    runCatching { rendered.image.close() }
                    val targetPct = (target * 100f).toInt()
                    if (abs(measured - targetPct) > 7) {
                        failures += "${pattern.id}/$weightMode/$fillMode: target=$targetPct measured=$measured"
                    }
                    if (measured <= previous) {
                        failures += "${pattern.id}/$weightMode/$fillMode: non-monotonic $measured <= $previous"
                    }
                    previous = measured
                }
            }
        }

        assertTrue(
            "Skia true-black calibration failures (${failures.size}):\n" +
                failures.take(60).joinToString("\n"),
            failures.isEmpty()
        )
    }

    @Test
    fun previewRenderer_usesHighResolutionLongEdgeWithoutDistortingAspect() {
        val palette = RandomPaletteGenerator.generate(7L)
        val landscapeScene = MathPatternGenerator.generate(
            seed = 7L,
            pattern = ArtPattern.SILK_RIBBONS,
            intricacy = 0.5f,
            aspectRatio = 9f / 16f
        )
        val landscape = SynthesizedWallpaper(
            seed = 7L,
            pattern = ArtPattern.SILK_RIBBONS,
            paletteColors = palette,
            colorFillMode = ColorFillMode.GRADIENT,
            pitchBlackRatio = 0.85f,
            intricacy = 0.5f,
            aspectRatio = landscapeScene.aspectRatio,
            scene = landscapeScene,
            ratioStats = landscapeScene.computeStats(0.85f)
        )
        val landscapePreview = SkiaWallpaperRenderer.renderPreview(
            landscape,
            maxLongEdgePx = 1920
        )
        assertTrue(landscapePreview.widthPx == 1920)
        assertTrue(landscapePreview.heightPx == 1080)
        runCatching { landscapePreview.image.close() }

        val portraitScene = MathPatternGenerator.generate(
            seed = 7L,
            pattern = ArtPattern.SILK_RIBBONS,
            intricacy = 0.5f,
            aspectRatio = 2f
        )
        val portrait = landscape.copy(
            aspectRatio = portraitScene.aspectRatio,
            scene = portraitScene,
            ratioStats = portraitScene.computeStats(0.85f)
        )
        val portraitPreview = SkiaWallpaperRenderer.renderPreview(
            portrait,
            maxLongEdgePx = 1920
        )
        assertTrue(portraitPreview.heightPx == 1920)
        assertTrue(portraitPreview.widthPx == 960)
        runCatching { portraitPreview.image.close() }
    }

    @Test
    fun previewRenderer_keepsAspectAndProducesPngBytes() {
        val palette = RandomPaletteGenerator.generate(42L)
        val scene = MathPatternGenerator.generate(
            seed = 42L,
            pattern = ArtPattern.SILK_RIBBONS,
            intricacy = 0.5f,
            aspectRatio = 9f / 16f
        )
        val synthesized = SynthesizedWallpaper(
            seed = 42L,
            pattern = ArtPattern.SILK_RIBBONS,
            paletteColors = palette,
            colorFillMode = ColorFillMode.GRADIENT,
            pitchBlackRatio = 0.85f,
            intricacy = 0.5f,
            aspectRatio = scene.aspectRatio,
            scene = scene,
            ratioStats = scene.computeStats(0.85f)
        )
        val preview = SkiaWallpaperRenderer.renderPreview(synthesized, maxLongEdgePx = 480)
        assertTrue(preview.image.width in 400..520)
        assertTrue(preview.image.height in 200..320)
        assertTrue(preview.measuredBlackPercent in 70..100)
        val encoded = preview.image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
        assertTrue((encoded?.bytes?.size ?: 0) > 1000)
        runCatching { preview.image.close() }
    }
}
