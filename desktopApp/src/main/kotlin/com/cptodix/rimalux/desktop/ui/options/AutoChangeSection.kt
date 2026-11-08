@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.cptodix.rimalux.desktop.ui.options

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.scheduler.AutoChangePolicy
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr

@Composable
fun AutoChangeSection(
    state: StudioUiState,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    var showTargets by remember { mutableStateOf(false) }
    var showInterval by remember { mutableStateOf(false) }
    val auto = state.autoChange
    val targetSummary = when {
        state.display.targetMonitorIds.isEmpty() && !state.display.matchAcrossDisplays ->
            localizedStringOr("desktop_apply_all_separate", "All Displays Separate")
        state.display.targetMonitorIds.isEmpty() ||
            state.display.targetMonitorIds.size >= state.monitors.size ->
            localizedStringOr("desktop_all_displays", "All Displays")
        else -> localizedString(
            "desktop_auto_selected_displays",
            state.display.targetMonitorIds.size
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
            )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localizedStringOr(
                                "auto_master_toggle",
                                "Auto-Change Wallpaper"
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = localizedStringOr(
                                "auto_off_desc",
                                "Automatically updates your wallpaper at set intervals"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = auto.enabled,
                        onCheckedChange = controller::setAutoChangeEnabled
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )

                AutoChangeRow(
                    title = localizedStringOr("desktop_auto_target_title", "Apply To"),
                    summary = targetSummary,
                    enabled = auto.enabled,
                    onClick = { showTargets = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
                AutoChangeRow(
                    title = localizedStringOr("desktop_auto_interval_title", "Interval"),
                    summary = intervalShortLabel(auto.intervalMinutes),
                    enabled = auto.enabled,
                    onClick = { showInterval = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
                AutoToggleRow(
                    title = localizedStringOr("desktop_auto_start_login", "Start at Login"),
                    summary = localizedStringOr(
                        "desktop_auto_start_login_desc",
                        "Run Rimalux in the background after you sign in."
                    ),
                    checked = auto.runAtLogin,
                    onCheckedChange = controller::setRunAtLogin
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
                AutoToggleRow(
                    title = localizedStringOr("desktop_auto_keep_tray", "Keep in Tray"),
                    summary = localizedStringOr(
                        "desktop_auto_keep_tray_desc",
                        "Closing the window keeps auto-change running."
                    ),
                    checked = auto.keepInTray,
                    onCheckedChange = controller::setKeepInTray
                )
            }
        }

    }

    if (showTargets) {
        AutoTargetDialog(
            state = state,
            controller = controller,
            onDismiss = { showTargets = false }
        )
    }
    if (showInterval) {
        AutoIntervalDialog(
            selectedMinutes = auto.intervalMinutes,
            onSelect = {
                controller.setAutoInterval(it)
                showInterval = false
            },
            onDismiss = { showInterval = false }
        )
    }
}

@Composable
private fun AutoChangeRow(
    title: String,
    summary: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.55f
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AutoToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AutoTargetDialog(
    state: StudioUiState,
    controller: StudioController,
    onDismiss: () -> Unit
) {
    val allTargeted = state.display.targetMonitorIds.isEmpty() ||
        state.display.targetMonitorIds.size >= state.monitors.size
    val sameAll = allTargeted && state.display.matchAcrossDisplays
    val separateAll = allTargeted && !state.display.matchAcrossDisplays

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = localizedStringOr("desktop_auto_target_title", "Apply To"),
                    style = MaterialTheme.typography.titleLarge
                )
                state.monitors.forEach { monitor ->
                    TargetOption(
                        title = monitor.label,
                        subtitle = if (monitor.isPrimary) {
                            localizedStringOr("desktop_primary", "Primary")
                        } else {
                            null
                        },
                        subtitleColor = MaterialTheme.colorScheme.primary,
                        selected = allTargeted || monitor.id in state.display.targetMonitorIds,
                        onClick = { controller.toggleAutoTargetMonitor(monitor.id) }
                    )
                }
                TargetOption(
                    title = localizedStringOr("desktop_all_displays", "All Displays"),
                    subtitle = localizedStringOr(
                        "desktop_apply_all_desc",
                        "Same wallpaper on every display"
                    ),
                    selected = sameAll,
                    onClick = {
                        controller.setAutoTargets(emptySet())
                        controller.setMatchAcrossDisplays(true)
                        onDismiss()
                    }
                )
                TargetOption(
                    title = localizedStringOr(
                        "desktop_apply_all_separate",
                        "All Displays Separate"
                    ),
                    subtitle = if (state.backendInfo.supportsPerMonitor) {
                        localizedStringOr(
                            "desktop_apply_all_separate_desc",
                            "A different wallpaper on each display"
                        )
                    } else {
                        localizedStringOr(
                            "desktop_apply_all_separate_unsupported",
                            "This backend applies one wallpaper to all displays"
                        )
                    },
                    selected = separateAll,
                    enabled = state.backendInfo.supportsPerMonitor,
                    onClick = {
                        controller.setAutoTargets(emptySet())
                        controller.setMatchAcrossDisplays(false)
                        onDismiss()
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(localizedStringOr("desktop_close", "Close"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetOption(
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = Color.Unspecified,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        border = BorderStroke(
            width = if (selected) 1.dp else 0.5.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (subtitleColor == Color.Unspecified) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            subtitleColor
                        }
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AutoIntervalDialog(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var custom by remember { mutableStateOf(selectedMinutes.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val intervalError = localizedStringOr(
        "error_interval_range",
        "Enter 1 to 10,080 minutes"
    )
    val presets = listOf(15, 30, 60, 180, 360, 720, 1440)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = localizedStringOr("desktop_auto_interval_title", "Interval"),
                    style = MaterialTheme.typography.titleLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { minutes ->
                        FilterChip(
                            selected = selectedMinutes == minutes,
                            onClick = { onSelect(minutes) },
                            label = { Text(intervalShortLabel(minutes)) },
                            leadingIcon = if (selectedMinutes == minutes) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it.filter(Char::isDigit).take(5) },
                        label = { Text(localizedStringOr("desktop_auto_minutes", "Minutes")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        shape = MaterialTheme.shapes.medium,
                        onClick = {
                            val minutes = custom.toIntOrNull()
                            if (minutes == null || minutes !in AutoChangePolicy.MIN_INTERVAL_MINUTES..AutoChangePolicy.MAX_INTERVAL_MINUTES) {
                                error = intervalError
                            } else {
                                onSelect(minutes)
                            }
                        }
                    ) {
                        Text(localizedStringOr("desktop_save", "Save"))
                    }
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(localizedStringOr("desktop_cancel", "Cancel"))
                    }
                }
            }
        }
    }
}

@Composable
private fun intervalShortLabel(minutes: Int): String = when {
    minutes < 60 -> localizedString("interval_short_minutes", minutes)
    minutes % 1440 == 0 -> localizedString("interval_short_days", minutes / 1440)
    minutes % 60 == 0 -> localizedString("interval_short_hours", minutes / 60)
    else -> localizedString(
        "interval_short_hours_minutes",
        minutes / 60,
        minutes % 60
    )
}
