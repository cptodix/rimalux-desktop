package com.cptodix.rimalux.desktop.model

import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.StarterPalettes

data class MonitorBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class MonitorInfo(
    val id: String,
    val name: String,
    val bounds: MonitorBounds,
    val widthPx: Int,
    val heightPx: Int,
    val scale: Float = 1f,
    val isPrimary: Boolean = false,
    val plasmaScreenIndex: Int? = null
) {
    val aspectRatio: Float
        get() = (heightPx.toFloat() / widthPx.toFloat()).coerceIn(0.45f, 3.2f)

    val label: String
        get() = "$name · ${widthPx}×${heightPx}"

    val safeCacheKey: String
        get() = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

data class WallpaperItem(
    val monitorId: String,
    val monitorName: String,
    val screenIndex: Int,
    val seed: Long,
    val pattern: ArtPattern,
    val paletteColors: List<Long>,
    val colorFillMode: ColorFillMode,
    val pitchBlackRatio: Float,
    val intricacy: Float,
    val patternWeightMode: PatternWeightMode,
    val widthPx: Int,
    val heightPx: Int,
    val measuredBlackPercent: Int? = null,
    val wasAutoScheduled: Boolean = false
) {
    val safeCacheKey: String
        get() = monitorName.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

data class WallpaperSet(
    val id: String,
    val timestampMillis: Long,
    val items: List<WallpaperItem>,
    val wasAutoScheduled: Boolean = false
) {
    fun itemFor(monitorId: String?): WallpaperItem? =
        items.firstOrNull { it.monitorId == monitorId } ?: items.firstOrNull()
}

enum class ResolutionMode(val id: String) {
    AUTO("auto"),
    P1080("1080p"),
    P1440("1440p"),
    P2160("4k"),
    CUSTOM("custom");

    companion object {
        fun fromId(id: String?): ResolutionMode = entries.find { it.id == id } ?: AUTO
    }
}

enum class FitMode(val id: String) {
    FILL("fill"),
    FIT("fit"),
    CENTER("center"),
    TILE("tile");

    companion object {
        fun fromId(id: String?): FitMode = entries.find { it.id == id } ?: FILL
    }
}

data class DisplaySettings(
    val selectedMonitorId: String? = null,
    val targetMonitorIds: Set<String> = emptySet(),
    val matchAcrossDisplays: Boolean = true,
    val resolutionMode: ResolutionMode = ResolutionMode.AUTO,
    val customWidth: Int = 2560,
    val customHeight: Int = 1440,
    val fitMode: FitMode = FitMode.FILL,
    val plasmaScreenIndexById: Map<String, Int> = emptyMap()
) {
    fun targets(all: List<MonitorInfo>): List<MonitorInfo> {
        if (targetMonitorIds.isEmpty()) return all
        return all.filter { it.id in targetMonitorIds }
    }

    fun selected(all: List<MonitorInfo>): MonitorInfo? =
        all.firstOrNull { it.id == selectedMonitorId }
            ?: all.firstOrNull { it.isPrimary }
            ?: all.firstOrNull()
}

enum class DesktopScreen(val id: String) {
    STUDIO("studio"),
    PATTERNS("patterns"),
    COLORS("colors"),
    SETTINGS("settings");

    companion object {
        fun fromId(id: String?): DesktopScreen = entries.find { it.id == id } ?: STUDIO
    }
}

data class UiSettings(
    val languageTag: String = "en-US",
    val lastScreen: DesktopScreen = DesktopScreen.STUDIO,
    val customWallpaperCommand: String = "",
    val backendOverride: String = "auto",
    val windowX: Int? = null,
    val windowY: Int? = null,
    val windowWidth: Int = 1280,
    val windowHeight: Int = 820,
    val windowMaximized: Boolean = false,
    val hasSeenWelcome: Boolean = false
)

/**
 * Phase 2 scheduler state is persisted now so settings migrations remain stable.
 * The Phase 1 UI does not expose these fields yet.
 */
data class AutoChangeSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Int = 60,
    val nextRunEpochMillis: Long = 0L,
    val lastRunEpochMillis: Long = 0L,
    val runAtLogin: Boolean = false,
    val keepInTray: Boolean = true,
    val lastError: String? = null
)

data class DesktopSettings(
    val generation: GenerationConfig = GenerationConfig(),
    val display: DisplaySettings = DisplaySettings(),
    val ui: UiSettings = UiSettings(),
    val autoChange: AutoChangeSettings = AutoChangeSettings(),
    val currentSet: WallpaperSet? = null,
    val history: List<WallpaperSet> = emptyList(),
    val userPalettes: List<ColorPalette> = StarterPalettes.defaults,
    val deletedPaletteIds: Set<String> = emptySet()
) {
    fun resolvedPalettes(): List<ColorPalette> {
        val existingIds = userPalettes.map { it.id }.toSet()
        val missingStarters = StarterPalettes.defaults.filter {
            it.id !in existingIds && it.id !in deletedPaletteIds
        }
        return (userPalettes + missingStarters).ifEmpty { StarterPalettes.defaults }
    }
}

enum class ApplyTarget {
    ALL_DISPLAYS,
    SELECTED_DISPLAY
}

data class BackendInfo(
    val id: String,
    val displayName: String,
    val supportsPerMonitor: Boolean,
    val available: Boolean,
    val detail: String = ""
) {
    companion object {
        fun unsupported(detail: String = "") = BackendInfo(
            id = "unsupported",
            displayName = "Unsupported",
            supportsPerMonitor = false,
            available = false,
            detail = detail
        )
    }
}

data class WallpaperFile(
    val item: WallpaperItem,
    val path: java.nio.file.Path,
    val measuredBlackPercent: Int
)
