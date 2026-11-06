package com.cptodix.rimalux.desktop.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import com.cptodix.rimalux.desktop.ui.components.ScreenHeader
import com.cptodix.rimalux.desktop.ui.components.SectionCard
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
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
        ScreenHeader(
            title = localizedStringOr("desktop_settings_title", "Settings"),
            subtitle = localizedStringOr("desktop_settings_subtitle", "Desktop behavior, display output, and diagnostics"
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard(
                title = localizedStringOr("desktop_language", "Language"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = state.languageTag == language.languageTag,
                            onClick = { controller.setLanguage(language) },
                            label = { Text(language.nativeName) }
                        )
                    }
                }
            }

            SectionCard(
                title = localizedStringOr("desktop_resolution", "Wallpaper Resolution"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ResolutionMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.display.resolutionMode == mode,
                            onClick = { controller.setResolutionMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ResolutionMode.entries.size)
                        ) {
                            Text(
                                text = when (mode) {
                                    ResolutionMode.AUTO -> localizedStringOr("desktop_resolution_auto", "Auto")
                                    ResolutionMode.P1080 -> "1920"
                                    ResolutionMode.P1440 -> "2560"
                                    ResolutionMode.P2160 -> "3840"
                                    ResolutionMode.CUSTOM -> localizedStringOr("desktop_resolution_custom", "Custom")
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
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
                            }
                        ) {
                            Text(localizedStringOr("desktop_apply", "Apply"))
                        }
                    }
                }
                Text(
                    text = localizedStringOr(
                        "desktop_resolution_hint",
                        "Auto uses the display's physical resolution. Presets use the long edge."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(
                title = localizedStringOr("desktop_fit_mode", "Fit Mode"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FitMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.display.fitMode == mode,
                            onClick = { controller.setFitMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        FitMode.FILL -> localizedStringOr("desktop_fit_fill", "Fill")
                                        FitMode.FIT -> localizedStringOr("desktop_fit_fit", "Fit")
                                        FitMode.CENTER -> localizedStringOr("desktop_fit_center", "Center")
                                        FitMode.TILE -> localizedStringOr("desktop_fit_tile", "Tile")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            SectionCard(
                title = localizedStringOr("desktop_backend", "Wallpaper Backend"),
                modifier = Modifier.widthIn(max = 920.dp),
                trailing = {
                    TextButton(onClick = controller::refreshBackend) {
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
                    listOf("auto" to "Auto", "kde" to "KDE", "gnome" to "GNOME", "xfce" to "XFCE", "custom" to "Custom")
                        .forEach { (id, label) ->
                            FilterChip(
                                selected = state.ui.backendOverride == id,
                                onClick = { controller.setBackendOverride(id) },
                                label = { Text(label) }
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
                    Button(onClick = { controller.setCustomWallpaperCommand(customCommand) }) {
                        Text(localizedStringOr("desktop_apply", "Apply"))
                    }
                    OutlinedButton(
                        onClick = { controller.applyWallpaper(ApplyTarget.SELECTED_DISPLAY) },
                        enabled = state.current != null && !state.isApplying
                    ) {
                        Text(localizedStringOr("desktop_test_backend", "Test Backend"))
                    }
                }
                Text(
                    text = localizedStringOr("desktop_backend_note", "KDE Plasma supports per-display wallpapers. GNOME uses one wallpaper for all displays."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(
                title = localizedStringOr("desktop_folders", "Folders"),
                modifier = Modifier.widthIn(max = 920.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = controller::openConfigFolder) {
                        Text(localizedStringOr("desktop_open_config", "Open Config Folder"))
                    }
                    OutlinedButton(onClick = controller::openCacheFolder) {
                        Text(localizedStringOr("desktop_open_cache", "Open Cache Folder"))
                    }
                    OutlinedButton(onClick = controller::openPicturesFolder) {
                        Text(localizedStringOr("desktop_open_pictures", "Open Pictures Folder"))
                    }
                    OutlinedButton(onClick = controller::openLogsFolder) {
                        Text(localizedStringOr("desktop_open_logs", "Open Logs"))
                    }
                }
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
                    text = "${localizedStringOr("desktop_license", "License")}: GPL-3.0",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = localizedStringOr("desktop_phase2_note", "Auto-change, tray mode, and Start at Login arrive in Phase 2. Save PNG or use Apply for now."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = localizedStringOr("desktop_shortcuts", "Keyboard Shortcuts"),
                    style = MaterialTheme.typography.titleSmall
                )
                listOf(
                    "desktop_shortcut_randomize" to "Space — Randomize",
                    "desktop_shortcut_history" to "← / → — Previous / next history",
                    "desktop_shortcut_save" to "Ctrl/Cmd + S — Save PNG",
                    "desktop_shortcut_apply" to "Ctrl/Cmd + Enter — Apply wallpaper",
                    "desktop_shortcut_settings" to "Ctrl/Cmd + , — Settings"
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
