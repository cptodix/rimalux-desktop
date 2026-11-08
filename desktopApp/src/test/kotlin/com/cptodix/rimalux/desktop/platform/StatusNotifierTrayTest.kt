package com.cptodix.rimalux.desktop.platform

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class StatusNotifierTrayTest {

    @Test
    fun installsRegistersAndActivates() {
        assumeTrue(
            "No session bus in this environment",
            System.getenv("DBUS_SESSION_BUS_ADDRESS") != null
        )
        val opened = CountDownLatch(1)
        val tray = StatusNotifierTray(
            onOpen = { opened.countDown() },
            onToggleAutoChange = {},
            onQuit = {}
        )
        try {
            val installed = tray.install(
                TrayLabels("Rimalux", "Open Rimalux Desktop", "Auto-Change Wallpaper", "Quit Rimalux"),
                autoChangeEnabled = false
            )
            assertTrue("SNI tray should install", installed)
            assertTrue(tray.isInstalled)
            val busName = tray.busName
            assertNotNull(busName)

            DBusConnectionBuilder.forSessionBus().build().use { connection ->
                val proxy = connection.getRemoteObject(
                    busName,
                    "/StatusNotifierItem",
                    StatusNotifierTray.StatusNotifierItem::class.java
                )
                proxy.activate(0, 0)
            }
            assertTrue("Activate should invoke onOpen", opened.await(3, TimeUnit.SECONDS))
        } finally {
            tray.remove()
        }
        assertFalse(tray.isInstalled)
    }
}
