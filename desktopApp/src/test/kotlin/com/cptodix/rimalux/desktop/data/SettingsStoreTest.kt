package com.cptodix.rimalux.desktop.data

import com.cptodix.rimalux.desktop.model.GenerationConfig
import com.cptodix.rimalux.desktop.model.UiSettings
import com.cptodix.rimalux.desktop.platform.PlatformPaths
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStoreTest {

    @Test
    fun settingsRoundTrip_persistsGenerationDisplayAndLanguage() = runBlocking {
        val root = Files.createTempDirectory("rimalux-settings-test")
        val paths = PlatformPaths(
            osName = "Linux",
            environment = mapOf(
                "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                "XDG_STATE_HOME" to root.resolve("state").toString()
            ),
            userHome = root
        )
        val store = SettingsStore(paths)
        store.update { settings ->
            settings.copy(
                generation = settings.generation.copy(
                    pitchBlackRatio = 0.72f,
                    patternsRandomized = false
                ),
                ui = settings.ui.copy(languageTag = "id-ID", lastScreen = com.cptodix.rimalux.desktop.model.DesktopScreen.COLORS)
            )
        }

        val reloaded = SettingsStore(paths)
        val state = reloaded.snapshot()
        assertEquals(0.72f, state.generation.pitchBlackRatio, 0.0001f)
        assertEquals(false, state.generation.patternsRandomized)
        assertEquals("id-ID", state.ui.languageTag)
        assertEquals(com.cptodix.rimalux.desktop.model.DesktopScreen.COLORS, state.ui.lastScreen)
        assertTrue(Files.exists(paths.configFile))
    }

    @Test
    fun corruptSettingsFile_isBackedUpAndDefaultsAreUsed() {
        val root = Files.createTempDirectory("rimalux-corrupt-test")
        val paths = PlatformPaths(
            osName = "Linux",
            environment = mapOf(
                "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                "XDG_STATE_HOME" to root.resolve("state").toString()
            ),
            userHome = root
        )
        paths.ensureDirectories()
        Files.writeString(paths.configFile, "{ this is not valid json")

        val store = SettingsStore(paths)
        assertEquals(GenerationConfig.DEFAULT_PITCH_BLACK_RATIO, store.snapshot().generation.pitchBlackRatio, 0.0001f)
        assertTrue(
            Files.list(paths.configDir).use { stream ->
                stream.anyMatch { it.fileName.toString().startsWith("settings.json.bak") }
            }
        )
    }
}
