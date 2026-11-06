@file:OptIn(ExperimentalMaterial3Api::class)

package com.cptodix.rimalux.desktop.ui.screens

import com.cptodix.rimalux.desktop.ui.localizedStringOr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.ui.components.PaletteSwatches
import com.cptodix.rimalux.desktop.ui.components.ScreenHeader
import com.cptodix.rimalux.desktop.ui.dialogs.CreatePaletteDialog
import com.cptodix.rimalux.desktop.ui.localizedDisplayName
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.PaletteMode

@OptIn(ExperimentalMaterial3Api::class)
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
            title = localizedStringOr("desktop_colors_title", "Colors"),
            subtitle = localizedStringOr("desktop_colors_subtitle", "Curated OLED palettes and color harmony"
            )
        ) {
            OutlinedButton(onClick = { showCreatePalette = true }) {
                Text(localizedStringOr("desktop_create_palette", "Create Palette"))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedStringOr("desktop_color_fill", "Color Fill"),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.config.colorFillMode == ColorFillMode.SOLID,
                            onClick = { controller.setColorFillMode(ColorFillMode.SOLID) },
                            label = { Text(localizedStringOr("desktop_fill_solid", "Solid")) }
                        )
                        FilterChip(
                            selected = state.config.colorFillMode == ColorFillMode.GRADIENT,
                            onClick = { controller.setColorFillMode(ColorFillMode.GRADIENT) },
                            label = { Text(localizedStringOr("desktop_fill_gradient", "Gradient")) }
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedStringOr("desktop_palette_mode", "Palette Mode"),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.config.paletteMode == PaletteMode.RANDOMIZED,
                            onClick = { controller.setPaletteMode(PaletteMode.RANDOMIZED) },
                            label = { Text(localizedStringOr("desktop_random_harmony", "Random Harmony")) }
                        )
                        FilterChip(
                            selected = state.config.paletteMode == PaletteMode.PALETTE_LIST,
                            onClick = { controller.setPaletteMode(PaletteMode.PALETTE_LIST) },
                            label = { Text(localizedStringOr("desktop_my_palettes", "My Palettes")) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.config.paletteMode == PaletteMode.RANDOMIZED) {
                    Button(onClick = controller::shuffleColors) {
                        Text(localizedStringOr("desktop_shuffle_harmony", "Shuffle Harmony"))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = localizedString(
                        "desktop_palette_selected_count",
                        state.config.selectedPaletteIds.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(230.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.userPalettes, key = { it.id }) { palette ->
                    PaletteCard(
                        palette = palette,
                        selected = palette.id in state.config.selectedPaletteIds,
                        custom = palette.id.startsWith("custom_"),
                        onToggle = { controller.togglePalette(palette.id) },
                        onDelete = { deletingPalette = palette }
                    )
                }
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
private fun PaletteCard(
    palette: ColorPalette,
    selected: Boolean,
    custom: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.dp else 0.5.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = palette.localizedDisplayName(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (custom) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = localizedStringOr("desktop_delete_palette", "Delete Palette"
                            ),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            PaletteSwatches(palette = palette, swatchSize = 20)
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
                    text = localizedStringOr("desktop_delete_palette_confirm", "Delete “%1\$s”? This cannot be undone.",
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
