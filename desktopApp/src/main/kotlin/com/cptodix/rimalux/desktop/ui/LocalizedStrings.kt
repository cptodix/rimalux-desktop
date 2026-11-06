package com.cptodix.rimalux.desktop.ui


import androidx.compose.runtime.Composable
import com.cptodix.rimalux.desktop.controller.UiMessage
import com.cptodix.rimalux.desktop.resources.Res
import com.cptodix.rimalux.desktop.resources.allStringResources
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.PatternCategory
import org.jetbrains.compose.resources.stringResource

@Composable
fun localizedString(key: String, vararg args: Any): String {
    val resource = Res.allStringResources[key]
    return when {
        resource == null -> key
        args.isEmpty() -> stringResource(resource)
        else -> stringResource(resource, *args)
    }
}

@Composable
fun localizedStringOr(key: String, fallback: String, vararg args: Any): String {
    val resource = Res.allStringResources[key]
    return when {
        resource == null -> fallback
        args.isEmpty() -> stringResource(resource)
        else -> stringResource(resource, *args)
    }
}

@Composable
fun UiMessage.localized(): String = localizedStringOr(key, key, *args.toTypedArray())

@Composable
fun ArtPattern.localizedTitle(): String =
    localizedStringOr("pattern_${id}", defaultTitle)

@Composable
fun ArtPattern.localizedSubtitle(): String =
    localizedStringOr("pattern_${id}_sub", defaultSubtitle)

@Composable
fun PatternCategory.localizedLabel(): String = when (this) {
    PatternCategory.FLOW_WAVES -> localizedStringOr("cat_flow", defaultLabel)
    PatternCategory.SACRED_SYMMETRY -> localizedStringOr("cat_symmetry", defaultLabel)
    PatternCategory.ARCHITECTURAL -> localizedStringOr("cat_architectural", defaultLabel)
    PatternCategory.TESSELLATION -> localizedStringOr("cat_tessellation", defaultLabel)
}

@Composable
fun ColorPalette.localizedDisplayName(): String =
    localizedStringOr("palette_name_$id", name)

fun ArtPattern.previewKey(): String = "pattern_preview_${id}"
