package com.cptodix.rimalux.desktop.ui.dialogs

import com.cptodix.rimalux.desktop.ui.localizedStringOr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.model.MonitorInfo
import com.cptodix.rimalux.desktop.ui.localizedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayLayoutDialog(
    state: StudioUiState,
    onSelectMonitor: (String) -> Unit,
    onRefresh: () -> Unit,
    onIdentify: (MonitorInfo) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = localizedStringOr("desktop_display_layout_title", "Display Layout"),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = localizedStringOr("desktop_display_layout_desc", "Select a display to preview. Per-display wallpapers depend on your desktop environment."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.monitors.isEmpty()) {
                    Text(
                        text = localizedStringOr("desktop_no_monitors", "No displays detected. Using a virtual 1920×1080 desktop."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    MonitorMap(
                        monitors = state.monitors,
                        selectedMonitorId = state.selectedMonitorId,
                        onSelect = onSelectMonitor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.monitors.forEach { monitor ->
                            FilterChip(
                                selected = monitor.id == state.selectedMonitorId,
                                onClick = { onSelectMonitor(monitor.id) },
                                label = {
                                    Text(
                                        text = monitor.name + if (monitor.isPrimary) " · Primary" else "",
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                Text(
                    text = if (state.backendInfo.supportsPerMonitor) {
                        localizedStringOr("desktop_per_monitor_yes", "Per-display supported")
                    } else {
                        localizedStringOr("desktop_per_monitor_no", "One wallpaper for all displays"
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.backendInfo.supportsPerMonitor) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (state.monitors.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                state.selectedMonitor?.let(onIdentify)
                            }
                        ) {
                            Text(localizedStringOr("desktop_identify_display", "Identify"))
                        }
                    }
                    OutlinedButton(onClick = onRefresh) {
                        Text(localizedStringOr("desktop_refresh_displays", "Refresh Displays"))
                    }
                    Button(onClick = onDismiss) {
                        Text(localizedStringOr("desktop_layout_done", "Done"))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorMap(
    monitors: List<MonitorInfo>,
    selectedMonitorId: String?,
    onSelect: (String) -> Unit
) {
    val minX = monitors.minOf { it.bounds.x }
    val minY = monitors.minOf { it.bounds.y }
    val maxX = monitors.maxOf { it.bounds.x + it.bounds.width }
    val maxY = monitors.maxOf { it.bounds.y + it.bounds.height }
    val totalW = (maxX - minX).coerceAtLeast(1).toFloat()
    val totalH = (maxY - minY).coerceAtLeast(1).toFloat()
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val rects = monitors.associateWith { monitor ->
        val scale = minOf(
            (canvasSize.width - 16f) / totalW,
            (canvasSize.height - 16f) / totalH
        ).coerceAtLeast(0.01f)
        val x = 8f + (monitor.bounds.x - minX) * scale
        val y = 8f + (monitor.bounds.y - minY) * scale
        val w = monitor.bounds.width * scale
        val h = monitor.bounds.height * scale
        androidx.compose.ui.geometry.Rect(x, y, x + w, y + h)
    }

    val selectedColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        canvasSize = size
        rects.forEach { (monitor, rect) ->
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = if (monitor.id == selectedMonitorId) selectedColor else outlineColor,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = if (monitor.id == selectedMonitorId) 3f else 1.5f)
            )
        }
    }
}
