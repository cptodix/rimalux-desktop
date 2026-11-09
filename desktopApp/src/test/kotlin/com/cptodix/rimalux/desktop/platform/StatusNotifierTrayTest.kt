package com.cptodix.rimalux.desktop.platform

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import org.junit.Assert.assertEquals
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

    @Test
    fun exportsPropertiesAndNativeMenu() {
        assumeTrue(
            "No session bus in this environment",
            System.getenv("DBUS_SESSION_BUS_ADDRESS") != null
        )
        val opened = CountDownLatch(1)
        val toggled = CountDownLatch(1)
        val tray = StatusNotifierTray(
            onOpen = { opened.countDown() },
            onToggleAutoChange = { toggled.countDown() },
            onQuit = {}
        )
        try {
            assertTrue(
                tray.install(
                    TrayLabels("Rimalux", "Open Rimalux Desktop", "Auto-Change Wallpaper", "Quit Rimalux"),
                    autoChangeEnabled = false
                )
            )
            val busName = requireNotNull(tray.busName)

            DBusConnectionBuilder.forSessionBus().build().use { connection ->
                val properties = connection.getRemoteObject(
                    busName,
                    "/StatusNotifierItem",
                    Properties::class.java
                )
                val all = properties.GetAll("org.kde.StatusNotifierItem")
                assertTrue("IconPixmap must be exported", all.containsKey("IconPixmap"))
                assertTrue("Menu must point at the dbusmenu object", all.containsKey("Menu"))
                assertEquals(
                    "/MenuBar",
                    (all["Menu"]?.value as? org.freedesktop.dbus.DBusPath)?.path
                )

                val menu = connection.getRemoteObject(
                    busName,
                    "/MenuBar",
                    StatusNotifierTray.DBusMenu::class.java
                )
                val layout = menu.getLayout(0, -1, emptyList())
                val root = layout.root
                assertEquals(4, root.children.size)

                menu.event(1, "clicked", Variant(""), UInt32(0))
                menu.event(2, "clicked", Variant(""), UInt32(0))
            }

            assertTrue("Menu click should open the studio", opened.await(3, TimeUnit.SECONDS))
            assertTrue("Auto-change click should toggle", toggled.await(3, TimeUnit.SECONDS))
        } finally {
            tray.remove()
        }
    }
}
