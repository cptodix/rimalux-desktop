package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.WallpaperFile
import com.cptodix.rimalux.desktop.model.WallpaperItem
import com.cptodix.rimalux.desktop.model.WallpaperSet
import com.cptodix.rimalux.desktop.render.SkiaWallpaperRenderer
import com.cptodix.rimalux.oled.wallpaper.engine.WallpaperGeneratorEngine
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.name

/**
 * Renders wallpaper sets to cache files and exports lossless PNGs to the Pictures folder.
 */
class WallpaperFileStore(
    private val paths: PlatformPaths,
    private val renderer: SkiaWallpaperRenderer = SkiaWallpaperRenderer
) {
    fun renderSet(set: WallpaperSet): List<WallpaperFile> {
        paths.ensureDirectories()
        val setDir = paths.cacheWallpapersDir.resolve(sanitize(set.id))
        Files.createDirectories(setDir)
        val files = ArrayList<WallpaperFile>(set.items.size)
        for (item in set.items) {
            val synthesized = synthesize(item)
            val rendered = renderer.renderAndMeasure(
                synthesized = synthesized,
                widthPx = item.widthPx,
                heightPx = item.heightPx
            )
            val path = setDir.resolve("${sanitize(item.safeCacheKey)}.png")
            try {
                renderer.writePng(rendered.image, path)
            } finally {
                runCatching { rendered.image.close() }
            }
            files += WallpaperFile(
                item = item.copy(measuredBlackPercent = rendered.measuredBlackPercent),
                path = path,
                measuredBlackPercent = rendered.measuredBlackPercent
            )
        }
        return files
    }

    fun saveToPictures(files: List<WallpaperFile>, timestampMillis: Long = System.currentTimeMillis()): List<Path> {
        paths.ensureDirectories()
        val targetDir = paths.picturesDir
        Files.createDirectories(targetDir)
        val saved = ArrayList<Path>(files.size)
        for (file in files) {
            val seedHex = (file.item.seed and 0xFFFFFFFFFFFFL)
                .toString(16)
                .uppercase(Locale.ROOT)
            val fileName = buildString {
                append("Rimalux_")
                append(file.item.pattern.id)
                append('_')
                append(seedHex)
                append('_')
                append(sanitize(file.item.monitorName))
                append('_')
                append(timestampMillis)
                append(".png")
            }
            val target = targetDir.resolve(fileName)
            Files.copy(file.path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            saved.add(target)
        }
        return saved
    }

    fun cleanup(keepSetIds: Set<String>) {
        val dir = paths.cacheWallpapersDir
        if (!Files.isDirectory(dir)) return
        runCatching {
            Files.list(dir).use { stream ->
                stream.filter { Files.isDirectory(it) }
                    .forEach { child ->
                        if (child.name !in keepSetIds) {
                            deleteRecursively(child)
                        }
                    }
            }
        }.onFailure { error ->
            AppLog.warning("Could not clean wallpaper cache", error)
        }
    }

    private fun synthesize(item: WallpaperItem) = WallpaperGeneratorEngine.synthesizeFromParameters(
        seed = item.seed,
        pattern = item.pattern,
        paletteColors = item.paletteColors,
        colorFillMode = item.colorFillMode,
        pitchBlackRatio = item.pitchBlackRatio,
        intricacy = item.intricacy,
        aspectRatio = item.heightPx.toFloat() / item.widthPx.toFloat(),
        patternWeightMode = item.patternWeightMode
    )

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)

    private fun deleteRecursively(path: Path) {
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
