package com.cptodix.rimalux.desktop.controller

import androidx.compose.ui.graphics.ImageBitmap
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.model.AutoChangeSettings
import com.cptodix.rimalux.desktop.model.BackendInfo
import com.cptodix.rimalux.desktop.model.DesktopScreen
import com.cptodix.rimalux.desktop.model.DisplaySettings
import com.cptodix.rimalux.desktop.model.GenerationConfig
import com.cptodix.rimalux.desktop.model.MonitorInfo
import com.cptodix.rimalux.desktop.model.UiSettings
import com.cptodix.rimalux.desktop.model.WallpaperItem
import com.cptodix.rimalux.desktop.model.WallpaperSet
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.StarterPalettes
import com.cptodix.rimalux.oled.wallpaper.engine.SynthesizedWallpaper

data class UiMessage(
    val key: String,
    val args: List<Any> = emptyList()
)

data class RenderedItem(
    val item: WallpaperItem,
    val synthesized: SynthesizedWallpaper,
    val preview: ImageBitmap,
    val measuredBlackPercent: Int
)

data class RenderedSet(
    val set: WallpaperSet,
    val items: List<RenderedItem>
) {
    fun itemFor(monitorId: String?): RenderedItem? =
        items.firstOrNull { it.item.monitorId == monitorId } ?: items.firstOrNull()
}

data class StudioUiState(
    val config: GenerationConfig = GenerationConfig(),
    val display: DisplaySettings = DisplaySettings(),
    val ui: UiSettings = UiSettings(),
    val autoChange: AutoChangeSettings = AutoChangeSettings(),
    val monitors: List<MonitorInfo> = emptyList(),
    val current: RenderedSet? = null,
    val selectedMonitorId: String? = null,
    val history: List<WallpaperSet> = emptyList(),
    val userPalettes: List<ColorPalette> = StarterPalettes.defaults,
    val screen: DesktopScreen = DesktopScreen.STUDIO,
    val isGenerating: Boolean = true,
    val isApplying: Boolean = false,
    val isSaving: Boolean = false,
    val statusMessage: UiMessage? = null,
    val backendInfo: BackendInfo = BackendInfo.unsupported(),
    val languageTag: String = "en-US",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val aheadReadyCount: Int = 0,
    val osSchedulerInstalled: Boolean = false,
    val previewError: String? = null
) {
    val selectedMonitor: MonitorInfo?
        get() = monitors.firstOrNull { it.id == selectedMonitorId }
            ?: monitors.firstOrNull { it.isPrimary }
            ?: monitors.firstOrNull()

    val selectedRenderedItem: RenderedItem?
        get() = current?.itemFor(selectedMonitorId)
}

internal data class GeneratedItem(
    val item: WallpaperItem,
    val synthesized: SynthesizedWallpaper
)

internal enum class RegenerationMode {
    PRESERVE,
    RESOLVE
}

internal fun ApplyTarget.descriptionKey(): String = when (this) {
    ApplyTarget.ALL_DISPLAYS -> "desktop_apply_all_displays"
    ApplyTarget.SELECTED_DISPLAY -> "desktop_apply_selected_display"
}
