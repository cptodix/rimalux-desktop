package com.cptodix.rimalux.desktop.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.cptodix.rimalux.desktop.model.DisplaySettings
import com.cptodix.rimalux.desktop.model.MonitorInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.BuildInfo
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.model.AppLanguage
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.model.FitMode
import com.cptodix.rimalux.desktop.model.ResolutionMode
import com.cptodix.rimalux.desktop.ui.components.ChoiceItem
import com.cptodix.rimalux.desktop.ui.components.ChoiceRow
import com.cptodix.rimalux.desktop.ui.components.SectionCard
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: StudioUiState,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    var customWidth by remember(state.display.customWidth) {
        mutableStateOf(state.display.customWidth.toString())
    }
    var customHeight by remember(state.display.customHeight) {
        mutableStateOf(state.display.customHeight.toString())
    }
    var customCommand by remember(state.ui.customWallpaperCommand) {
        mutableStateOf(state.ui.customWallpaperCommand)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = localizedStringOr("desktop_settings_title", "Advanced Settings"),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = localizedStringOr(
                    "desktop_settings_subtitle",
                    "Advanced desktop behavior, display output, and diagnostics"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 920.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
            ) {
                Column {
                    ChoiceRow(
                        title = localizedStringOr("desktop_language", "Language"),
                        summary = AppLanguage.entries
                            .firstOrNull { it.languageTag == state.languageTag }
                            ?.nativeName
                            ?: state.languageTag,
                        items = AppLanguage.entries.map { language ->
                            ChoiceItem(
                                label = language.nativeName,
                                selected = state.languageTag == language.languageTag
                            )
                        },
                        onSelect = { index -> controller.setLanguage(AppLanguage.entries[index]) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                    )
                    ChoiceRow(
                        title = localizedStringOr("desktop_resolution", "Wallpaper Resolution"),
                        summary = resolutionModeName(state.display.resolutionMode) +
                            " · " + resolutionLabelFor(
                                state.display.resolutionMode,
                                state.selectedMonitor,
                                state.display
                            ),
                        items = ResolutionMode.entries.map { mode ->
                            ChoiceItem(
                                label = resolutionModeName(mode),
                                summary = resolutionLabelFor(mode, state.selectedMonitor, state.display),
                                selected = state.display.resolutionMode == mode
                            )
                        },
                        onSelect = { index ->
                            controller.setResolutionMode(ResolutionMode.entries[index])
                        }
                    )
                    if (state.display.resolutionMode == ResolutionMode.CUSTOM) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customWidth,
                                onValueChange = { customWidth = it.filter(Char::isDigit).take(5) },
                                label = { Text(localizedStringOr("desktop_custom_width", "Width")) },
                                singleLine = true,
                                modifier = Modifier.widthIn(min = 120.dp, max = 180.dp)
                            )
                            OutlinedTextField(
                                value = customHeight,
                                onValueChange = { customHeight = it.filter(Char::isDigit).take(5) },
                                label = { Text(localizedStringOr("desktop_custom_height", "Height")) },
                                singleLine = true,
                                modifier = Modifier.widthIn(min = 120.dp, max = 180.dp)
                            )
                            Button(
                                onClick = {
                                    val w = customWidth.toIntOrNull() ?: 2560
                                    val h = customHeight.toIntOrNull() ?: 1440
                                    controller.setCustomResolution(w, h)
                                },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(localizedStringOr("desktop_apply", "Apply"))
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                    )
                    ChoiceRow(
                        title = localizedStringOr("desktop_fit_mode", "Fit Mode"),
                        summary = fitModeLabel(state.display.fitMode),
                        items = FitMode.entries.map { mode ->
                            ChoiceItem(
                                label = fitModeLabel(mode),
                                selected = state.display.fitMode == mode
                            )
                        },
                        onSelect = { index -> controller.setFitMode(FitMode.entries[index]) }
                    )
                    Text(
                        text = localizedStringOr(
                            "desktop_resolution_hint",
                            "Auto Match uses each display's reported resolution. Presets use the long edge."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            SectionCard(
                title = localizedStringOr("desktop_backend", "Wallpaper Backend"),
                modifier = Modifier.widthIn(max = 920.dp),
                trailing = {
                    TextButton(
                        onClick = controller::refreshBackend,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(localizedStringOr("desktop_refresh_backend", "Refresh Backend"))
                    }
                }
            ) {
                val info = state.backendInfo
                Text(
                    text = info.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (info.available) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = (if (info.available) {
                        localizedStringOr("desktop_backend_available", "Available")
                    } else {
                        localizedStringOr("desktop_backend_unavailable", "Unavailable")
                    }) + " · " + (if (info.supportsPerMonitor) {
                        localizedStringOr("desktop_per_monitor_yes", "Per-display supported")
                    } else {
                        localizedStringOr("desktop_per_monitor_no", "One wallpaper for all displays")
                    }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (info.detail.isNotBlank()) {
                    Text(
                        text = info.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isWindows = System.getProperty("os.name").orEmpty()
                        .lowercase()
                        .contains("win")
                    val backendOptions = buildList {
                        add("auto" to "Auto")
                        if (isWindows) {
                            add("windows" to localizedStringOr("desktop_windows", "Windows"))
                        } else {
                            add("kde" to "KDE")
                            add("gnome" to "GNOME")
                            add("xfce" to "XFCE")
                        }
                        add("custom" to localizedStringOr("desktop_custom", "Custom"))
                    }
                    backendOptions.forEach { (id, label) ->
                        FilterChip(
                            selected = state.ui.backendOverride == id,
                            onClick = { controller.setBackendOverride(id) },
                            label = { Text(label) },
                            leadingIcon = if (state.ui.backendOverride == id) {
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
                OutlinedTextField(
                    value = customCommand,
                    onValueChange = { customCommand = it },
                    label = { Text(localizedStringOr("desktop_custom_command", "Custom Command")) },
                    placeholder = {
                        Text(
                            localizedStringOr("desktop_custom_command_hint", "Use {file} and {monitor}. Example: feh --bg-fill {file}"
                            )
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { controller.setCustomWallpaperCommand(customCommand) },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_apply", "Apply"))
                    }
                    OutlinedButton(
                        onClick = { controller.applyWallpaper(ApplyTarget.SELECTED_DISPLAY) },
                        enabled = state.current != null && !state.isApplying,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_test_backend", "Test Backend"))
                    }
                }
                Text(
                    text = if (state.backendInfo.id == "windows") {
                        localizedStringOr(
                            "desktop_backend_note_windows",
                            "Windows uses IDesktopWallpaper for per-display wallpapers and SystemParametersInfoW as a global fallback."
                        )
                    } else {
                        localizedStringOr(
                            "desktop_backend_note",
                            "KDE Plasma supports per-display wallpapers. GNOME uses one wallpaper for all displays."
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(
                title = localizedStringOr("desktop_displays", "Displays"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                state.monitors.forEach { monitor ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = monitor.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${monitor.widthPx}×${monitor.heightPx}" +
                                    if (monitor.isPrimary) {
                                        " · " + localizedStringOr("desktop_primary", "Primary")
                                    } else {
                                        ""
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { controller.identifyMonitor(monitor.id) },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(localizedStringOr("desktop_identify_display", "Identify"))
                        }
                    }
                }
                Text(
                    text = localizedStringOr(
                        "desktop_displays_hint",
                        "Identify shows the display name and resolution over the preview."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(
                title = localizedStringOr("desktop_folders", "Folders"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FolderShortcutButton(
                            label = localizedStringOr("desktop_open_config", "Open Config Folder"),
                            icon = Icons.Rounded.Settings,
                            onClick = controller::openConfigFolder,
                            modifier = Modifier.weight(1f)
                        )
                        FolderShortcutButton(
                            label = localizedStringOr("desktop_open_cache", "Open Cache Folder"),
                            icon = Icons.Rounded.Folder,
                            onClick = controller::openCacheFolder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FolderShortcutButton(
                            label = localizedStringOr("desktop_open_pictures", "Open Pictures Folder"),
                            icon = Icons.Rounded.Image,
                            onClick = controller::openPicturesFolder,
                            modifier = Modifier.weight(1f)
                        )
                        FolderShortcutButton(
                            label = localizedStringOr("desktop_open_logs", "Open Logs"),
                            icon = Icons.Rounded.Description,
                            onClick = controller::openLogsFolder,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SectionCard(
                title = localizedStringOr("desktop_auto_status_title", "Auto-Change Status"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                val auto = state.autoChange
                Text(
                    text = autoChangeStatusText(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (auto.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (auto.enabled) {
                    if (auto.nextRunEpochMillis > 0L) {
                        Text(
                            text = localizedString(
                                "desktop_auto_next",
                                formatTimestamp(auto.nextRunEpochMillis)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (auto.lastRunEpochMillis > 0L) {
                        Text(
                            text = localizedString(
                                "desktop_auto_last",
                                formatTimestamp(auto.lastRunEpochMillis)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = localizedStringOr("desktop_auto_never", "Not yet"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    auto.lastError?.let { error ->
                        Text(
                            text = localizedString("desktop_auto_error", error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Button(
                    onClick = controller::changeWallpaperNow,
                    enabled = state.current != null && !state.isApplying,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = localizedStringOr("desktop_auto_change_now", "Change Now"),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            SectionCard(
                title = localizedStringOr("desktop_os_scheduler", "Background Scheduler"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Text(
                    text = if (state.osSchedulerInstalled) {
                        localizedStringOr("desktop_os_scheduler_on", "Installed — rotates even when Rimalux is closed.")
                    } else {
                        localizedStringOr("desktop_os_scheduler_off", "Not installed — the in-app tray scheduler is used.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.osSchedulerInstalled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = controller::installOsScheduler,
                        enabled = !state.osSchedulerInstalled,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_os_scheduler_install", "Install Scheduler"))
                    }
                    OutlinedButton(
                        onClick = controller::removeOsScheduler,
                        enabled = state.osSchedulerInstalled,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_os_scheduler_remove", "Remove Scheduler"))
                    }
                }
                Text(
                    text = localizedStringOr(
                        "desktop_os_scheduler_hint",
                        "Uses a systemd user timer on Linux or Task Scheduler on Windows. It runs --rotate-now at the auto-change interval."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(
                title = localizedStringOr("desktop_support_title", "Support"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { controller.checkForUpdates() },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_check_updates", "Check for Updates"))
                    }
                    OutlinedButton(
                        onClick = controller::exportDiagnostics,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_export_diagnostics", "Export Diagnostics"))
                    }
                }
                state.updateInfo?.let { update ->
                    Text(
                        text = localizedStringOr(
                            "desktop_update_versions",
                            "Installed ${update.currentVersion} · Latest ${update.latestVersion}",
                            update.currentVersion,
                            update.latestVersion
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (update.updateAvailable) {
                        Button(
                            onClick = controller::installUpdate,
                            enabled = !state.isInstallingUpdate,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (state.isInstallingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    localizedStringOr(
                                        "desktop_install_update",
                                        "Install Update ${update.latestVersion}",
                                        update.latestVersion
                                    )
                                )
                            }
                        }
                    }
                }
                Text(
                    text = localizedStringOr(
                        "desktop_diagnostics_hint",
                        "Diagnostics bundles include system info, redacted settings, monitor/backend state, and recent logs."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(
                title = localizedStringOr("desktop_about", "About"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Text(
                    text = localizedStringOr("desktop_about_body", "Rimalux Desktop is a procedural OLED wallpaper studio ported from the Android app. The mathematical engine is shared; the desktop UI, renderer, and wallpaper backends are native to Linux and Windows."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${localizedStringOr("desktop_version", "Version")}: ${BuildInfo.VERSION}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${localizedStringOr("desktop_engine_commit", "Engine Commit")}: ${BuildInfo.engineCommit}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${localizedStringOr("desktop_license", "License")}: " +
                        localizedStringOr("desktop_license_name", "PolyForm Perimeter 1.0.1"),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "https://polyformproject.org/licenses/perimeter/1.0.1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = localizedStringOr(
                        "desktop_shortcuts",
                        "Keyboard Shortcuts"
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                listOf(
                    "desktop_shortcut_randomize" to "Space — Randomize",
                    "desktop_shortcut_history" to "← / → — Previous / next history",
                    "desktop_shortcut_save" to "Ctrl/Cmd + S — Save PNG",
                    "desktop_shortcut_apply" to "Ctrl/Cmd + Enter — Apply wallpaper",
                    "desktop_shortcut_settings" to "Ctrl/Cmd + , — Settings",
                    "desktop_shortcut_fullscreen" to "F11 or double-click — Fullscreen preview"
                ).forEach { (key, fallback) ->
                    Text(
                        text = localizedStringOr(key, fallback),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "© 2026 Cptodix Studio · Engine ${BuildInfo.engineCommit.take(8)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun resolutionModeName(mode: ResolutionMode): String = when (mode) {
    ResolutionMode.AUTO -> localizedStringOr("desktop_resolution_auto_match", "Auto Match")
    ResolutionMode.P1080 -> "1920"
    ResolutionMode.P1440 -> "2560"
    ResolutionMode.P2160 -> "3840"
    ResolutionMode.CUSTOM -> localizedStringOr("desktop_resolution_custom", "Custom")
}

private fun resolutionLabelFor(
    mode: ResolutionMode,
    monitor: MonitorInfo?,
    display: DisplaySettings
): String {
    val (width, height) = when (mode) {
        ResolutionMode.AUTO -> {
            if (monitor == null || monitor.widthPx <= 0 || monitor.heightPx <= 0) {
                3840 to 2160
            } else {
                monitor.widthPx to monitor.heightPx
            }
        }
        ResolutionMode.P1080 -> presetResolution(monitor, 1920)
        ResolutionMode.P1440 -> presetResolution(monitor, 2560)
        ResolutionMode.P2160 -> presetResolution(monitor, 3840)
        ResolutionMode.CUSTOM -> display.customWidth to display.customHeight
    }
    return "$width×$height"
}

private fun presetResolution(monitor: MonitorInfo?, longEdge: Int): Pair<Int, Int> {
    val aspect = monitor?.aspectRatio ?: (9f / 16f)
    return if (aspect <= 1f) {
        longEdge to (longEdge * aspect).roundToInt().coerceAtLeast(240)
    } else {
        (longEdge / aspect).roundToInt().coerceAtLeast(320) to longEdge
    }
}

@Composable
private fun FolderShortcutButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun autoChangeStatusText(state: StudioUiState): String {
    val auto = state.autoChange
    if (!auto.enabled) {
        return localizedStringOr("desktop_auto_status_off", "Auto-change is off.")
    }
    return when {
        state.osSchedulerInstalled && auto.keepInTray ->
            localizedStringOr(
                "desktop_auto_status_scheduler_tray",
                "Active via the background scheduler and the tray."
            )
        state.osSchedulerInstalled ->
            localizedStringOr(
                "desktop_auto_status_scheduler",
                "Active via the background scheduler."
            )
        auto.keepInTray && auto.runAtLogin ->
            localizedStringOr(
                "desktop_auto_status_tray_login",
                "Active in the tray, including after sign-in."
            )
        auto.keepInTray ->
            localizedStringOr(
                "desktop_auto_status_tray",
                "Active while Rimalux is in the tray."
            )
        auto.runAtLogin ->
            localizedStringOr(
                "desktop_auto_status_login",
                "Active while Rimalux is open; Start at Login launches it in the background."
            )
        else ->
            localizedStringOr(
                "desktop_auto_status_open",
                "Active while Rimalux is open."
            )
    }
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(millis))

@Composable
private fun fitModeLabel(mode: FitMode): String = when (mode) {
    FitMode.FILL -> localizedStringOr("desktop_fit_fill", "Fill")
    FitMode.FIT -> localizedStringOr("desktop_fit_fit", "Fit")
    FitMode.CENTER -> localizedStringOr("desktop_fit_center", "Center")
    FitMode.TILE -> localizedStringOr("desktop_fit_tile", "Tile")
}
