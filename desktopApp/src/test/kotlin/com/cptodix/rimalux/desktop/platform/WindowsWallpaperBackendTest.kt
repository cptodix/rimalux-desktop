package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.FitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowsWallpaperBackendTest {

    @Test
    fun fitModesMapToDesktopWallpaperPositions() {
        assertEquals(4, WindowsWallpaperBackend.windowsPosition(FitMode.FILL))
        assertEquals(3, WindowsWallpaperBackend.windowsPosition(FitMode.FIT))
        assertEquals(0, WindowsWallpaperBackend.windowsPosition(FitMode.CENTER))
        assertEquals(1, WindowsWallpaperBackend.windowsPosition(FitMode.TILE))
    }

    @Test
    fun monitorIndexIsClamped() {
        assertEquals(0, WindowsWallpaperBackend.monitorIndexFor(0, 2, 2))
        assertEquals(1, WindowsWallpaperBackend.monitorIndexFor(1, 2, 2))
        assertEquals(1, WindowsWallpaperBackend.monitorIndexFor(9, 2, 2))
        assertEquals(0, WindowsWallpaperBackend.monitorIndexFor(-1, 2, 2))
    }

    @Test
    fun backendReportsWindowsOnlyOnWindows() {
        val backend = WindowsWallpaperBackend()
        val runningOnWindows = System.getProperty("os.name").orEmpty()
            .lowercase()
            .contains("win")
        assertEquals(runningOnWindows, backend.info.available)
        assertTrue(backend.info.supportsPerMonitor)
        assertFalse(backend.info.displayName.isBlank())
    }
}
