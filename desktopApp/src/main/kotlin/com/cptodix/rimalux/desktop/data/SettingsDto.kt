package com.cptodix.rimalux.desktop.data

import com.cptodix.rimalux.desktop.model.AutoChangeSettings
import com.cptodix.rimalux.desktop.model.DesktopScreen
import com.cptodix.rimalux.desktop.model.DesktopSettings
import com.cptodix.rimalux.desktop.model.DisplaySettings
import com.cptodix.rimalux.desktop.model.FitMode
import com.cptodix.rimalux.desktop.model.GenerationConfig
import com.cptodix.rimalux.desktop.model.ResolutionMode
import com.cptodix.rimalux.desktop.model.UiSettings
import com.cptodix.rimalux.desktop.model.WallpaperItem
import com.cptodix.rimalux.desktop.model.WallpaperSet
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.CoreGenerationSpec
import com.cptodix.rimalux.oled.wallpaper.engine.PaletteMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.PitchBlackMode
import com.cptodix.rimalux.oled.wallpaper.engine.StarterPalettes
import kotlinx.serialization.Serializable

@Serializable
data class SettingsFileDto(
    val schemaVersion: Int = 1,
    val generation: GenerationDto = GenerationDto(),
    val display: DisplayDto = DisplayDto(),
    val ui: UiDto = UiDto(),
    val autoChange: AutoChangeDto = AutoChangeDto(),
    val currentSet: WallpaperSetDto? = null,
    val history: List<WallpaperSetDto> = emptyList(),
    val userPalettes: List<PaletteDto> = emptyList()
)

@Serializable
data class GenerationDto(
    val patternsRandomized: Boolean = true,
    val selectedPatterns: List<String> = CoreGenerationSpec.DEFAULT_SELECTED_PATTERNS.map { it.id },
    val patternWeightMode: String = PatternWeightMode.NORMAL.id,
    val colorFillMode: String = ColorFillMode.GRADIENT.id,
    val paletteMode: String = PaletteMode.RANDOMIZED.id,
    val selectedPaletteIds: List<String> = listOf("obsidian_aurora"),
    val pitchBlackMode: String = PitchBlackMode.SINGLE.id,
    val pitchBlackRatio: Float = GenerationConfig.DEFAULT_PITCH_BLACK_RATIO,
    val pitchBlackRangeMin: Float = 0.75f,
    val pitchBlackRangeMax: Float = 0.90f
)

@Serializable
data class DisplayDto(
    val selectedMonitorId: String? = null,
    val targetMonitorIds: List<String> = emptyList(),
    val matchAcrossDisplays: Boolean = true,
    val resolutionMode: String = ResolutionMode.AUTO.id,
    val customWidth: Int = 2560,
    val customHeight: Int = 1440,
    val fitMode: String = FitMode.FILL.id,
    val plasmaScreenIndexById: Map<String, Int> = emptyMap()
)

@Serializable
data class UiDto(
    val languageTag: String = "en-US",
    val lastScreen: String = DesktopScreen.STUDIO.id,
    val customWallpaperCommand: String = "",
    val backendOverride: String = "auto",
    val windowX: Int? = null,
    val windowY: Int? = null,
    val windowWidth: Int = 1280,
    val windowHeight: Int = 820,
    val windowMaximized: Boolean = false,
    val hasSeenWelcome: Boolean = false
)

@Serializable
data class AutoChangeDto(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 60,
    val nextRunEpochMillis: Long = 0L,
    val lastRunEpochMillis: Long = 0L,
    val runAtLogin: Boolean = false,
    val keepInTray: Boolean = true,
    val lastError: String? = null
)

@Serializable
data class WallpaperSetDto(
    val id: String,
    val timestampMillis: Long,
    val items: List<WallpaperItemDto> = emptyList(),
    val wasAutoScheduled: Boolean = false
)

@Serializable
data class WallpaperItemDto(
    val monitorId: String,
    val monitorName: String,
    val screenIndex: Int = 0,
    val seed: Long,
    val pattern: String,
    val paletteColors: List<Long> = emptyList(),
    val colorFillMode: String = ColorFillMode.GRADIENT.id,
    val pitchBlackRatio: Float = GenerationConfig.DEFAULT_PITCH_BLACK_RATIO,
    val intricacy: Float = 0.5f,
    val patternWeightMode: String = PatternWeightMode.NORMAL.id,
    val widthPx: Int = 1920,
    val heightPx: Int = 1080,
    val measuredBlackPercent: Int? = null,
    val wasAutoScheduled: Boolean = false
)

