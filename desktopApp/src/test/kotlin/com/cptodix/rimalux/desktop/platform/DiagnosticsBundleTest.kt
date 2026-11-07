package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.data.SettingsStore
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsBundleTest {

    @Test
    fun bundleContainsReportsAndRedactsCustomCommands() {
        val root = Files.createTempDirectory("rimalux-diagnostics-test")
        val paths = PlatformPaths(
            osName = "Linux",
            environment = mapOf(
                "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                "XDG_STATE_HOME" to root.resolve("state").toString(),
                "XDG_PICTURES_DIR" to root.resolve("pictures").toString()
            ),
            userHome = root
        )
        val store = SettingsStore(paths)
        runBlocking {
            store.update {
                it.copy(ui = it.ui.copy(customWallpaperCommand = "secret-token --flag"))
            }
        }
        val target = root.resolve("bundle.zip")
        DiagnosticsBundle.write(paths, store, target)

        ZipFile(target.toFile()).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertTrue("system.txt" in names)
            assertTrue("settings.redacted.json" in names)
            assertTrue("monitors.txt" in names)
            assertTrue("backend.txt" in names)
            val settings = zip.getInputStream(zip.getEntry("settings.redacted.json"))
                .bufferedReader()
                .readText()
            assertTrue(settings.contains("[redacted]"))
            assertFalse(settings.contains("secret-token"))
        }
    }
}
