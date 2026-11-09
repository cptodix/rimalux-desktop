package com.cptodix.rimalux.desktop.platform

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant

/**
 * Chooses the tray glyph that reads on the current desktop theme.
 *
 * Tray hosts do not recolour bitmap icons, so we ship a light and a dark glyph and pick one.
 * The XDG desktop portal is the portable source; KDE config and the Windows registry are
 * fallbacks. When nothing can be detected we assume a dark panel, which is the common case.
 */
internal object TrayTheme {

    /** True when the light (white) glyph should be used, i.e. the desktop is dark. */
    val lightGlyph: Boolean by lazy { detectDark() ?: true }

    private fun detectDark(): Boolean? = runCatching {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "rimalux-tray-theme").apply { isDaemon = true }
        }
        try {
            executor.submit<Boolean?> { portalDark() ?: windowsDark() ?: kdeDark() }
                .get(1500, TimeUnit.MILLISECONDS)
        } finally {
            executor.shutdownNow()
        }
    }.getOrNull()

    private fun portalDark(): Boolean? = runCatching {
        DBusConnectionBuilder.forSessionBus().build().use { bus ->
            val settings = bus.getRemoteObject(
                PORTAL_SERVICE,
                PORTAL_PATH,
                PortalSettings::class.java
            )
            when ((settings.read("org.freedesktop.appearance", "color-scheme").value as? Number)?.toInt()) {
                1 -> true
                2 -> false
                else -> null
            }
        }
    }.getOrNull()

    private fun windowsDark(): Boolean? {
        if (!System.getProperty("os.name").orEmpty().lowercase().contains("win")) return null
        return runCatching {
            val process = ProcessBuilder(
                "reg",
                "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v",
                "AppsUseLightTheme"
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val value = REG_DWORD.find(output)?.groupValues?.get(1)?.toInt(16) ?: return null
            value == 0
        }.getOrNull()
    }

    private fun kdeDark(): Boolean? = runCatching {
        val config = Path.of(System.getProperty("user.home"), ".config", "kdeglobals")
        if (!Files.exists(config)) return null
        var inWindowColors = false
        for (line in Files.readAllLines(config)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[")) {
                inWindowColors = trimmed == "[Colors:Window]"
            } else if (inWindowColors && trimmed.startsWith("BackgroundNormal=")) {
                val parts = trimmed.substringAfter('=').split(',').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 3) {
                    return isDarkRgb(parts[0], parts[1], parts[2])
                }
            }
        }
        null
    }.getOrNull()

    internal fun isDarkRgb(red: Int, green: Int, blue: Int): Boolean {
        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
        return luminance < 128.0
    }

    @DBusInterfaceName("org.freedesktop.portal.Settings")
    private interface PortalSettings : DBusInterface {
        @DBusMemberName("Read")
        fun read(namespace: String, key: String): Variant<*>
    }

    private const val PORTAL_SERVICE = "org.freedesktop.portal.Desktop"
    private const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
    private val REG_DWORD = Regex("AppsUseLightTheme\\s+REG_DWORD\\s+0x([0-9a-fA-F]+)")
}
