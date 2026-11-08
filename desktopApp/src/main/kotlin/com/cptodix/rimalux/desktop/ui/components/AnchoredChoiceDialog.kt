package com.cptodix.rimalux.desktop.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

data class ChoiceItem(
    val label: String,
    val summary: String? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true
)

/**
 * A settings row that opens a choice popup centered on the row's horizontal axis.
 */
@Composable
fun ChoiceRow(
    title: String,
    summary: String,
    items: List<ChoiceItem>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dialogTitle: String = title
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorCenterXPx by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                anchorCenterXPx = coordinates.boundsInWindow().center.x
            }
    ) {
        Surface(
            onClick = { expanded = true },
            color = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnchoredChoiceDialog(
            visible = expanded,
            anchorCenterXPx = anchorCenterXPx,
            title = dialogTitle,
            items = items,
            onSelect = { index ->
                onSelect(index)
                expanded = false
            },
            onDismiss = { expanded = false }
        )
    }
}

@Composable
fun AnchoredChoiceDialog(
    visible: Boolean,
    anchorCenterXPx: Float,
    title: String,
    items: List<ChoiceItem>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AnchoredDialog(
        visible = visible,
        anchorCenterXPx = anchorCenterXPx,
        onDismiss = onDismiss
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        items.forEachIndexed { index, item ->
            Surface(
                onClick = { onSelect(index) },
                enabled = item.enabled,
                shape = MaterialTheme.shapes.medium,
                color = if (item.selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                border = BorderStroke(
                    width = if (item.selected) 1.dp else 0.5.dp,
                    color = if (item.selected) {
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
                            text = item.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        item.summary?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (item.selected) {
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

/**
 * A centered dialog whose horizontal center is aligned with the triggering row.
 */
@Composable
fun AnchoredDialog(
    visible: Boolean,
    anchorCenterXPx: Float,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    if (!visible) return

    var scrimDismissEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(180)
        scrimDismissEnabled = true
    }

    Popup(
        popupPositionProvider = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = IntOffset.Zero
            }
        },
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (scrimDismissEnabled) onDismiss() }
                )
        ) {
            val density = LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val dialogWidth = minOf(360.dp, maxWidth - 32.dp)
            val dialogWidthPx = with(density) { dialogWidth.toPx() }
            val marginPx = with(density) { 16.dp.toPx() }
            val maxOffsetPx = ((screenWidthPx - dialogWidthPx) / 2f - marginPx)
                .coerceAtLeast(0f)
            val offsetPx = (anchorCenterXPx - screenWidthPx / 2f)
                .coerceIn(-maxOffsetPx, maxOffsetPx)

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(offsetPx.roundToInt(), 0) }
                    .width(dialogWidth)
                    .heightIn(max = maxHeight - 32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}
