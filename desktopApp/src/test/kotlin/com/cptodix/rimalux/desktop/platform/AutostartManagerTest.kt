package com.cptodix.rimalux.desktop.platform

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutostartManagerTest {

    @Test
    fun linuxAutostartEntryCanBeEnabledAndRemoved() {
        val root = Files.createTempDirectory("rimalux-autostart-test")
        val paths = PlatformPaths(
            osName = "Linux",
            environment = mapOf(
                "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                "XDG_STATE_HOME" to root.resolve("state").toString()
            ),
            userHome = root
        )
        val manager = AutostartManager(
            paths = paths,
            executablePath = "/opt/rimalux/bin/Rimalux",
            osName = "Linux"
        )
        assertTrue(manager.isSupported())
        assertFalse(manager.isEnabled())

        manager.setEnabled(true).getOrThrow()
        assertTrue(manager.isEnabled())
        val content = Files.readString(paths.autostartFile)
        assertTrue(content.contains("[Desktop Entry]"))
        assertTrue(content.contains("Exec=\"/opt/rimalux/bin/Rimalux\" --background"))
        assertTrue(content.contains("X-GNOME-Autostart-enabled=true"))

        manager.setEnabled(false).getOrThrow()
        assertFalse(manager.isEnabled())
        assertFalse(Files.exists(paths.autostartFile))
    }
}
