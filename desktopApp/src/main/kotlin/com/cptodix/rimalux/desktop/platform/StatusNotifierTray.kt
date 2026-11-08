package com.cptodix.rimalux.desktop.platform

import java.awt.Color as AwtColor
import java.awt.GraphicsEnvironment
import javax.swing.BorderFactory
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JWindow
import javax.swing.SwingUtilities
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.annotations.DBusProperty.Access
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface

private const val SNI_OBJECT_PATH = "/StatusNotifierItem"

/**
 * Native StatusNotifierItem tray for Linux.
 *
 * KDE Plasma Wayland hosts tray icons through StatusNotifierItem; AWT's XEmbed tray icon appears
 * but its click events are not forwarded. Exporting an SNI item directly gives Plasma a real
 * `Activate`/`ContextMenu` target.
 */
internal class StatusNotifierTray(
    private val onOpen: () -> Unit,
    private val onToggleAutoChange: () -> Unit,
    private val onQuit: () -> Unit
) : TrayBackend {

    private var connection: DBusConnection? = null
    private var item: StatusNotifierItemImpl? = null
    private var invoker: JWindow? = null

    internal var busName: String? = null
        private set

    override var isInstalled: Boolean = false
        private set

    override fun install(labels: TrayLabels, autoChangeEnabled: Boolean): Boolean {
        if (isInstalled) {
            update(labels, autoChangeEnabled)
            return true
        }
        if (!GraphicsEnvironment.isHeadless()) {
            // Ensure the AWT popup invoker can be created on the AWT thread.
            runCatching { ensureInvoker() }
        }
        return runCatching {
            val bus = DBusConnectionBuilder.forSessionBus()
                .withShared(false)
                .build()
            val busName = "org.kde.StatusNotifierItem-${ProcessHandle.current().pid()}-1"
            bus.requestBusName(busName)

            val notifier = StatusNotifierItemImpl(
                labels = labels,
                autoChangeEnabled = autoChangeEnabled,
                callbacks = Callbacks(
                    open = { SwingUtilities.invokeLater(onOpen) },
                    toggleAutoChange = { SwingUtilities.invokeLater(onToggleAutoChange) },
                    quit = { SwingUtilities.invokeLater(onQuit) },
                    showMenu = { x, y, menuLabels, enabled ->
                        SwingUtilities.invokeLater { showMenu(x, y, menuLabels, enabled) }
                    }
                )
            )
            bus.exportObject(SNI_OBJECT_PATH, notifier)

            val watcher = bus.getRemoteObject(
                "org.kde.StatusNotifierWatcher",
                "/StatusNotifierWatcher",
                StatusNotifierWatcher::class.java
            )
            watcher.registerStatusNotifierItem(busName)

            connection = bus
            item = notifier
            this.busName = busName
            isInstalled = true
            AppLog.info("StatusNotifierItem tray installed as $busName")
            true
        }.onFailure { error ->
            AppLog.warning("Could not install StatusNotifierItem tray", error)
            remove()
        }.getOrDefault(false)
    }

    override fun update(labels: TrayLabels, autoChangeEnabled: Boolean) {
        item?.labels = labels
        item?.autoChangeEnabled = autoChangeEnabled
    }

    override fun remove() {
        runCatching { connection?.close() }
        connection = null
        item = null
        busName = null
        runCatching { invoker?.dispose() }
        invoker = null
        isInstalled = false
    }

    private fun ensureInvoker(): JWindow {
        invoker?.let { return it }
        val window = JWindow()
        window.setSize(1, 1)
        window.setLocation(-10_000, -10_000)
        window.isVisible = true
        invoker = window
        return window
    }

    private fun showMenu(x: Int, y: Int, labels: TrayLabels, autoEnabled: Boolean) {
        val owner = runCatching { ensureInvoker() }.getOrNull() ?: return
        val menu = JPopupMenu().apply {
            background = AwtColor(18, 18, 20)
            border = BorderFactory.createLineBorder(AwtColor(58, 58, 64))
        }
        menu.add(styledMenuItem(JMenuItem(labels.open).apply {
            addActionListener { onOpen() }
        }))
        var lastKnown = autoEnabled
        val autoItem = JCheckBoxMenuItem(labels.autoChange, autoEnabled)
        autoItem.addItemListener {
            if (autoItem.state != lastKnown) {
                lastKnown = autoItem.state
                onToggleAutoChange()
            }
        }
        menu.add(styledMenuItem(autoItem))
        menu.addSeparator()
        menu.add(styledMenuItem(JMenuItem(labels.quit).apply {
            addActionListener { onQuit() }
        }))
        menu.show(owner, x, y)
    }

    private fun styledMenuItem(item: JMenuItem): JMenuItem = item.apply {
        background = AwtColor(18, 18, 20)
        foreground = AwtColor(235, 235, 240)
        isOpaque = true
    }

    data class Callbacks(
        val open: () -> Unit,
        val toggleAutoChange: () -> Unit,
        val quit: () -> Unit,
        val showMenu: (Int, Int, TrayLabels, Boolean) -> Unit
    )

    /**
     * Exported SNI object. Properties are read by Plasma through the standard DBus properties
     * interface generated by dbus-java.
     */
    class StatusNotifierItemImpl(
        @Volatile var labels: TrayLabels,
        @Volatile var autoChangeEnabled: Boolean,
        private val callbacks: Callbacks
    ) : StatusNotifierItem {

        override fun getObjectPath(): String = SNI_OBJECT_PATH

        @DBusBoundProperty(name = "Category", access = Access.READ)
        fun getCategory(): String = "ApplicationStatus"

        @DBusBoundProperty(name = "Id", access = Access.READ)
        fun getId(): String = "rimalux-desktop"

        @DBusBoundProperty(name = "Title", access = Access.READ)
        fun getTitle(): String = "Rimalux"

        @DBusBoundProperty(name = "Status", access = Access.READ)
        fun getStatus(): String = "Active"

        @DBusBoundProperty(name = "IconName", access = Access.READ)
        fun getIconName(): String = "preferences-desktop-wallpaper"

        @DBusBoundProperty(name = "ItemIsMenu", access = Access.READ)
        fun isItemIsMenu(): Boolean = false

        @DBusBoundProperty(name = "Menu", access = Access.READ)
        fun getMenu(): DBusPath = DBusPath("/NO_DBUSMENU")

        @DBusBoundProperty(name = "WindowId", access = Access.READ)
        fun getWindowId(): Int = 0

        @DBusMemberName("Activate")
        override fun activate(x: Int, y: Int) {
            callbacks.open()
        }

        @DBusMemberName("SecondaryActivate")
        override fun secondaryActivate(x: Int, y: Int) {
            callbacks.open()
        }

        @DBusMemberName("ContextMenu")
        override fun contextMenu(x: Int, y: Int) {
            callbacks.showMenu(x, y, labels, autoChangeEnabled)
        }

        @DBusMemberName("Scroll")
        override fun scroll(delta: Int, orientation: String) = Unit
    }

    @DBusInterfaceName("org.kde.StatusNotifierItem")
    interface StatusNotifierItem : DBusInterface {
        @DBusMemberName("Activate")
        fun activate(x: Int, y: Int)

        @DBusMemberName("SecondaryActivate")
        fun secondaryActivate(x: Int, y: Int)

        @DBusMemberName("ContextMenu")
        fun contextMenu(x: Int, y: Int)

        @DBusMemberName("Scroll")
        fun scroll(delta: Int, orientation: String)
    }

    @DBusInterfaceName("org.kde.StatusNotifierWatcher")
    interface StatusNotifierWatcher : DBusInterface {
        @DBusMemberName("RegisterStatusNotifierItem")
        fun registerStatusNotifierItem(service: String)
    }
}
