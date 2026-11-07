@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.cptodix.rimalux.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.cptodix.rimalux.desktop.ui.components.PatternThumbnail
import com.cptodix.rimalux.desktop.ui.components.ScreenHeader
import com.cptodix.rimalux.desktop.ui.localizedLabel
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.desktop.ui.localizedSubtitle
import com.cptodix.rimalux.desktop.ui.localizedTitle
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.PatternCategory

@Composable
fun PatternsScreen(
    state: StudioUiState,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<PatternCategory?>(null) }

    val filteredPatterns = remember(selectedCategory, state.config.patternsRandomized) {
        if (state.config.patternsRandomized || selectedCategory == null) {
            ArtPattern.entries.toList()
        } else {
            ArtPattern.entries.filter { it.category == selectedCategory }
        }
    }
    val currentlyRendered = state.selectedRenderedItem?.item?.pattern

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = localizedStringOr("patterns_title", "Patterns"),
            subtitle = localizedString(
                "patterns_subtitle",
                ArtPattern.entries.size
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "patterns_mode_bar", span = { GridItemSpan(maxLineSpan) }) {
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
                        selected = state.config.patternsRandomized,
                        onClick = { controller.setPatternsRandomized(true) },
                        icon = {},
                        colors = segmentedColors,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0,
                            count = 2,
                            baseShape = MaterialTheme.shapes.medium
                        )
                    ) {
                        Text(localizedStringOr("mode_randomized", "Randomize All"))
                    }
                    SegmentedButton(
                        selected = !state.config.patternsRandomized,
                        onClick = { controller.setPatternsRandomized(false) },
                        icon = {},
                        colors = segmentedColors,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1,
                            count = 2,
                            baseShape = MaterialTheme.shapes.medium
                        )
                    ) {
                        Text(localizedStringOr("mode_multiselect", "Select Custom"))
                    }
                }
            }

            if (!state.config.patternsRandomized) {
                item(key = "patterns_category_filters", span = { GridItemSpan(maxLineSpan) }) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allSelected = selectedCategory == null
                        FilterChip(
                            selected = allSelected,
                            onClick = { selectedCategory = null },
                            leadingIcon = if (allSelected) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            },
                            label = {
                                Text(
                                    localizedString(
                                        "cat_all",
                                        ArtPattern.entries.size
                                    )
                                )
                            }
                        )
                        PatternCategory.entries.forEach { category ->
                            val picked = selectedCategory == category
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedCategory = if (picked) null else category
                                },
                                leadingIcon = if (picked) {
                                    {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = { Text(category.localizedLabel()) }
                            )
                        }
                    }
                }

                item(key = "patterns_hint", span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = localizedString(
                                "patterns_multiselect_hint",
                                state.config.selectedPatterns.size,
                                ArtPattern.entries.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = controller::selectAllPatterns,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = localizedStringOr("btn_select_all", "Select All"),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            TextButton(
                                onClick = controller::selectNonePatterns,
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

            items(filteredPatterns, key = { it.id }) { pattern ->
                val isSelected = !state.config.patternsRandomized &&
                    pattern in state.config.selectedPatterns
                PatternGalleryCard(
                    pattern = pattern,
                    isSelected = isSelected,
                    isCurrentlyRendered = currentlyRendered == pattern,
                    onClick = { controller.togglePattern(pattern) }
                )
            }
        }
    }
}

@Composable
private fun PatternGalleryCard(
    pattern: ArtPattern,
    isSelected: Boolean,
    isCurrentlyRendered: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = when {
            isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
            isCurrentlyRendered -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = when {
            isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            isCurrentlyRendered -> BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            )
            else -> BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
            )
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                PatternThumbnail(
                    patternKey = "pattern_preview_${pattern.id}",
                    contentDescription = pattern.localizedTitle(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(22.dp)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = pattern.localizedTitle(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pattern.localizedSubtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
