package com.cptodix.rimalux.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.RenderedItem
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.controller.UiMessage
import com.cptodix.rimalux.desktop.model.MonitorInfo
import com.cptodix.rimalux.desktop.model.WallpaperSet
import com.cptodix.rimalux.desktop.resources.Res
import com.cptodix.rimalux.desktop.resources.allDrawableResources
import com.cptodix.rimalux.desktop.ui.localized
import com.cptodix.rimalux.desktop.ui.localizedDisplayName
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.desktop.ui.localizedTitle
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.PitchBlackMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PaletteSwatches(
    palette: ColorPalette,
    modifier: Modifier = Modifier,
    swatchSize: Int = 14
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        palette.colors.take(6).forEach { argb ->
            Box(
                modifier = Modifier
                    .size(swatchSize.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
            )
        }
    }
}

@Composable
fun PatternThumbnail(
    patternKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val resource = Res.allDrawableResources[patternKey]
    if (resource != null) {
        Image(
            painter = painterResource(resource),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "R",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MonitorSelector(
    monitors: List<MonitorInfo>,
    selectedMonitorId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = monitors.firstOrNull { it.id == selectedMonitorId } ?: monitors.firstOrNull()
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                text = selected?.label ?: localizedStringOr("desktop_display", "Display"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            monitors.forEach { monitor ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(monitor.label)
                            if (monitor.isPrimary) {
                                Text(
                                    text = localizedStringOr("desktop_primary", "Primary"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(monitor.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueBlackControl(
    state: StudioUiState,
    onSetMode: (PitchBlackMode) -> Unit,
    onSetRatio: (Float) -> Unit,
    onSetRange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(PitchBlackMode.SINGLE, PitchBlackMode.RANGE)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.config.pitchBlackMode == mode,
                    onClick = { onSetMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                ) {
                    Text(
                        text = when (mode) {
                            PitchBlackMode.SINGLE -> localizedStringOr("desktop_single", "Single")
                            PitchBlackMode.RANGE -> localizedStringOr("desktop_range", "Range")
                        }
                    )
                }
            }
        }
        when (state.config.pitchBlackMode) {
            PitchBlackMode.SINGLE -> {
                Text(
                    text = "${(state.config.pitchBlackRatio * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = state.config.pitchBlackRatio,
                    onValueChange = onSetRatio,
                    valueRange = 0f..0.95f
                )
            }

            PitchBlackMode.RANGE -> {
                Text(
                    text = "${(state.config.pitchBlackRangeMin * 100f).roundToInt()}% – " +
                        "${(state.config.pitchBlackRangeMax * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                RangeSlider(
                    value = state.config.pitchBlackRangeMin..state.config.pitchBlackRangeMax,
                    onValueChange = { onSetRange(it.start, it.endInclusive) },
                    valueRange = 0f..0.95f
                )
            }
        }
        Text(
            text = localizedStringOr(
                "desktop_true_black_desc",
                "Exact #000000 coverage for OLED displays"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternLayoutControl(
    state: StudioUiState,
    onSelect: (PatternWeightMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(PatternWeightMode.NORMAL, PatternWeightMode.CENTER_WEIGHTED)
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.config.patternWeightMode == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size)
            ) {
                Text(
                    text = if (mode == PatternWeightMode.NORMAL) {
                        localizedStringOr("desktop_layout_normal", "Normal")
                    } else {
                        localizedStringOr("desktop_layout_center", "Center Weighted")
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HistoryStrip(
    history: List<WallpaperSet>,
    onSelect: (WallpaperSet) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = localizedStringOr("desktop_history", "History"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (history.isEmpty()) {
            Text(
                text = localizedStringOr(
                    "desktop_no_history",
                    "No history yet. Randomize or apply a wallpaper."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history.take(12), key = { it.id }) { set ->
                    HistoryCard(set = set, onClick = { onSelect(set) })
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(set: WallpaperSet, onClick: () -> Unit) {
    val primary = set.items.firstOrNull()
    val time = remember(set.timestampMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(set.timestampMillis))
    }
    Surface(
        modifier = Modifier
            .width(148.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            ) {
                if (primary != null) {
                    PatternThumbnail(
                        patternKey = "pattern_preview_${primary.pattern.id}",
                        contentDescription = primary.pattern.localizedTitle(),
                        modifier = Modifier.fillMaxWidth().height(76.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = primary?.pattern?.localizedTitle() ?: "Wallpaper",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusSnackbar(
    message: UiMessage?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val active = message ?: return@AnimatedVisibility
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.inverseSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = active.localized(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = localizedStringOr("desktop_dismiss", "Dismiss"),
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorPreviewCard(
    rendered: RenderedItem,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small),
            shape = MaterialTheme.shapes.small,
            color = Color.Black,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Image(
                bitmap = rendered.preview,
                contentDescription = rendered.item.monitorName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = "${rendered.item.monitorName} · ${rendered.measuredBlackPercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BackendBadge(state: StudioUiState, modifier: Modifier = Modifier) {
    val info = state.backendInfo
    val color = if (info.available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = info.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DisplayIdentifyOverlay(monitor: MonitorInfo) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = monitor.name,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Text(
                text = "${monitor.widthPx}×${monitor.heightPx}" +
                    if (monitor.isPrimary) {
                        " · " + localizedStringOr("desktop_primary", "Primary")
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}
