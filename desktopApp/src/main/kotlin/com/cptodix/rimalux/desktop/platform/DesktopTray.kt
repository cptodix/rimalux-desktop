package com.cptodix.rimalux.desktop.platform

import java.awt.CheckboxMenuItem
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

interface TrayBackend {
    val isInstalled: Boolean
    fun install(labels: TrayLabels, autoChangeEnabled: Boolean): Boolean
    fun update(labels: TrayLabels, autoChangeEnabled: Boolean)
    fun remove()
}

/**
 * Tray facade.
 *
 * On Linux, prefers a native StatusNotifierItem because KDE Plasma Wayland does not forward
 * tray clicks to AWT's XEmbed tray icon. Falls back to AWT `SystemTray` on Windows and on Linux
 * desktops without a StatusNotifierWatcher.
 */
class DesktopTray(
    private val image: Image,
    private val onOpen: () -> Unit,
    private val onToggleAutoChange: () -> Unit,
    private val onQuit: () -> Unit
) {
    private var backend: TrayBackend? = null

    val isInstalled: Boolean
        get() = backend?.isInstalled == true

    fun install(labels: TrayLabels, autoChangeEnabled: Boolean): Boolean {
        backend?.let { existing ->
            existing.update(labels, autoChangeEnabled)
            return existing.isInstalled
        }

        val osName = System.getProperty("os.name").orEmpty().lowercase()
        if (osName.contains("linux") && System.getenv("DBUS_SESSION_BUS_ADDRESS") != null) {
            val sni = StatusNotifierTray(onOpen, onToggleAutoChange, onQuit)
            if (sni.install(labels, autoChangeEnabled)) {
                backend = sni
                return true
            }
        }

        val awt = AwtTray(image, onOpen, onToggleAutoChange, onQuit)
        if (awt.install(labels, autoChangeEnabled)) {
            backend = awt
            return true
        }
        return false
    }

    fun update(labels: TrayLabels, autoChangeEnabled: Boolean) {
        backend?.update(labels, autoChangeEnabled)
    }

    fun remove() {
        backend?.remove()
        backend = null
    }
}

/**
 * AWT tray fallback. Left click opens the studio; right click opens the compact menu.
 */
internal class AwtTray(
    private val image: Image,
    private val onOpen: () -> Unit,
    private val onToggleAutoChange: () -> Unit,
    private val onQuit: () -> Unit
) : TrayBackend {
    private var trayIcon: TrayIcon? = null
    private var popup: PopupMenu? = null
    private var openItem: MenuItem? = null
    private var autoItem: CheckboxMenuItem? = null
    private var quitItem: MenuItem? = null
    private var lastKnownAutoState: Boolean = false

    override var isInstalled: Boolean = false
        private set

    override fun install(labels: TrayLabels, autoChangeEnabled: Boolean): Boolean {
        if (isInstalled) {
            update(labels, autoChangeEnabled)
            return true
        }
        if (!SystemTray.isSupported()) return false
        return runCatching {
            val menu = PopupMenu().apply {
                openItem = MenuItem(labels.open).also { item ->
                    item.addActionListener { SwingUtilities.invokeLater(onOpen) }
                    add(item)
                }
                autoItem = CheckboxMenuItem(labels.autoChange, autoChangeEnabled).also { item ->
                    lastKnownAutoState = autoChangeEnabled
                    item.addItemListener {
                        if (item.state != lastKnownAutoState) {
                            lastKnownAutoState = item.state
                            SwingUtilities.invokeLater(onToggleAutoChange)
                        }
                    }
                    add(item)
                }
                addSeparator()
                quitItem = MenuItem(labels.quit).also { item ->
                    item.addActionListener { SwingUtilities.invokeLater(onQuit) }
                    add(item)
                }
            }
            val icon = TrayIcon(image, labels.tooltip, menu).apply {
                isImageAutoSize = true
                addActionListener { SwingUtilities.invokeLater(onOpen) }
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(event: MouseEvent) {
                        if (SwingUtilities.isLeftMouseButton(event)) {
                            SwingUtilities.invokeLater(onOpen)
                        }
                    }
                })
            }
            SystemTray.getSystemTray().add(icon)
            popup = menu
            trayIcon = icon
            isInstalled = true
            true
        }.onFailure { error ->
            AppLog.warning("Could not install AWT tray icon", error)
            remove()
        }.getOrDefault(false)
    }

    override fun update(labels: TrayLabels, autoChangeEnabled: Boolean) {
        openItem?.label = labels.open
        autoItem?.apply {
            label = labels.autoChange
            lastKnownAutoState = autoChangeEnabled
            state = autoChangeEnabled
        }
        quitItem?.label = labels.quit
        trayIcon?.setToolTip(labels.tooltip)
    }

    override fun remove() {
        runCatching { trayIcon?.let { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        popup = null
        openItem = null
        autoItem = null
        quitItem = null
        isInstalled = false
    }
}

data class TrayLabels(
    val tooltip: String,
    val open: String,
    val autoChange: String,
    val quit: String
)
