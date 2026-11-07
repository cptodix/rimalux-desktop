package com.cptodix.rimalux.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.RenderedItem
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.ui.dialogs.DisplayLayoutDialog
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.desktop.ui.localizedTitle

@Composable
fun StudioScreen(
    state: StudioUiState,
    controller: StudioController,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDisplayLayout by remember { mutableStateOf(false) }
    val rendered = state.selectedRenderedItem

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (rendered != null) {
            Image(
                bitmap = rendered.preview,
                contentDescription = rendered.item.pattern.localizedTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.previewError
                        ?: localizedStringOr("desktop_generating", "Generating wallpaper…"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bottom readability scrim. The wallpaper remains the centerpiece.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.18f),
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )

        if (rendered != null) {
            PreviewInfoPill(
                rendered = rendered,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 112.dp)
            )
        }

        if (state.isGenerating) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = localizedStringOr("desktop_generating", "Generating wallpaper…"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            OptionsButton(
                onClick = onOpenOptions,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            MediaControls(
                state = state,
                controller = controller,
                modifier = Modifier.align(Alignment.Center)
            )

            DisplayControls(
                state = state,
                onSelectMonitor = controller::selectMonitor,
                onOpenLayout = { showDisplayLayout = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
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
private fun PreviewInfoPill(
    rendered: RenderedItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(alpha = 0.68f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = rendered.item.pattern.localizedTitle(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rendered.item.paletteColors.take(4).forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    )
                }
            }
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f)
            )
            Text(
                text = localizedString(
                    "preview_true_black_percent",
                    rendered.measuredBlackPercent
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OptionsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
    ) {
        Icon(
            imageVector = Icons.Rounded.Menu,
            contentDescription = localizedStringOr("desktop_open_options", "Open options"),
            tint = Color.White
        )
    }
}

@Composable
private fun MediaControls(
    state: StudioUiState,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.58f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = controller::previous,
                enabled = state.canGoBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = localizedStringOr(
                        "desktop_previous_wallpaper",
                        "Previous wallpaper"
                    ),
                    tint = if (state.canGoBack) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }

            Surface(
                onClick = { controller.applyWallpaper(ApplyTarget.ALL_DISPLAYS) },
                enabled = state.current != null && !state.isApplying,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Wallpaper,
                            contentDescription = localizedStringOr(
                                "desktop_apply_wallpaper",
                                "Apply Wallpaper"
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { controller.savePngs(ApplyTarget.ALL_DISPLAYS) },
                enabled = state.current != null && !state.isSaving,
                modifier = Modifier.size(48.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = localizedStringOr("desktop_save_png", "Save PNG"),
                        tint = if (state.current != null) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            IconButton(
                onClick = controller::next,
                modifier = Modifier.size(48.dp)
            ) {
                if (state.isGenerating && state.aheadReadyCount == 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = localizedStringOr(
                            "desktop_next_wallpaper",
                            "Next wallpaper"
                        ),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayControls(
    state: StudioUiState,
    onSelectMonitor: (String) -> Unit,
    onOpenLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.selectedMonitor

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Monitor,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selected?.name ?: localizedStringOr("desktop_display", "Display"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        IconButton(
            onClick = onOpenLayout,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = localizedStringOr(
                    "desktop_display_layout",
                    "Display Layout"
                ),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.monitors.forEach { monitor ->
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
                        onSelectMonitor(monitor.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
