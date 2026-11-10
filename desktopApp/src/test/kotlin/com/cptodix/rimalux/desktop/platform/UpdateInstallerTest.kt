package com.cptodix.rimalux.desktop.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateInstallerTest {

    private val assets = listOf(
        UpdateAsset("rimalux_desktop_0.2.0_20261110.AppImage", "https://example.test/appimage"),
        UpdateAsset("rimalux_desktop_0.2.0_20261110.exe", "https://example.test/exe"),
        UpdateAsset("SHA256SUMS", "https://example.test/sums")
    )

    @Test
    fun picksWindowsInstaller() {
        val asset = UpdateInstaller.selectAsset(assets, UpdatePlatform.WINDOWS)
        assertEquals("https://example.test/exe", asset?.downloadUrl)
    }

    @Test
    fun picksAppImage() {
        val asset = UpdateInstaller.selectAsset(assets, UpdatePlatform.LINUX_APPIMAGE)
        assertEquals("https://example.test/appimage", asset?.downloadUrl)
    }

    @Test
    fun manualPlatformHasNoAsset() {
        assertNull(UpdateInstaller.selectAsset(assets, UpdatePlatform.MANUAL))
    }

    @Test
    fun missingAssetFallsBackToManual() {
        val noExe = assets.filterNot { it.name.endsWith(".exe") }
        assertNull(UpdateInstaller.selectAsset(noExe, UpdatePlatform.WINDOWS))
    }
}
