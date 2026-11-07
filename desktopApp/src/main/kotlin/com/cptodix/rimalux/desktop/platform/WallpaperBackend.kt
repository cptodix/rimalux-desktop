package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.BackendInfo
import com.cptodix.rimalux.desktop.model.DesktopSettings
import com.cptodix.rimalux.desktop.model.FitMode
import com.cptodix.rimalux.desktop.model.WallpaperFile
import java.nio.file.Path

data class ApplyResult(
    val success: Boolean,
    val appliedCount: Int,
    val message: String,
    val errors: List<String> = emptyList()
)

interface WallpaperBackend {
    val info: BackendInfo
    suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode = FitMode.FILL): ApplyResult
}

class WallpaperBackendFactory(
    private val runner: ProcessRunner = ProcessRunner(),
    private val paths: PlatformPaths = PlatformPaths()
) {
    fun detect(settings: DesktopSettings): WallpaperBackend {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.contains("win")) {
            if (settings.ui.backendOverride == "custom" && settings.ui.customWallpaperCommand.isNotBlank()) {
                return CustomCommandBackend(runner, settings.ui.customWallpaperCommand)
            }
            return WindowsWallpaperBackend()
        }

        val override = settings.ui.backendOverride.lowercase()
        if (override == "custom" && settings.ui.customWallpaperCommand.isNotBlank()) {
            return CustomCommandBackend(runner, settings.ui.customWallpaperCommand)
        }

        val desktop = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().lowercase()
        val session = System.getenv("DESKTOP_SESSION").orEmpty().lowercase()
        val combined = "$desktop $session"

        val kde = KdeWallpaperBackend(runner)
        if ((override == "kde" || (override == "auto" && combined.contains("kde"))) && kde.isAvailable()) {
            return kde
        }

        val gnome = GnomeWallpaperBackend(runner)
        if ((override == "gnome" || (override == "auto" && (combined.contains("gnome") || combined.contains("unity") || combined.contains("budgie")))) && gnome.isAvailable()) {
            return gnome
        }

        val xfce = XfceWallpaperBackend(runner)
        if ((override == "xfce" || (override == "auto" && combined.contains("xfce"))) && xfce.isAvailable()) {
            return xfce
        }

        if (settings.ui.customWallpaperCommand.isNotBlank()) {
            return CustomCommandBackend(runner, settings.ui.customWallpaperCommand)
        }

        return UnsupportedWallpaperBackend(
            detail = "No supported wallpaper backend detected. Save a PNG or configure a custom command."
        )
    }
}

class KdeWallpaperBackend(
    private val runner: ProcessRunner
) : WallpaperBackend {
    private val qdbus = runner.findExecutable("qdbus6") ?: runner.findExecutable("qdbus")
    private val applyImage = runner.findExecutable("plasma-apply-wallpaperimage")

    fun isAvailable(): Boolean = qdbus != null || applyImage != null

    override val info: BackendInfo = BackendInfo(
        id = "kde",
        displayName = "KDE Plasma",
        supportsPerMonitor = true,
        available = isAvailable(),
        detail = if (qdbus != null) "Plasma scripting via ${qdbus.substringAfterLast('/')}"
        else "Global apply via plasma-apply-wallpaperimage"
    )

    override suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode): ApplyResult {
        if (files.isEmpty()) {
            return ApplyResult(false, 0, "No wallpaper files to apply")
        }
        val errors = mutableListOf<String>()

        if (qdbus != null) {
            val script = buildPlasmaScript(files, fitMode)
            val result = runner.run(
                listOf(
                    qdbus,
                    "org.kde.plasmashell",
                    "/PlasmaShell",
                    "org.kde.PlasmaShell.evaluateScript",
                    script
                )
            )
            if (result.success) {
                return ApplyResult(
                    success = true,
                    appliedCount = files.size,
                    message = "Applied to ${files.size} display(s) via KDE Plasma"
                )
            }
            errors += "Plasma script failed: ${result.stderr.ifBlank { result.stdout }}".trim()
        }

        // Global fallback is safe for a single display; it applies one image everywhere.
        if (applyImage != null && files.size == 1) {
            val result = runner.run(
                listOf(applyImage, "-f", "preserveAspectCrop", files.first().path.toAbsolutePath().toString())
            )
            if (result.success) {
                return ApplyResult(
                    success = true,
                    appliedCount = 1,
                    message = "Applied to Desktop via plasma-apply-wallpaperimage"
                )
            }
            errors += "plasma-apply-wallpaperimage failed: ${result.stderr.ifBlank { result.stdout }}".trim()
        }

        return ApplyResult(
            success = false,
            appliedCount = 0,
            message = "Could not apply wallpaper",
            errors = errors
        )
    }

    internal fun buildPlasmaScript(files: List<WallpaperFile>, fitMode: FitMode = FitMode.FILL): String {
        val mappings = files.joinToString("\n") { file ->
            "images[${file.item.screenIndex}] = \"${escapeJs(file.path.toUri().toString())}\";"
        }
        val kdeFillMode = when (fitMode) {
            FitMode.FILL -> 2
            FitMode.FIT -> 1
            FitMode.CENTER -> 4
            FitMode.TILE -> 3
        }
        return """
            var images = {};
            $mappings
            var ds = desktops();
            for (var i = 0; i < ds.length; i++) {
                var d = ds[i];
                var idx = (typeof d.screen === "number") ? d.screen : 0;
                if (images[idx] !== undefined) {
                    try {
                        d.wallpaperPlugin = "org.kde.image";
                        d.currentConfigGroup = ["Wallpaper", "org.kde.image", "General"];
                        d.writeConfig("Image", images[idx]);
                        d.writeConfig("FillMode", $kdeFillMode);
                    } catch (e) {}
                }
            }
        """.trimIndent()
    }

    private fun escapeJs(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
    }
}

