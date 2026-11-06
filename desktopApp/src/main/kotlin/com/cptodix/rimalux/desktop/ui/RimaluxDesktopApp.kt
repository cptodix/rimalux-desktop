package com.cptodix.rimalux.desktop.ui

import com.cptodix.rimalux.desktop.ui.localizedStringOr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.BuildInfo
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.model.DesktopScreen
import com.cptodix.rimalux.desktop.ui.components.BackendBadge
import com.cptodix.rimalux.desktop.ui.components.StatusSnackbar
import com.cptodix.rimalux.desktop.ui.screens.ColorsScreen
import com.cptodix.rimalux.desktop.ui.screens.PatternsScreen
import com.cptodix.rimalux.desktop.ui.screens.SettingsScreen
import com.cptodix.rimalux.desktop.ui.screens.StudioScreen
import kotlinx.coroutines.delay

@Composable
fun RimaluxDesktopApp(
    state: StudioUiState,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            delay(3_200L)
            controller.dismissStatus()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Spacer(modifier = Modifier.size(14.dp))
                Text(
                    text = "R",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Rimalux",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                NavigationDestination.entries.forEach { destination ->
                    NavigationRailItem(
                        selected = state.screen == destination.screen,
                        onClick = { controller.setScreen(destination.screen) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label()
                            )
                        },
                        label = { Text(destination.label()) }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (state.screen) {
                        DesktopScreen.STUDIO -> StudioScreen(
                            state = state,
                            controller = controller,
                            onNavigate = controller::setScreen
                        )

                        DesktopScreen.PATTERNS -> PatternsScreen(
                            state = state,
                            controller = controller
                        )

                        DesktopScreen.COLORS -> ColorsScreen(
                            state = state,
                            controller = controller
                        )

                        DesktopScreen.SETTINGS -> SettingsScreen(
                            state = state,
                            controller = controller
                        )
                    }
                }
                StatusBar(state = state)
            }
        }

        StatusSnackbar(
            message = state.statusMessage,
            onDismiss = controller::dismissStatus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 96.dp, end = 24.dp, bottom = 54.dp)
        )
    }
}

@Composable
private fun StatusBar(state: StudioUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackendBadge(state)
            Text(
                text = localizedString("desktop_display_count", state.monitors.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (state.backendInfo.supportsPerMonitor) {
                    localizedStringOr("desktop_per_monitor_yes", "Per-display supported")
                } else {
                    localizedStringOr("desktop_per_monitor_no", "One wallpaper for all displays")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "v${BuildInfo.VERSION} · ${localizedStringOr("desktop_phase_badge", "Phase 1 · Desktop MVP")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class NavigationDestination(
    val screen: DesktopScreen,
    val icon: ImageVector
) {
    STUDIO(DesktopScreen.STUDIO, Icons.Rounded.Brush),
    PATTERNS(DesktopScreen.PATTERNS, Icons.Rounded.GridView),
    COLORS(DesktopScreen.COLORS, Icons.Rounded.Palette),
    SETTINGS(DesktopScreen.SETTINGS, Icons.Rounded.Settings);

    @Composable
    fun label(): String = when (this) {
        STUDIO -> localizedStringOr("desktop_nav_studio", "Studio")
        PATTERNS -> localizedStringOr("desktop_nav_patterns", "Patterns")
        COLORS -> localizedStringOr("desktop_nav_colors", "Colors")
        SETTINGS -> localizedStringOr("desktop_nav_settings", "Settings")
    }
}
