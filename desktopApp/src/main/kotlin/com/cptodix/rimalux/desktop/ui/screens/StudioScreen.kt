package com.cptodix.rimalux.desktop.ui.screens

import com.cptodix.rimalux.desktop.ui.localizedStringOr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.RenderedItem
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.model.DesktopScreen
import com.cptodix.rimalux.desktop.ui.components.HistoryStrip
import com.cptodix.rimalux.desktop.ui.components.LabeledValue
import com.cptodix.rimalux.desktop.ui.components.MonitorPreviewCard
import com.cptodix.rimalux.desktop.ui.components.MonitorSelector
import com.cptodix.rimalux.desktop.ui.components.PaletteSwatches
import com.cptodix.rimalux.desktop.ui.components.PatternLayoutControl
import com.cptodix.rimalux.desktop.ui.components.PatternThumbnail
import com.cptodix.rimalux.desktop.ui.components.ScreenHeader
import com.cptodix.rimalux.desktop.ui.components.SectionCard
import com.cptodix.rimalux.desktop.ui.components.TrueBlackControl
import com.cptodix.rimalux.desktop.ui.dialogs.DisplayLayoutDialog
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedSubtitle
import com.cptodix.rimalux.desktop.ui.localizedTitle
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.PitchBlackMode

@Composable
fun StudioScreen(
    state: StudioUiState,
    controller: StudioController,
    onNavigate: (DesktopScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = state.current
    val selected = state.selectedRenderedItem
    var showDisplayLayout by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = localizedStringOr("desktop_studio_title", "Rimalux Studio"),
            subtitle = localizedStringOr("desktop_studio_subtitle", "Mathematical vector wallpapers with exact True Black calibration"
            )
        ) {
            IconButton(
                onClick = controller::previous,
                enabled = state.canGoBack
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous wallpaper"
                )
            }
            IconButton(
                onClick = controller::next,
                enabled = state.canGoForward
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next wallpaper"
                )
            }
            OutlinedButton(onClick = controller::randomize) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null)
                Text(
                    text = localizedStringOr("desktop_randomize", "Randomize"),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            OutlinedButton(
                onClick = { controller.savePngs(ApplyTarget.ALL_DISPLAYS) },
                enabled = current != null && !state.isSaving
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Text(
                    text = localizedStringOr("desktop_save_png", "Save PNG"),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Button(
                onClick = { controller.applyWallpaper(ApplyTarget.ALL_DISPLAYS) },
                enabled = current != null && !state.isApplying
            ) {
                Text(localizedStringOr("desktop_apply_wallpaper", "Apply Wallpaper"))
            }
        }

        if (state.isGenerating || state.isApplying || state.isSaving) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 980.dp
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    PreviewPane(
                        state = state,
                        selected = selected,
                        controller = controller,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    ControlPanel(
                        state = state,
                        controller = controller,
                        onNavigate = onNavigate,
                        onOpenDisplayLayout = { showDisplayLayout = true },
                        scrollable = true,
                        modifier = Modifier.width(400.dp).fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreviewPane(
                        state = state,
                        selected = selected,
                        controller = controller,
                        modifier = Modifier.fillMaxWidth().height(380.dp)
                    )
                    ControlPanel(
                        state = state,
                        controller = controller,
                        onNavigate = onNavigate,
                        onOpenDisplayLayout = { showDisplayLayout = true },
                        scrollable = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDisplayLayout) {
        DisplayLayoutDialog(
            state = state,
            onSelectMonitor = controller::selectMonitor,
            onRefresh = controller::refreshMonitors,
            onDismiss = { showDisplayLayout = false }
        )
    }
}

@Composable
private fun PreviewPane(
    state: StudioUiState,
    selected: RenderedItem?,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    val current = state.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MonitorSelector(
                monitors = state.monitors,
                selectedMonitorId = state.selectedMonitorId,
                onSelect = controller::selectMonitor
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = selected?.item?.let {
                    localizedString(
                        "desktop_display_summary",
                        it.monitorName,
                        it.widthPx,
                        it.heightPx
                    )
                } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            color = Color.Black,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                if (selected != null) {
                    Image(
                        bitmap = selected.preview,
                        contentDescription = selected.item.pattern.localizedTitle(),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (state.isGenerating) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = localizedStringOr("desktop_generating", "Generating wallpaper…"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                } else {
                    Text(
                        text = state.previewError ?: localizedStringOr("desktop_error_no_wallpaper", "No wallpaper is ready yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }

                if (selected != null) {
                    OverlayLabel(
                        text = selected.item.pattern.localizedTitle(),
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    )
                    OverlayLabel(
                        text = localizedString(
                            "desktop_measured_black",
                            selected.measuredBlackPercent
                        ),
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )
                    OverlayLabel(
                        text = "${localizedStringOr("desktop_seed", "Seed")} " +
                            (selected.item.seed and 0xFFFFFFFFFFFFL).toString(16).uppercase(),
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    )
                }
            }
        }

        if (current != null && current.items.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(current.items, key = { it.item.monitorId }) { rendered ->
                    MonitorPreviewCard(
                        rendered = rendered,
                        modifier = Modifier.width(210.dp)
                    )
                }
            }
        }

        HistoryStrip(
            history = state.history,
            onSelect = controller::restoreHistorySet
        )
    }
}

@Composable
private fun OverlayLabel(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Black.copy(alpha = 0.62f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ControlPanel(
    state: StudioUiState,
    controller: StudioController,
    onNavigate: (DesktopScreen) -> Unit,
    onOpenDisplayLayout: () -> Unit,
    scrollable: Boolean,
    modifier: Modifier = Modifier
) {
    val selected = state.selectedRenderedItem
    val currentColors = selected?.item?.paletteColors ?: state.userPalettes.firstOrNull()?.colors.orEmpty()
    val palette = ColorPalette("current", "Current", currentColors)

    Column(
        modifier = if (scrollable) {
            modifier.verticalScroll(rememberScrollState())
        } else {
            modifier
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(
            title = localizedStringOr("desktop_pattern", "Pattern"),
            subtitle = selected?.item?.pattern?.localizedSubtitle(),
            trailing = {
                TextButton(onClick = { onNavigate(DesktopScreen.PATTERNS) }) {
                    Text(localizedStringOr("desktop_browse", "Browse"))
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected != null) {
                    PatternThumbnail(
                        patternKey = "pattern_preview_${selected.item.pattern.id}",
                        contentDescription = selected.item.pattern.localizedTitle(),
                        modifier = Modifier
                            .size(width = 96.dp, height = 62.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected?.item?.pattern?.localizedTitle()
                            ?: localizedStringOr("desktop_loading", "Loading…"),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LabeledValue(
                        label = localizedStringOr("desktop_seed", "Seed"),
                        value = selected?.item?.seed?.let {
                            (it and 0xFFFFFFFFFFFFL).toString(16).uppercase()
                        } ?: "—"
                    )
                }
            }
        }

        SectionCard(
            title = localizedStringOr("desktop_colors", "Colors"),
            subtitle = localizedStringOr("desktop_colors_subtitle", "Curated OLED palettes and color harmony"),
            trailing = {
                TextButton(onClick = { onNavigate(DesktopScreen.COLORS) }) {
                    Text(localizedStringOr("desktop_browse", "Browse"))
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaletteSwatches(palette = palette, swatchSize = 18)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${palette.colors.size} colors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard(
            title = localizedStringOr("desktop_true_black", "True Black Ratio"),
            subtitle = localizedStringOr("desktop_true_black_desc", "Exact #000000 coverage for OLED displays"
            )
        ) {
            TrueBlackControl(
                state = state,
                onSetMode = controller::setPitchBlackMode,
                onSetRatio = controller::setPitchBlackRatio,
                onSetRange = controller::setPitchBlackRange
            )
        }

        SectionCard(title = localizedStringOr("desktop_pattern_layout", "Pattern Layout")) {
            PatternLayoutControl(state = state, onSelect = controller::setPatternWeightMode)
        }

        SectionCard(title = localizedStringOr("desktop_color_fill", "Color Fill")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FillChip(
                    label = localizedStringOr("desktop_fill_solid", "Solid"),
                    selected = state.config.colorFillMode == ColorFillMode.SOLID,
                    onClick = { controller.setColorFillMode(ColorFillMode.SOLID) }
                )
                FillChip(
                    label = localizedStringOr("desktop_fill_gradient", "Gradient"),
                    selected = state.config.colorFillMode == ColorFillMode.GRADIENT,
                    onClick = { controller.setColorFillMode(ColorFillMode.GRADIENT) }
                )
            }
        }

        SectionCard(
            title = localizedStringOr("desktop_display_targets", "Display Targets"),
            subtitle = if (state.backendInfo.supportsPerMonitor) {
                localizedStringOr("desktop_per_monitor_yes", "Per-display supported")
            } else {
                localizedStringOr("desktop_per_monitor_no", "One wallpaper for all displays")
            },
            trailing = {
                TextButton(onClick = onOpenDisplayLayout) {
                    Text(localizedStringOr("desktop_display_layout", "Display Layout"))
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedStringOr("desktop_match_design", "Match design across displays"
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = localizedStringOr("desktop_match_design_desc", "Use the same pattern, palette, and ratio on every display."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.display.matchAcrossDisplays,
                    onCheckedChange = controller::setMatchAcrossDisplays
                )
            }
            if (!state.backendInfo.supportsPerMonitor) {
                Text(
                    text = localizedStringOr("desktop_backend_note", "KDE Plasma supports per-display wallpapers. GNOME uses one wallpaper for all displays."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard(title = localizedStringOr("desktop_apply_actions", "Apply & Export")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { controller.applyWallpaper(ApplyTarget.ALL_DISPLAYS) },
                    enabled = state.current != null && !state.isApplying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedStringOr("desktop_apply_all", "Apply to All Displays"))
                }
                OutlinedButton(
                    onClick = { controller.applyWallpaper(ApplyTarget.SELECTED_DISPLAY) },
                    enabled = state.current != null && !state.isApplying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedStringOr("desktop_apply_this", "Apply to This Display"))
                }
                OutlinedButton(
                    onClick = { controller.savePngs(ApplyTarget.ALL_DISPLAYS) },
                    enabled = state.current != null && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedStringOr("desktop_save_all", "Save All Displays"))
                }
                OutlinedButton(
                    onClick = { controller.savePngs(ApplyTarget.SELECTED_DISPLAY) },
                    enabled = state.current != null && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedStringOr("desktop_save_this", "Save This Display"))
                }
                if (!state.backendInfo.available) {
                    Text(
                        text = localizedStringOr("desktop_error_backend", "No wallpaper backend detected. Save a PNG or configure a custom command in Settings."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun FillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        }
    )
}
