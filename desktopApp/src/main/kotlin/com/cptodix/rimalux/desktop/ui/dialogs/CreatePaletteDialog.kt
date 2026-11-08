package com.cptodix.rimalux.desktop.ui.dialogs

import com.cptodix.rimalux.desktop.ui.localizedStringOr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cptodix.rimalux.desktop.ui.localizedString
import java.awt.Color as AwtColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePaletteDialog(
    onSave: (String, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = remember { mutableStateListOf<Long>(0xFF00F5D4, 0xFF7B2CBF, 0xFFF15BB5) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val minError = localizedStringOr(
        "desktop_palette_min_error",
        "A palette needs at least 2 colors."
    )
    val blackError = localizedStringOr(
        "desktop_color_black_error",
        "Pure #000000 is reserved for OLED void. Pick a non-black color."
    )

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
                    text = localizedStringOr("desktop_create_palette", "Create Palette"),
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(localizedStringOr("desktop_palette_name", "Palette Name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = localizedStringOr("desktop_palette_colors", "Colors"),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEachIndexed { index, argb ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .clickable { editingIndex = index }
                        )
                    }
                    if (colors.size < 6) {
                        OutlinedButton(
                            onClick = {
                                colors += 0xFF82DCEB
                                editingIndex = colors.lastIndex
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(42.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Color")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            editingIndex?.let { index ->
                                if (colors.size > 2) colors.removeAt(index)
                            }
                        },
                        enabled = colors.size > 2 && editingIndex != null,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Text(
                            text = localizedStringOr("desktop_remove_color", "Remove Color"),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(localizedStringOr("desktop_cancel", "Cancel"))
                    }
                    Button(
                        onClick = {
                            when {
                                colors.size < 2 -> error = minError
                                colors.any { (it and 0xFFFFFFL) == 0L } -> error = blackError
                                else -> onSave(name.ifBlank { "Custom" }, colors.toList())
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_save_palette", "Add to List"))
                    }
                }
            }
        }
    }

    editingIndex?.let { index ->
        if (index in colors.indices) {
            ColorPickerDialog(
                initialArgb = colors[index],
                onSelect = { argb ->
                    colors[index] = argb
                    editingIndex = null
                    error = null
                },
                onDismiss = { editingIndex = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    initialArgb: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHsv = remember(initialArgb) { rgbToHsv(initialArgb) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var hex by remember(initialArgb) { mutableStateOf(argbToHex(initialArgb)) }
    var error by remember { mutableStateOf<String?>(null) }
    val blackError = localizedStringOr(
        "desktop_color_black_error",
        "Pure #000000 is reserved for OLED void. Pick a non-black color."
    )

    fun updateFromHsv() {
        hex = argbToHex(hsvToArgb(hue, saturation, brightness))
        error = null
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = localizedStringOr("desktop_color_picker", "Color Picker"),
                    style = MaterialTheme.typography.titleLarge
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(hsvToArgb(hue, saturation, brightness)))
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { raw ->
                        hex = raw.take(9)
                        val parsed = parseHex(hex)
                        if (parsed != null) {
                            val hsv = rgbToHsv(parsed)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                            error = null
                        }
                    },
                    label = { Text(localizedStringOr("desktop_hex", "Hex")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SliderRow(
                    label = localizedStringOr("desktop_hue", "Hue"),
                    valueText = "${(hue * 360f).roundToInt()}°",
                    value = hue,
                    onValueChange = {
                        hue = it
                        updateFromHsv()
                    }
                )
                SliderRow(
                    label = localizedStringOr("desktop_saturation", "Saturation"),
                    valueText = "${(saturation * 100f).roundToInt()}%",
                    value = saturation,
                    onValueChange = {
                        saturation = it
                        updateFromHsv()
                    }
                )
                SliderRow(
                    label = localizedStringOr("desktop_brightness", "Brightness"),
                    valueText = "${(brightness * 100f).roundToInt()}%",
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        updateFromHsv()
                    }
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(localizedStringOr("desktop_cancel", "Cancel"))
                    }
                    Button(
                        onClick = {
                            val argb = hsvToArgb(hue, saturation, brightness)
                            if ((argb and 0xFFFFFFL) == 0L) {
                                error = blackError
                            } else {
                                onSelect(argb)
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(localizedStringOr("desktop_apply_color", "Use Color"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

private fun rgbToHsv(argb: Long): FloatArray {
    val r = ((argb shr 16) and 0xFF).toInt()
    val g = ((argb shr 8) and 0xFF).toInt()
    val b = (argb and 0xFF).toInt()
    return AwtColor.RGBtoHSB(r, g, b, null)
}

private fun hsvToArgb(hue: Float, saturation: Float, brightness: Float): Long {
    val rgb = AwtColor.HSBtoRGB(hue.coerceIn(0f, 1f), saturation.coerceIn(0f, 1f), brightness.coerceIn(0f, 1f))
    return 0xFF000000L or (rgb.toLong() and 0xFFFFFFL)
}

private fun argbToHex(argb: Long): String =
    "#%06X".format(argb and 0xFFFFFFL)

private fun parseHex(raw: String): Long? {
    val cleaned = raw.trim().removePrefix("#")
    if (cleaned.length != 6 || cleaned.any { it !in "0123456789abcdefABCDEF" }) return null
    return 0xFF000000L or cleaned.toLong(16)
}
