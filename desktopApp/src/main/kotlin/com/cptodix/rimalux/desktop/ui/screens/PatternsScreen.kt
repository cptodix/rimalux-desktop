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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.ui.components.PatternThumbnail
import com.cptodix.rimalux.desktop.ui.components.ScreenHeader
import com.cptodix.rimalux.desktop.ui.components.SectionCard
import com.cptodix.rimalux.desktop.ui.localizedLabel
import com.cptodix.rimalux.desktop.ui.localizedString
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
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PatternCategory?>(null) }

    val filtered = remember(query, category) {
        ArtPattern.entries.filter { pattern ->
            (category == null || pattern.category == category) &&
                (query.isBlank() ||
                    pattern.defaultTitle.contains(query, ignoreCase = true) ||
                    pattern.id.contains(query, ignoreCase = true))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = localizedStringOr("desktop_patterns_title", "Patterns"),
            subtitle = localizedStringOr("desktop_patterns_subtitle", "Choose the procedural geometry pool"
            )
        ) {
            OutlinedButton(onClick = controller::selectAllPatterns) {
                Text(localizedStringOr("desktop_select_all", "Select All"))
            }
            OutlinedButton(onClick = controller::selectNonePatterns) {
                Text(localizedStringOr("desktop_select_none", "Select None"))
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = {
                        Text(localizedStringOr("desktop_search_patterns", "Search patterns…"))
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = localizedString(
                        "desktop_selected_count",
                        state.config.selectedPatterns.size
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text(localizedStringOr("desktop_category_all", "All")) }
                )
                PatternCategory.entries.forEach { entry ->
                    FilterChip(
                        selected = category == entry,
                        onClick = { category = entry },
                        label = { Text(entry.localizedLabel()) }
                    )
                }
            }

            SectionCard(
                title = localizedStringOr("desktop_randomize_patterns", "Randomize Patterns"),
                subtitle = localizedStringOr("desktop_randomize_patterns_desc", "When enabled, every new wallpaper can use any pattern."
                ),
                trailing = {
                    Switch(
                        checked = state.config.patternsRandomized,
                        onCheckedChange = controller::setPatternsRandomized
                    )
                }
            ) {
                Text(
                    text = if (state.config.patternsRandomized) {
                        localizedStringOr("desktop_all_patterns", "All patterns")
                    } else {
                        localizedString(
                            "desktop_selected_count",
                            state.config.selectedPatterns.size
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(190.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { pattern ->
                    PatternCard(
                        pattern = pattern,
                        selected = pattern in state.config.selectedPatterns,
                        selectionEnabled = !state.config.patternsRandomized,
                        onToggle = { controller.togglePattern(pattern) },
                        onPreview = { controller.previewPattern(pattern) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternCard(
    pattern: ArtPattern,
    selected: Boolean,
    selectionEnabled: Boolean,
    onToggle: () -> Unit,
    onPreview: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = selectionEnabled, onClick = onToggle),
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
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                PatternThumbnail(
                    patternKey = "pattern_preview_${pattern.id}",
                    contentDescription = pattern.localizedTitle(),
                    modifier = Modifier.fillMaxSize()
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(enabled = selectionEnabled, onClick = onToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(3.dp).size(16.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Visibility,
                        contentDescription = localizedStringOr("desktop_preview_pattern", "Preview this pattern"
                        ),
                        tint = Color.White
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = pattern.localizedTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pattern.localizedSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onPreview) {
                    Text(localizedStringOr("desktop_preview_pattern", "Preview this pattern"))
                }
            }
        }
    }
}