class GnomeWallpaperBackend(
    private val runner: ProcessRunner
) : WallpaperBackend {
    private val gsettings = runner.findExecutable("gsettings")

    fun isAvailable(): Boolean = gsettings != null

    override val info: BackendInfo = BackendInfo(
        id = "gnome",
        displayName = "GNOME",
        supportsPerMonitor = false,
        available = isAvailable(),
        detail = "GNOME applies one wallpaper to all displays"
    )

    override suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode): ApplyResult {
        val file = files.firstOrNull()
            ?: return ApplyResult(false, 0, "No wallpaper files to apply")
        if (gsettings == null) {
            return ApplyResult(false, 0, "gsettings is not available")
        }
        val uri = file.path.toUri().toString()
        val errors = mutableListOf<String>()
        val pictureOptions = when (fitMode) {
            FitMode.FILL -> "zoom"
            FitMode.FIT -> "scaled"
            FitMode.CENTER -> "centered"
            FitMode.TILE -> "wallpaper"
        }

        fun run(vararg args: String): Boolean {
            val result = runner.run(listOf(gsettings) + args)
            if (!result.success) {
                errors += "${args.joinToString(" ")}: ${result.stderr.ifBlank { result.stdout }}".trim()
            }
            return result.success
        }

        val mainOk = run("set", "org.gnome.desktop.background", "picture-uri", uri)
        run("set", "org.gnome.desktop.background", "picture-uri-dark", uri)
        run("set", "org.gnome.desktop.background", "picture-options", pictureOptions)

        return if (mainOk) {
            ApplyResult(
                success = true,
                appliedCount = 1,
                message = "Applied to Desktop via GNOME (one wallpaper for all displays)"
            )
        } else {
            ApplyResult(false, 0, "Could not apply GNOME wallpaper", errors)
        }
    }
}

