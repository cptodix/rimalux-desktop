package com.cptodix.rimalux.desktop.model

import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.CoreGenerationSpec
import com.cptodix.rimalux.oled.wallpaper.engine.DeepBlackRatioEngine
import com.cptodix.rimalux.oled.wallpaper.engine.PaletteMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.PitchBlackMode
import com.cptodix.rimalux.oled.wallpaper.engine.WallpaperGeneratorEngine
import kotlin.random.Random

/**
 * Desktop generation settings. Mirrors the portable parts of the Android `GeneratorConfig`
 * without Android wallpaper targets or scheduler state.
 */
data class GenerationConfig(
    val patternsRandomized: Boolean = true,
    val selectedPatterns: Set<ArtPattern> = CoreGenerationSpec.DEFAULT_SELECTED_PATTERNS,
    val patternWeightMode: PatternWeightMode = PatternWeightMode.NORMAL,
    val colorFillMode: ColorFillMode = ColorFillMode.GRADIENT,
    val paletteMode: PaletteMode = PaletteMode.RANDOMIZED,
    val selectedPaletteIds: Set<String> = setOf("obsidian_aurora"),
    val pitchBlackMode: PitchBlackMode = PitchBlackMode.SINGLE,
    val pitchBlackRatio: Float = DeepBlackRatioEngine.DEFAULT_PITCH_BLACK_RATIO,
    val pitchBlackRangeMin: Float = 0.75f,
    val pitchBlackRangeMax: Float = 0.90f
) {
    fun toCoreSpec(): CoreGenerationSpec = CoreGenerationSpec(
        patternsRandomized = patternsRandomized,
        selectedPatterns = selectedPatterns,
        patternWeightMode = patternWeightMode,
        colorFillMode = colorFillMode,
        paletteMode = paletteMode,
        selectedPaletteIds = selectedPaletteIds,
        pitchBlackMode = pitchBlackMode,
        pitchBlackRatio = pitchBlackRatio,
        pitchBlackRangeMin = pitchBlackRangeMin,
        pitchBlackRangeMax = pitchBlackRangeMax
    )

    fun resolvePitchBlackRatio(rng: Random): Float =
        DeepBlackRatioEngine.resolveTargetBlackRatio(
            mode = pitchBlackMode,
            singleRatio = pitchBlackRatio,
            rangeMin = pitchBlackRangeMin,
            rangeMax = pitchBlackRangeMax,
            rng = rng
        )

    companion object {
        const val DEFAULT_PITCH_BLACK_RATIO: Float = DeepBlackRatioEngine.DEFAULT_PITCH_BLACK_RATIO
        const val MIN_RANDOM_INTRICACY: Float = WallpaperGeneratorEngine.MIN_RANDOM_INTRICACY
        const val MAX_RANDOM_INTRICACY: Float = WallpaperGeneratorEngine.MAX_RANDOM_INTRICACY

        fun sampleIntricacy(rng: Random): Float = WallpaperGeneratorEngine.sampleIntricacy(rng)

        fun sampleIntricacyForSeed(seed: Long): Float =
            WallpaperGeneratorEngine.sampleIntricacyForSeed(seed)
    }
}
