@file:OptIn(ExperimentalMaterial3Api::class)

package com.cptodix.rimalux.desktop.ui.options

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.BuildInfo
import com.cptodix.rimalux.desktop.controller.StudioController
import com.cptodix.rimalux.desktop.controller.StudioUiState
import com.cptodix.rimalux.desktop.ui.components.ChoiceItem
import com.cptodix.rimalux.desktop.ui.components.ChoiceRow
import com.cptodix.rimalux.desktop.ui.localizedDisplayName
import com.cptodix.rimalux.desktop.ui.localizedString
import com.cptodix.rimalux.desktop.ui.localizedStringOr
import com.cptodix.rimalux.desktop.ui.localizedTitle
import com.cptodix.rimalux.desktop.ui.screens.ColorsScreen
import com.cptodix.rimalux.desktop.ui.screens.PatternsScreen
import com.cptodix.rimalux.desktop.ui.screens.SettingsScreen
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.PaletteMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.PitchBlackMode
import kotlin.math.roundToInt

enum class OptionsPage {
    NONE,
    PATTERNS,
    COLORS,
    SETTINGS
}

@Composable
fun OptionsModal(
    state: StudioUiState,
    controller: StudioController,
    page: OptionsPage,
    visible: Boolean,
    onPageChange: (OptionsPage) -> Unit,
    onDismiss: () -> Unit
) {
    val transition = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { transition.targetState = visible }
    if (!visible && transition.isIdle && !transition.currentState) return

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val targetWidth = if (page == OptionsPage.NONE) {
                430.dp
            } else {
                minOf(maxWidth - 32.dp, 1080.dp)
            }
            val panelHeight = minOf(maxHeight - 132.dp, 780.dp).coerceAtLeast(320.dp)
            val panelWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = tween(220),
                label = "options_modal_width"
            )

            AnimatedVisibility(
                visibleState = transition,
                enter = expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = tween(240)
                ) + fadeIn(tween(140)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = tween(200)
                ) + fadeOut(tween(120)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .width(panelWidth)
                        .height(panelHeight)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        OptionsLeftPane(
                            state = state,
                            controller = controller,
                            page = page,
                            onPageChange = onPageChange,
                            modifier = Modifier
                                .width(minOf(430.dp, panelWidth))
                                .fillMaxHeight()
                        )

                        if (page != OptionsPage.NONE) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                when (page) {
                                    OptionsPage.PATTERNS -> PatternsScreen(
                                        state = state,
                                        controller = controller,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    OptionsPage.COLORS -> ColorsScreen(
                                        state = state,
                                        controller = controller,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    OptionsPage.SETTINGS -> SettingsScreen(
                                        state = state,
                                        controller = controller,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    OptionsPage.NONE -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun OptionsLeftPane(
    state: StudioUiState,
    controller: StudioController,
    page: OptionsPage,
    onPageChange: (OptionsPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val patternsSummary = if (state.config.patternsRandomized) {
        localizedString("summary_patterns_random", ArtPattern.entries.size)
    } else {
        val firstTwo = state.config.selectedPatterns.take(2).toList()
        val names = when (firstTwo.size) {
            0 -> ""
            1 -> firstTwo[0].localizedTitle().substringBefore(" ")
            else -> firstTwo[0].localizedTitle().substringBefore(" ") +
                ", " + firstTwo[1].localizedTitle().substringBefore(" ")
        }
        val overflow = state.config.selectedPatterns.size - 2
        if (overflow > 0) {
            localizedString("summary_patterns_selected_more", names, overflow)
        } else {
            names.ifBlank { localizedString("desktop_selected_count", 0) }
        }
    }
    val weightLabel = if (state.config.patternWeightMode == PatternWeightMode.NORMAL) {
        localizedStringOr("weight_mode_normal", "Normal")
    } else {
        localizedStringOr("weight_mode_center", "Center Weighted")
    }
    val paletteSummary = when (state.config.paletteMode) {
        PaletteMode.RANDOMIZED -> localizedStringOr("summary_palette_random", "Randomized")
        PaletteMode.PALETTE_LIST -> {
            val active = state.userPalettes.filter { it.id in state.config.selectedPaletteIds }
            if (active.size == 1) {
                localizedString(
                    "summary_palette_single",
                    active.first().localizedDisplayName()
                )
            } else {
                localizedString("summary_palette_list", active.size.coerceAtLeast(1))
            }
        }
    }
    val fillLabel = if (state.config.colorFillMode == ColorFillMode.SOLID) {
        localizedStringOr("fill_mode_solid", "Solid Colors")
    } else {
        localizedStringOr("fill_mode_gradient", "Gradients")
    }
    val blackPercent = (state.config.pitchBlackRatio * 100f).roundToInt()
    val activePercent = state.selectedRenderedItem?.measuredBlackPercent ?: blackPercent

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedStringOr("label_pitch_black_ratio", "True Black Ratio"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.config.pitchBlackMode == PitchBlackMode.SINGLE) {
                            Text(
                                text = "$blackPercent%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = localizedString(
                                            "pitch_black_active_now",
                                            activePercent
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = localizedString(
                                        "pitch_black_range_value",
                                        (state.config.pitchBlackRangeMin * 100f).roundToInt(),
                                        (state.config.pitchBlackRangeMax * 100f).roundToInt()
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

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
                            selected = state.config.pitchBlackMode == PitchBlackMode.SINGLE,
                            onClick = { controller.setPitchBlackMode(PitchBlackMode.SINGLE) },
                            icon = {},
                            colors = segmentedColors,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 0,
                                count = 2,
                                baseShape = MaterialTheme.shapes.medium
                            )
                        ) {
                            Text(localizedStringOr("black_mode_single", "Single Value"))
                        }
                        SegmentedButton(
                            selected = state.config.pitchBlackMode == PitchBlackMode.RANGE,
                            onClick = { controller.setPitchBlackMode(PitchBlackMode.RANGE) },
                            icon = {},
                            colors = segmentedColors,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 1,
                                count = 2,
                                baseShape = MaterialTheme.shapes.medium
                            )
                        ) {
                            Text(localizedStringOr("black_mode_range", "Range"))
                        }
                    }

                    if (state.config.pitchBlackMode == PitchBlackMode.SINGLE) {
                        Slider(
                            value = state.config.pitchBlackRatio,
                            onValueChange = controller::setPitchBlackRatio,
                            valueRange = 0f..0.95f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        RangeSlider(
                            value = state.config.pitchBlackRangeMin..state.config.pitchBlackRangeMax,
                            onValueChange = { controller.setPitchBlackRange(it.start, it.endInclusive) },
                            valueRange = 0f..0.95f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )

                OptionsRow(
                    title = localizedStringOr("nav_patterns", "Patterns"),
                    summary = patternsSummary,
                    selected = page == OptionsPage.PATTERNS,
                    trailingIcon = if (page == OptionsPage.PATTERNS) {
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                    } else {
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight
                    },
                    onClick = {
                        onPageChange(
                            if (page == OptionsPage.PATTERNS) OptionsPage.NONE else OptionsPage.PATTERNS
                        )
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
                ChoiceRow(
                    title = localizedStringOr("label_pattern_layout", "Pattern Layout"),
                    summary = weightLabel,
                    items = listOf(
                        ChoiceItem(
                            label = localizedStringOr("weight_mode_normal", "Normal"),
                            selected = state.config.patternWeightMode == PatternWeightMode.NORMAL
                        ),
                        ChoiceItem(
                            label = localizedStringOr("weight_mode_center", "Center Weighted"),
                            selected = state.config.patternWeightMode == PatternWeightMode.CENTER_WEIGHTED
                        )
                    ),
                    onSelect = { index ->
                        controller.setPatternWeightMode(
                            if (index == 0) PatternWeightMode.NORMAL else PatternWeightMode.CENTER_WEIGHTED
                        )
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
                OptionsRow(
                    title = localizedStringOr("nav_palettes", "Colors"),
                    summary = paletteSummary,
                    selected = page == OptionsPage.COLORS,
                    trailingIcon = if (page == OptionsPage.COLORS) {
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                    } else {
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight
                    },
                    onClick = {
                        onPageChange(
                            if (page == OptionsPage.COLORS) OptionsPage.NONE else OptionsPage.COLORS
                        )
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                )
                ChoiceRow(
                    title = localizedStringOr("label_color_shading", "Color Fill Style"),
                    summary = fillLabel,
                    items = listOf(
                        ChoiceItem(
                            label = localizedStringOr("fill_mode_solid", "Solid Colors"),
                            selected = state.config.colorFillMode == ColorFillMode.SOLID
                        ),
                        ChoiceItem(
                            label = localizedStringOr("fill_mode_gradient", "Gradients"),
                            selected = state.config.colorFillMode == ColorFillMode.GRADIENT
                        )
                    ),
                    onSelect = { index ->
                        controller.setColorFillMode(
                            if (index == 0) ColorFillMode.SOLID else ColorFillMode.GRADIENT
                        )
                    }
                )
            }
        }

        AutoChangeSection(state = state, controller = controller)

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
            )
        ) {
            OptionsRow(
                title = localizedStringOr("desktop_desktop_settings", "Advanced Settings"),
                summary = localizedStringOr(
                    "desktop_settings_subtitle",
                    "Advanced desktop behavior, display output, and diagnostics"
                ),
                selected = page == OptionsPage.SETTINGS,
                trailingIcon = if (page == OptionsPage.SETTINGS) {
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                } else {
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight
                },
                onClick = {
                    onPageChange(
                        if (page == OptionsPage.SETTINGS) OptionsPage.NONE else OptionsPage.SETTINGS
                    )
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = localizedString(
                    "about_sheet_footer",
                    BuildInfo.VERSION,
                    1
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun OptionsRow(
    title: String,
    summary: String,
    selected: Boolean,
    trailingIcon: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        },
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
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
