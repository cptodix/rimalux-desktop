package com.cptodix.rimalux.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.RenderedItem
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.ui.components.AnchoredChoiceDialog
import com.cptodix.rimalux.desktop.ui.components.ChoiceItem
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.desktop.ui.localizedTitle
import kotlinx.coroutines.delay

@Composable
fun StudioScreen(
    state: StudioUiState,
    controller: StudioController,
    fullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rendered = state.selectedRenderedItem

    LaunchedEffect(state.identifyMonitorId) {
        if (state.identifyMonitorId != null) {
            delay(2_600L)
            controller.clearIdentify()
        }
    }

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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onToggleFullscreen() })
                    }
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

        if (!fullscreen) {
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

            }
        }

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
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.5.dp, Color.White.copy(alpha = 0.16f), MaterialTheme.shapes.medium)
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
    var applyDialogVisible by remember { mutableStateOf(false) }
    var applyAnchorX by remember { mutableStateOf(0f) }
    var applyAnchorTop by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
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

            val applyLabel = localizedStringOr("desktop_apply_wallpaper", "Apply Wallpaper")
            Surface(
                onClick = { applyDialogVisible = true },
                enabled = state.current != null && !state.isApplying,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .onGloballyPositioned {
                        val bounds = it.boundsInWindow()
                        applyAnchorX = bounds.center.x
                        applyAnchorTop = bounds.top
                    }
                    .semantics { contentDescription = applyLabel }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isApplying || state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ThickCheck(
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
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

    ApplyDialog(
        visible = applyDialogVisible,
        anchorCenterXPx = applyAnchorX,
        anchorTopPx = applyAnchorTop,
        state = state,
        controller = controller,
        onDismiss = { applyDialogVisible = false }
    )
}

/** Thick, round-capped checkmark used on the circular apply button. */
@Composable
private fun ThickCheck(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.17f
        val elbow = Offset(size.width * 0.42f, size.height * 0.76f)
        drawLine(
            color = tint,
            start = Offset(size.width * 0.18f, size.height * 0.52f),
            end = elbow,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = elbow,
            end = Offset(size.width * 0.84f, size.height * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Apply targets plus export, shown as an anchored modal centered on the apply button.
 */
@Composable
private fun ApplyDialog(
    visible: Boolean,
    anchorCenterXPx: Float,
    anchorTopPx: Float,
    state: StudioUiState,
    controller: StudioController,
    onDismiss: () -> Unit
) {
    val monitors = state.monitors
    val items = buildList {
        monitors.forEach { monitor ->
            add(
                ChoiceItem(
                    label = monitor.name,
                    summary = buildString {
                        append("${monitor.widthPx}×${monitor.heightPx}")
                        if (monitor.isPrimary) {
                            append(" · ")
                            append(localizedStringOr("desktop_primary", "Primary"))
                        }
                    }
                )
            )
        }
        add(
            ChoiceItem(
                label = localizedStringOr("desktop_all_displays", "All Displays"),
                summary = localizedStringOr(
                    "desktop_apply_all_desc",
                    "Same wallpaper on every display"
                )
            )
        )
        add(
            ChoiceItem(
                label = localizedStringOr(
                    "desktop_apply_all_separate",
                    "All Displays Separate"
                ),
                summary = if (state.backendInfo.supportsPerMonitor) {
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
                enabled = state.backendInfo.supportsPerMonitor
            )
        )
        add(
            ChoiceItem(
                label = localizedStringOr("desktop_save_wallpaper", "Save Wallpaper"),
                summary = localizedStringOr(
                    "desktop_save_wallpaper_desc",
                    "Export PNG files for every display"
                ),
                enabled = state.current != null && !state.isSaving
            )
        )
    }

    AnchoredChoiceDialog(
        visible = visible,
        anchorCenterXPx = anchorCenterXPx,
        title = localizedStringOr("desktop_apply_to", "Apply To"),
        items = items,
        anchorTopPx = anchorTopPx,
        showCancel = false,
        separatorBeforeIndex = monitors.size + 2,
        onSelect = { index ->
            when {
                index < monitors.size ->
                    controller.applyWallpaper(ApplyTarget.SELECTED_DISPLAY, monitors[index].id)

                index == monitors.size ->
                    controller.applyWallpaper(ApplyTarget.ALL_DISPLAYS)

                index == monitors.size + 1 ->
                    controller.applyWallpaper(ApplyTarget.ALL_DISPLAYS_SEPARATE)

                else -> controller.savePngs(ApplyTarget.ALL_DISPLAYS)
            }
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
