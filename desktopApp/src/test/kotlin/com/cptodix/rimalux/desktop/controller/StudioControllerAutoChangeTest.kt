package com.cptodix.rimalux.desktop.controller

import com.cptodix.rimalux.desktop.model.BackendInfo
import com.cptodix.rimalux.desktop.model.FitMode
import com.cptodix.rimalux.desktop.model.WallpaperFile
import com.cptodix.rimalux.desktop.platform.ApplyResult
import com.cptodix.rimalux.desktop.platform.PlatformPaths
import com.cptodix.rimalux.desktop.platform.WallpaperBackend
import com.cptodix.rimalux.desktop.scheduler.AutoChangePolicy
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioControllerAutoChangeTest {

    @Test
    fun autoRotationGeneratesAndAppliesANewSet() = runBlocking {
        val root = Files.createTempDirectory("rimalux-auto-test")
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
        val backend = FakeBackend()
        var now = 1_000_000L
        val controller = StudioController(
            paths = paths,
            backendOverride = backend,
            clock = { now }
        )
        try {
            withTimeout(60_000L) {
                controller.uiState.first { it.current != null }
            }
            controller.setAutoChangeEnabled(true)
            val firstState = controller.uiState.first { it.autoChange.enabled }
            now = firstState.autoChange.nextRunEpochMillis + 1L

            val result = controller.performAutoRotation(force = false)
            assertTrue(result?.success == true)
            assertTrue(backend.applied)
            val after = controller.uiState.first { it.autoChange.lastRunEpochMillis == now }
            assertTrue(after.autoChange.lastRunEpochMillis == now)
            assertTrue(
                after.autoChange.nextRunEpochMillis ==
                    now + AutoChangePolicy.intervalMillis(after.autoChange.intervalMinutes)
            )
        } finally {
            controller.shutdown()
        }
    }

    private class FakeBackend : WallpaperBackend {
        var applied = false
        override val info = BackendInfo(
            id = "fake",
            displayName = "Fake",
            supportsPerMonitor = true,
            available = true
        )

        override suspend fun apply(
            files: List<WallpaperFile>,
            fitMode: FitMode
        ): ApplyResult {
            applied = true
            return ApplyResult(true, files.size, "Fake apply")
        }
    }
}
