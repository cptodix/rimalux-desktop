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

/**
 * Native tray integration.
 *
 * Left click opens the studio window. Right click opens a compact menu with exactly the
 * desktop-level actions: open, toggle auto-change, and quit. The menu is rebuilt whenever the
 * auto-change state or the language changes.
 */
class DesktopTray(
    private val image: Image,
    private val onOpen: () -> Unit,
    private val onToggleAutoChange: () -> Unit,
    private val onQuit: () -> Unit
) {
    private var trayIcon: TrayIcon? = null
    private var popup: PopupMenu? = null
    private var openItem: MenuItem? = null
    private var autoItem: CheckboxMenuItem? = null
    private var quitItem: MenuItem? = null
    private var lastKnownAutoState: Boolean = false

    var isInstalled: Boolean = false
        private set

    fun install(labels: TrayLabels, autoChangeEnabled: Boolean): Boolean {
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
            AppLog.warning("Could not install tray icon", error)
            remove()
        }.getOrDefault(false)
    }

    fun update(labels: TrayLabels, autoChangeEnabled: Boolean) {
        openItem?.label = labels.open
        autoItem?.apply {
            label = labels.autoChange
            lastKnownAutoState = autoChangeEnabled
            state = autoChangeEnabled
        }
        quitItem?.label = labels.quit
        trayIcon?.setToolTip(labels.tooltip)
    }

    fun remove() {
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