@Serializable
data class PaletteDto(
    val id: String,
    val name: String,
    val colors: List<Long> = emptyList()
)

object SettingsMapper {

    fun toDomain(dto: SettingsFileDto): DesktopSettings {
        val palettes = resolvePalettes(dto.userPalettes)
        val paletteIds = palettes.map { it.id }.toSet()
        val selectedPaletteIds = dto.generation.selectedPaletteIds
            .filter { it in paletteIds }
            .toSet()
            .ifEmpty { setOf(palettes.firstOrNull()?.id ?: "obsidian_aurora") }

        val selectedPatterns = dto.generation.selectedPatterns
            .mapNotNull { ArtPattern.fromId(it) }
            .toSet()
            .ifEmpty { CoreGenerationSpec.DEFAULT_SELECTED_PATTERNS }

        val rangeLo = minOf(dto.generation.pitchBlackRangeMin, dto.generation.pitchBlackRangeMax)
            .coerceIn(0f, 0.95f)
        val rangeHi = maxOf(dto.generation.pitchBlackRangeMin, dto.generation.pitchBlackRangeMax)
            .coerceIn(rangeLo, 0.95f)

        return DesktopSettings(
            generation = GenerationConfig(
                patternsRandomized = dto.generation.patternsRandomized,
                selectedPatterns = selectedPatterns,
                patternWeightMode = PatternWeightMode.fromId(dto.generation.patternWeightMode),
                colorFillMode = ColorFillMode.fromId(dto.generation.colorFillMode),
                paletteMode = PaletteMode.fromId(dto.generation.paletteMode),
                selectedPaletteIds = selectedPaletteIds,
                pitchBlackMode = PitchBlackMode.fromId(dto.generation.pitchBlackMode),
                pitchBlackRatio = dto.generation.pitchBlackRatio.coerceIn(0f, 0.95f),
                pitchBlackRangeMin = rangeLo,
                pitchBlackRangeMax = rangeHi
            ),
            display = DisplaySettings(
                selectedMonitorId = dto.display.selectedMonitorId,
                targetMonitorIds = dto.display.targetMonitorIds.toSet(),
                matchAcrossDisplays = dto.display.matchAcrossDisplays,
                resolutionMode = ResolutionMode.fromId(dto.display.resolutionMode),
                customWidth = dto.display.customWidth.coerceIn(320, 8192),
                customHeight = dto.display.customHeight.coerceIn(240, 8192),
                fitMode = FitMode.fromId(dto.display.fitMode),
                plasmaScreenIndexById = dto.display.plasmaScreenIndexById
            ),
            ui = UiSettings(
                languageTag = dto.ui.languageTag.ifBlank { "en-US" },
                lastScreen = DesktopScreen.fromId(dto.ui.lastScreen),
                customWallpaperCommand = dto.ui.customWallpaperCommand,
                backendOverride = dto.ui.backendOverride.ifBlank { "auto" },
                windowX = dto.ui.windowX,
                windowY = dto.ui.windowY,
                windowWidth = dto.ui.windowWidth.coerceAtLeast(960),
                windowHeight = dto.ui.windowHeight.coerceAtLeast(640),
                windowMaximized = dto.ui.windowMaximized,
                hasSeenWelcome = dto.ui.hasSeenWelcome
            ),
            autoChange = AutoChangeSettings(
                enabled = dto.autoChange.enabled,
                intervalMinutes = dto.autoChange.intervalMinutes.coerceIn(1, 10080),
                nextRunEpochMillis = dto.autoChange.nextRunEpochMillis,
                lastRunEpochMillis = dto.autoChange.lastRunEpochMillis,
                runAtLogin = dto.autoChange.runAtLogin,
                keepInTray = dto.autoChange.keepInTray,
                lastError = dto.autoChange.lastError
            ),
            currentSet = dto.currentSet?.toDomain(),
            history = dto.history.map { it.toDomain() }.take(24),
            userPalettes = palettes
        )
    }

