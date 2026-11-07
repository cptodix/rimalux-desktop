package com.cptodix.rimalux.desktop.controller

import com.cptodix.rimalux.desktop.platform.PlatformPaths
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioControllerPrefetchTest {

    @Test
    fun controller_fillsNextWallpaperBufferWithoutDesktopInteraction() = runBlocking {
        val root = Files.createTempDirectory("rimalux-prefetch-test")
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
        val controller = StudioController(paths = paths)
        try {
            val state = withTimeout(45_000L) {
                controller.uiState.first { it.current != null && it.aheadReadyCount > 0 }
            }
            assertTrue(state.current != null)
            assertTrue(state.aheadReadyCount in 1..3)
        } finally {
            controller.shutdown()
        }
    }
}
