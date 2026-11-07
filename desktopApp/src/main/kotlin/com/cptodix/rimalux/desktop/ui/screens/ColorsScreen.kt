@file:OptIn(ExperimentalMaterial3Api::class)

package com.cptodix.rimalux.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.ui.components.ScreenHeader
import com.cptodix.rimalux.desktop.ui.dialogs.CreatePaletteDialog
import com.cptodix.rimalux.desktop.ui.localizedDisplayName
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.PaletteMode

@Composable
fun ColorsScreen(
    state: StudioUiState,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    var showCreatePalette by remember { mutableStateOf(false) }
    var deletingPalette by remember { mutableStateOf<ColorPalette?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = localizedStringOr("palettes_title", "Colors"),
            subtitle = localizedStringOr(
                "palettes_subtitle",
                "Select active color palettes for generation."
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(230.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "palette_mode_bar", span = { GridItemSpan(maxLineSpan) }) {
                val segmentedColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.config.paletteMode == PaletteMode.RANDOMIZED,
                        onClick = { controller.setPaletteMode(PaletteMode.RANDOMIZED) },
                        icon = {},
                        colors = segmentedColors,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0,
                            count = 2,
                            baseShape = MaterialTheme.shapes.medium
                        )
                    ) {
                        Text(localizedStringOr("palette_mode_random", "Randomize All"))
                    }
                    SegmentedButton(
                        selected = state.config.paletteMode == PaletteMode.PALETTE_LIST,
                        onClick = { controller.setPaletteMode(PaletteMode.PALETTE_LIST) },
                        icon = {},
                        colors = segmentedColors,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1,
                            count = 2,
                            baseShape = MaterialTheme.shapes.medium
                        )
                    ) {
                        Text(localizedStringOr("palette_mode_list", "Select Custom"))
                    }
                }
            }

            item(key = "palette_add_button", span = { GridItemSpan(maxLineSpan) }) {
                FilledTonalButton(
                    onClick = { showCreatePalette = true },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(localizedStringOr("btn_add_palette", "Add Palette"))
                }
            }

            if (state.config.paletteMode == PaletteMode.PALETTE_LIST) {
                item(key = "palette_list_hint", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = localizedString(
                                "palette_list_hint",
                                state.config.selectedPaletteIds.size,
                                state.userPalettes.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = controller::selectAllPalettes,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = localizedStringOr("btn_select_all", "Select All"),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            TextButton(
                                onClick = controller::selectNonePalettes,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = localizedStringOr("btn_select_none", "Select None"),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            items(state.userPalettes, key = { it.id }) { palette ->
                val selected = state.config.paletteMode == PaletteMode.PALETTE_LIST &&
                    palette.id in state.config.selectedPaletteIds
                PaletteListItemCard(
                    palette = palette,
                    isSelected = selected,
                    canDelete = state.userPalettes.size > 1,
                    onClick = { controller.togglePalette(palette.id) },
                    onDelete = { deletingPalette = palette }
                )
            }
        }
    }

    if (showCreatePalette) {
        CreatePaletteDialog(
            onSave = { name, colors ->
                controller.addCustomPalette(name, colors)
                showCreatePalette = false
            },
            onDismiss = { showCreatePalette = false }
        )
    }

    deletingPalette?.let { palette ->
        DeletePaletteDialog(
            palette = palette,
            onConfirm = {
                controller.deletePalette(palette.id)
                deletingPalette = null
            },
            onDismiss = { deletingPalette = null }
        )
    }
}

@Composable
private fun PaletteListItemCard(
    palette: ColorPalette,
    isSelected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = palette.localizedDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    if (canDelete) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = localizedStringOr("btn_remove_palette", "Remove"),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PaletteSolidSwatchesRow(colors = palette.colors)
        }
    }
}

@Composable
private fun PaletteSolidSwatchesRow(colors: List<Long>) {
    val safeColors = colors.ifEmpty { listOf(0xFF00F5D4L) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        safeColors.forEach { argb ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color(argb))
            )
        }
    }
}

@Composable
private fun DeletePaletteDialog(
    palette: ColorPalette,
    onConfirm: () -> Unit,
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
                    text = localizedStringOr("desktop_delete_palette", "Delete Palette"),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = localizedStringOr(
                        "desktop_delete_palette_confirm",
                        "Delete “%1\$s”? This cannot be undone.",
                        palette.localizedDisplayName()
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(localizedStringOr("desktop_cancel", "Cancel"))
                    }
                    Button(onClick = onConfirm) {
                        Text(localizedStringOr("desktop_delete_palette", "Delete Palette"))
                    }
                }
            }
        }
    }
}