    fun toDto(settings: DesktopSettings): SettingsFileDto = SettingsFileDto(
        generation = GenerationDto(
            patternsRandomized = settings.generation.patternsRandomized,
            selectedPatterns = settings.generation.selectedPatterns.map { it.id },
            patternWeightMode = settings.generation.patternWeightMode.id,
            colorFillMode = settings.generation.colorFillMode.id,
            paletteMode = settings.generation.paletteMode.id,
            selectedPaletteIds = settings.generation.selectedPaletteIds.toList(),
            pitchBlackMode = settings.generation.pitchBlackMode.id,
            pitchBlackRatio = settings.generation.pitchBlackRatio,
            pitchBlackRangeMin = settings.generation.pitchBlackRangeMin,
            pitchBlackRangeMax = settings.generation.pitchBlackRangeMax
        ),
        display = DisplayDto(
            selectedMonitorId = settings.display.selectedMonitorId,
            targetMonitorIds = settings.display.targetMonitorIds.toList(),
            matchAcrossDisplays = settings.display.matchAcrossDisplays,
            resolutionMode = settings.display.resolutionMode.id,
            customWidth = settings.display.customWidth,
            customHeight = settings.display.customHeight,
            fitMode = settings.display.fitMode.id,
            plasmaScreenIndexById = settings.display.plasmaScreenIndexById
        ),
        ui = UiDto(
            languageTag = settings.ui.languageTag,
            lastScreen = settings.ui.lastScreen.id,
            customWallpaperCommand = settings.ui.customWallpaperCommand,
            backendOverride = settings.ui.backendOverride,
            windowX = settings.ui.windowX,
            windowY = settings.ui.windowY,
            windowWidth = settings.ui.windowWidth,
            windowHeight = settings.ui.windowHeight,
            windowMaximized = settings.ui.windowMaximized,
            hasSeenWelcome = settings.ui.hasSeenWelcome
        ),
        autoChange = AutoChangeDto(
            enabled = settings.autoChange.enabled,
            intervalMinutes = settings.autoChange.intervalMinutes,
            nextRunEpochMillis = settings.autoChange.nextRunEpochMillis,
            lastRunEpochMillis = settings.autoChange.lastRunEpochMillis,
            runAtLogin = settings.autoChange.runAtLogin,
            keepInTray = settings.autoChange.keepInTray,
            lastError = settings.autoChange.lastError
        ),
        currentSet = settings.currentSet?.toDto(),
        history = settings.history.take(24).map { it.toDto() },
        userPalettes = settings.resolvedPalettes().map { it.toDto() }
    )

    private fun resolvePalettes(palettes: List<PaletteDto>): List<ColorPalette> {
        val decoded = palettes
            .filter { it.colors.isNotEmpty() }
            .map { ColorPalette(id = it.id, name = it.name, colors = it.colors) }
        if (decoded.isEmpty()) return StarterPalettes.defaults
        val existing = decoded.map { it.id }.toSet()
        val missing = StarterPalettes.defaults.filterNot { it.id in existing }
        return decoded + missing
    }

    private fun WallpaperSetDto.toDomain(): WallpaperSet = WallpaperSet(
        id = id,
        timestampMillis = timestampMillis,
        items = items.map { it.toDomain() },
        wasAutoScheduled = wasAutoScheduled
    )

    private fun WallpaperSet.toDto(): WallpaperSetDto = WallpaperSetDto(
        id = id,
        timestampMillis = timestampMillis,
        items = items.map { it.toDto() },
        wasAutoScheduled = wasAutoScheduled
    )

    private fun WallpaperItemDto.toDomain(): WallpaperItem = WallpaperItem(
        monitorId = monitorId,
        monitorName = monitorName,
        screenIndex = screenIndex,
        seed = seed,
        pattern = ArtPattern.fromId(pattern) ?: ArtPattern.SILK_RIBBONS,
        paletteColors = paletteColors.ifEmpty { StarterPalettes.defaults.first().colors },
        colorFillMode = ColorFillMode.fromId(colorFillMode),
        pitchBlackRatio = pitchBlackRatio.coerceIn(0f, 0.95f),
        intricacy = intricacy.coerceIn(0.15f, 1f),
        patternWeightMode = PatternWeightMode.fromId(patternWeightMode),
        widthPx = widthPx.coerceIn(320, 8192),
        heightPx = heightPx.coerceIn(240, 8192),
        measuredBlackPercent = measuredBlackPercent,
        wasAutoScheduled = wasAutoScheduled
    )

    private fun WallpaperItem.toDto(): WallpaperItemDto = WallpaperItemDto(
        monitorId = monitorId,
        monitorName = monitorName,
        screenIndex = screenIndex,
        seed = seed,
        pattern = pattern.id,
        paletteColors = paletteColors,
        colorFillMode = colorFillMode.id,
        pitchBlackRatio = pitchBlackRatio,
        intricacy = intricacy,
        patternWeightMode = patternWeightMode.id,
        widthPx = widthPx,
        heightPx = heightPx,
        measuredBlackPercent = measuredBlackPercent,
        wasAutoScheduled = wasAutoScheduled
    )

    private fun ColorPalette.toDto(): PaletteDto = PaletteDto(
        id = id,
        name = name,
        colors = colors
    )
}