class XfceWallpaperBackend(
    private val runner: ProcessRunner
) : WallpaperBackend {
    private val xfconf = runner.findExecutable("xfconf-query")

    fun isAvailable(): Boolean = xfconf != null

    override val info: BackendInfo = BackendInfo(
        id = "xfce",
        displayName = "XFCE",
        supportsPerMonitor = true,
        available = isAvailable(),
        detail = "Per-monitor via xfconf-query"
    )

    override suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode): ApplyResult {
        if (files.isEmpty()) return ApplyResult(false, 0, "No wallpaper files to apply")
        val executable = xfconf ?: return ApplyResult(false, 0, "xfconf-query is not available")
        val imageStyle = when (fitMode) {
            FitMode.FILL -> "5"
            FitMode.FIT -> "4"
            FitMode.CENTER -> "1"
            FitMode.TILE -> "2"
        }
        val list = runner.run(listOf(executable, "-c", "xfce4-desktop", "-l"))
        if (!list.success) {
            return ApplyResult(false, 0, "Could not list XFCE desktop properties")
        }
        val properties = list.stdout.lineSequence()
            .map { it.trim() }
            .filter { it.endsWith("/last-image") }
            .toList()
        if (properties.isEmpty()) {
            return ApplyResult(false, 0, "No XFCE last-image properties found")
        }

        var applied = 0
        val errors = mutableListOf<String>()
        for (file in files) {
            val name = file.item.monitorName.lowercase()
            val matching = properties.filter { it.lowercase().contains(name) }
            val targets = matching.ifEmpty { if (files.size == 1) properties else emptyList() }
            for (property in targets) {
                val result = runner.run(
                    listOf(
                        executable,
                        "-c", "xfce4-desktop",
                        "-p", property,
                        "-s", file.path.toAbsolutePath().toString()
                    )
                )
                if (result.success) {
                    applied++
                    val styleProperty = property.removeSuffix("last-image") + "image-style"
                    runner.run(
                        listOf(executable, "-c", "xfce4-desktop", "-p", styleProperty, "-s", imageStyle)
                    )
                } else {
                    errors += "$property: ${result.stderr.ifBlank { result.stdout }}".trim()
                }
            }
        }
        return ApplyResult(
            success = applied > 0,
            appliedCount = applied,
            message = if (applied > 0) "Applied to XFCE desktop" else "Could not apply XFCE wallpaper",
            errors = errors
        )
    }
}

class CustomCommandBackend(
    private val runner: ProcessRunner,
    private val commandTemplate: String
) : WallpaperBackend {
    override val info: BackendInfo = BackendInfo(
        id = "custom",
        displayName = "Custom command",
        supportsPerMonitor = true,
        available = commandTemplate.isNotBlank(),
        detail = commandTemplate
    )

    override suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode): ApplyResult {
        if (files.isEmpty()) return ApplyResult(false, 0, "No wallpaper files to apply")
        if (commandTemplate.isBlank()) return ApplyResult(false, 0, "Custom command is empty")

        val tokens = tokenize(commandTemplate)
        if (tokens.isEmpty()) return ApplyResult(false, 0, "Custom command is empty")
        val errors = mutableListOf<String>()
        var applied = 0

        for (file in files) {
            val replaced = tokens.map { token ->
                token
                    .replace("{file}", file.path.toAbsolutePath().toString())
                    .replace("{monitor}", file.item.monitorName)
            }
            val result = runner.run(replaced)
            if (result.success) {
                applied++
            } else {
                errors += "${replaced.joinToString(" ")}: ${result.stderr.ifBlank { result.stdout }}".trim()
            }
        }

        return ApplyResult(
            success = applied > 0,
            appliedCount = applied,
            message = if (applied > 0) "Applied via custom command" else "Custom command failed",
            errors = errors
        )
    }

    internal fun tokenize(command: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false
        for (ch in command) {
            when {
                escaped -> {
                    current.append(ch)
                    escaped = false
                }
                ch == '\\' && quote != '\'' -> escaped = true
                quote != null && ch == quote -> quote = null
                quote == null && (ch == '"' || ch == '\'') -> quote = ch
                quote == null && ch.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens += current.toString()
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens += current.toString()
        return tokens
    }
}

class UnsupportedWallpaperBackend(
    detail: String
) : WallpaperBackend {
    override val info: BackendInfo = BackendInfo.unsupported(detail)

    override suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode): ApplyResult {
        return ApplyResult(
            success = false,
            appliedCount = 0,
            message = info.detail.ifBlank { "No wallpaper backend available" }
        )
    }
}

fun openInFileManager(path: Path) {
    runCatching {
        if (java.awt.Desktop.isDesktopSupported()) {
            val desktop = java.awt.Desktop.getDesktop()
            if (path.toFile().isDirectory) {
                desktop.open(path.toFile())
            } else {
                desktop.open(path.parent?.toFile() ?: path.toFile())
            }
        }
    }.onFailure { error ->
        AppLog.warning("Could not open path $path", error)
    }
}
