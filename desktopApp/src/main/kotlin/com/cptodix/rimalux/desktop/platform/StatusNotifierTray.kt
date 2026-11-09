package com.cptodix.rimalux.desktop.platform

import java.awt.Color as AwtColor
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JWindow
import javax.swing.SwingUtilities
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.Tuple
import org.freedesktop.dbus.annotations.DBusBoundProperty
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.annotations.DBusProperty.Access
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

private const val SNI_OBJECT_PATH = "/StatusNotifierItem"
private const val MENU_OBJECT_PATH = "/MenuBar"
private const val MENU_INTERFACE = "com.canonical.dbusmenu"

private const val MENU_ID_ROOT = 0
private const val MENU_ID_OPEN = 1
private const val MENU_ID_AUTO_CHANGE = 2
private const val MENU_ID_SEPARATOR = 3
private const val MENU_ID_QUIT = 4

/**
 * Native StatusNotifierItem tray for Linux.
 *
 * KDE Plasma Wayland hosts tray icons through StatusNotifierItem; AWT's XEmbed tray icon appears
 * but its click events are not forwarded. Exporting an SNI item directly gives Plasma a real
 * `Activate` target, and exporting a `com.canonical.dbusmenu` object lets Plasma draw the context
 * menu with its own native styling instead of an AWT popup.
 */
internal class StatusNotifierTray(
    private val onOpen: () -> Unit,
    private val onToggleAutoChange: () -> Unit,
    private val onQuit: () -> Unit
) : TrayBackend {

    private var connection: DBusConnection? = null
    private var item: StatusNotifierItemImpl? = null
    private var menu: DBusMenuImpl? = null
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
                iconPixmaps = AppIconPixmaps.pixmaps(TrayTheme.lightGlyph),
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

            val dbusMenu = DBusMenuImpl(notifier, bus)
            bus.exportObject(MENU_OBJECT_PATH, dbusMenu)

            val watcher = bus.getRemoteObject(
                "org.kde.StatusNotifierWatcher",
                "/StatusNotifierWatcher",
                StatusNotifierWatcher::class.java
            )
            watcher.registerStatusNotifierItem(busName)

            connection = bus
            item = notifier
            menu = dbusMenu
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
        menu?.refresh()
    }

    override fun remove() {
        runCatching { connection?.close() }
        connection = null
        item = null
        menu = null
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
     * Exported SNI object. Bound properties are declared on the interface because dbus-java only
     * exports `org.freedesktop.DBus.Properties` entries annotated there.
     */
    class StatusNotifierItemImpl(
        @Volatile var labels: TrayLabels,
        @Volatile var autoChangeEnabled: Boolean,
        private val iconPixmaps: List<IconPixmapStruct>,
        val callbacks: Callbacks
    ) : StatusNotifierItem {

        override fun getObjectPath(): String = SNI_OBJECT_PATH

        override fun getCategory(): String = "ApplicationStatus"

        override fun getId(): String = "rimalux-desktop"

        override fun getTitle(): String = "Rimalux"

        override fun getStatus(): String = "Active"

        // Empty name makes Plasma fall back to IconPixmap, which ships the real app icon.
        override fun getIconName(): String = ""

        override fun getIconPixmap(): List<IconPixmapStruct> = iconPixmaps

        override fun isItemIsMenu(): Boolean = false

        override fun getMenu(): DBusPath = DBusPath(MENU_OBJECT_PATH)

        override fun getWindowId(): Int = 0

        override fun activate(x: Int, y: Int) {
            callbacks.open()
        }

        override fun secondaryActivate(x: Int, y: Int) {
            callbacks.open()
        }

        override fun contextMenu(x: Int, y: Int) {
            callbacks.showMenu(x, y, labels, autoChangeEnabled)
        }

        override fun scroll(delta: Int, orientation: String) = Unit
    }

    @DBusInterfaceName("org.kde.StatusNotifierItem")
    interface StatusNotifierItem : DBusInterface {
        @DBusBoundProperty(name = "Category", access = Access.READ)
        fun getCategory(): String

        @DBusBoundProperty(name = "Id", access = Access.READ)
        fun getId(): String

        @DBusBoundProperty(name = "Title", access = Access.READ)
        fun getTitle(): String

        @DBusBoundProperty(name = "Status", access = Access.READ)
        fun getStatus(): String

        @DBusBoundProperty(name = "IconName", access = Access.READ)
        fun getIconName(): String

        @DBusBoundProperty(name = "IconPixmap", access = Access.READ)
        fun getIconPixmap(): List<IconPixmapStruct>

        @DBusBoundProperty(name = "ItemIsMenu", access = Access.READ)
        fun isItemIsMenu(): Boolean

        @DBusBoundProperty(name = "Menu", access = Access.READ)
        fun getMenu(): DBusPath

        @DBusBoundProperty(name = "WindowId", access = Access.READ)
        fun getWindowId(): Int

        @DBusMemberName("Activate")
        fun activate(x: Int, y: Int)

        @DBusMemberName("SecondaryActivate")
        fun secondaryActivate(x: Int, y: Int)

        @DBusMemberName("ContextMenu")
        fun contextMenu(x: Int, y: Int)

        @DBusMemberName("Scroll")
        fun scroll(delta: Int, orientation: String)
    }

    /**
     * `com.canonical.dbusmenu` model so the desktop draws its own native menu.
     */
    class DBusMenuImpl(
        private val item: StatusNotifierItemImpl,
        private val connection: DBusConnection
    ) : DBusMenu {

        @Volatile
        private var revision: UInt32 = UInt32(0)

        override fun getObjectPath(): String = MENU_OBJECT_PATH

        override fun getVersion(): UInt32 = UInt32(3)

        override fun getTextDirection(): String = "ltr"

        override fun getStatus(): String = "normal"

        override fun getIconThemePath(): List<String> = emptyList()

        override fun getLayout(
            parentId: Int,
            recursionDepth: Int,
            propertyNames: List<String>
        ): MenuLayoutTuple<UInt32, MenuNode> {
            val includeChildren = recursionDepth != 0
            val node = when (parentId) {
                MENU_ID_ROOT -> MenuNode(
                    id = MENU_ID_ROOT,
                    properties = rootProperties(),
                    children = if (includeChildren) {
                        entries().map { entry ->
                            Variant(
                                MenuNode(entry.id, entry.properties(), emptyList()),
                                "(ia{sv}av)"
                            )
                        }
                    } else {
                        emptyList()
                    }
                )

                else -> {
                    val entry = entries().firstOrNull { it.id == parentId }
                    MenuNode(
                        id = parentId,
                        properties = entry?.properties() ?: emptyMap(),
                        children = emptyList()
                    )
                }
            }
            return MenuLayoutTuple(revision, node)
        }

        override fun getGroupProperties(
            ids: List<Int>,
            propertyNames: List<String>
        ): List<MenuPropertiesStruct> {
            val requested = if (ids.isEmpty()) {
                listOf(MENU_ID_ROOT) + entries().map { it.id }
            } else {
                ids
            }
            return requested.mapNotNull { id ->
                val properties = if (id == MENU_ID_ROOT) {
                    rootProperties()
                } else {
                    entries().firstOrNull { it.id == id }?.properties() ?: return@mapNotNull null
                }
                MenuPropertiesStruct(id, properties)
            }
        }

        override fun getProperty(id: Int, name: String): Variant<*> {
            val properties = if (id == MENU_ID_ROOT) {
                rootProperties()
            } else {
                entries().firstOrNull { it.id == id }?.properties() ?: emptyMap()
            }
            return properties[name] ?: Variant("")
        }

        override fun event(id: Int, eventId: String, data: Variant<*>, timestamp: UInt32) {
            if (eventId != "clicked") return
            when (id) {
                MENU_ID_OPEN -> item.activate(0, 0)
                MENU_ID_AUTO_CHANGE -> {
                    item.autoChangeEnabled = !item.autoChangeEnabled
                    refresh()
                    item.callbacks.toggleAutoChange()
                }

                MENU_ID_QUIT -> item.callbacks.quit()
            }
        }

        override fun eventGroup(events: List<MenuEventStruct>) {
            events.forEach { event(it.id, it.eventId, it.data, it.timestamp) }
        }

        override fun aboutToShow(id: Int): Boolean = false

        override fun aboutToShowGroup(ids: List<Int>): AboutToShowGroupTuple<List<Int>, List<Int>> =
            AboutToShowGroupTuple(emptyList(), emptyList())

        /** Bump the revision and tell the host to refetch the layout. */
        fun refresh() {
            revision = UInt32(revision.toLong() + 1)
            runCatching {
                connection.sendMessage(DBusMenu.LayoutUpdated(MENU_OBJECT_PATH, revision, MENU_ID_ROOT))
            }
        }

        private fun rootProperties(): Map<String, Variant<*>> =
            mapOf("children-display" to Variant("submenu"))

        private fun entries(): List<MenuEntry> = listOf(
            MenuEntry(MENU_ID_OPEN, label = item.labels.open),
            MenuEntry(
                MENU_ID_AUTO_CHANGE,
                label = item.labels.autoChange,
                toggleType = "checkmark",
                toggleState = if (item.autoChangeEnabled) 1 else 0
            ),
            MenuEntry(MENU_ID_SEPARATOR, type = "separator"),
            MenuEntry(MENU_ID_QUIT, label = item.labels.quit)
        )
    }

    private data class MenuEntry(
        val id: Int,
        val label: String? = null,
        val type: String? = null,
        val toggleType: String? = null,
        val toggleState: Int? = null
    ) {
        fun properties(): Map<String, Variant<*>> = buildMap {
            label?.let { put("label", Variant(it)) }
            type?.let { put("type", Variant(it)) }
            toggleType?.let { put("toggle-type", Variant(it)) }
            toggleState?.let { put("toggle-state", Variant(it)) }
        }
    }

    /** Root layout node: `(ia{sv}av)`. */
    class MenuNode(
        @field:Position(0) val id: Int,
        @field:Position(1) val properties: Map<String, Variant<*>>,
        @field:Position(2) val children: List<Variant<*>>
    ) : Struct()

    /** `GetLayout` result: `(u(ia{sv}av))`. */
    class MenuLayoutTuple<A, B>(
        @field:Position(0) val revision: A,
        @field:Position(1) val root: B
    ) : Tuple()

    /** `GetGroupProperties` element: `(ia{sv})`. */
    class MenuPropertiesStruct(
        @field:Position(0) val id: Int,
        @field:Position(1) val properties: Map<String, Variant<*>>
    ) : Struct()

    /** `EventGroup` element: `(isvu)`. */
    class MenuEventStruct(
        @field:Position(0) val id: Int,
        @field:Position(1) val eventId: String,
        @field:Position(2) val data: Variant<*>,
        @field:Position(3) val timestamp: UInt32
    ) : Struct()

    /** `AboutToShowGroup` result: `(aiai)`. */
    class AboutToShowGroupTuple<A, B>(
        @field:Position(0) val updatesNeeded: A,
        @field:Position(1) val idErrors: B
    ) : Tuple()

    /** `(iiay)` icon payload. */
    class IconPixmapStruct(
        @field:Position(0) val width: Int,
        @field:Position(1) val height: Int,
        @field:Position(2) val data: ByteArray
    ) : Struct()

    @DBusInterfaceName(MENU_INTERFACE)
    interface DBusMenu : DBusInterface {
        @DBusBoundProperty(name = "Version", access = Access.READ)
        fun getVersion(): UInt32

        @DBusBoundProperty(name = "TextDirection", access = Access.READ)
        fun getTextDirection(): String

        @DBusBoundProperty(name = "Status", access = Access.READ)
        fun getStatus(): String

        @DBusBoundProperty(name = "IconThemePath", access = Access.READ)
        fun getIconThemePath(): List<String>

        @DBusMemberName("GetLayout")
        fun getLayout(
            parentId: Int,
            recursionDepth: Int,
            propertyNames: List<String>
        ): MenuLayoutTuple<UInt32, MenuNode>

        @DBusMemberName("GetGroupProperties")
        fun getGroupProperties(
            ids: List<Int>,
            propertyNames: List<String>
        ): List<MenuPropertiesStruct>

        @DBusMemberName("GetProperty")
        fun getProperty(id: Int, name: String): Variant<*>

        @DBusMemberName("Event")
        fun event(id: Int, eventId: String, data: Variant<*>, timestamp: UInt32)

        @DBusMemberName("EventGroup")
        fun eventGroup(events: List<MenuEventStruct>)

        @DBusMemberName("AboutToShow")
        fun aboutToShow(id: Int): Boolean

        @DBusMemberName("AboutToShowGroup")
        fun aboutToShowGroup(ids: List<Int>): AboutToShowGroupTuple<List<Int>, List<Int>>

        /** `LayoutUpdated(u i)`: revision, parent id. */
        class LayoutUpdated(path: String, revision: UInt32, parent: Int) :
            DBusSignal(path, revision, parent)
    }

    @DBusInterfaceName("org.kde.StatusNotifierWatcher")
    interface StatusNotifierWatcher : DBusInterface {
        @DBusMemberName("RegisterStatusNotifierItem")
        fun registerStatusNotifierItem(service: String)
    }

    private object AppIconPixmaps {
        private val sizes = listOf(22, 24, 32, 48)
        private val onDark by lazy { load("/icons/tray-on-dark.png") }
        private val onLight by lazy { load("/icons/tray-on-light.png") }

        fun pixmaps(lightGlyph: Boolean): List<IconPixmapStruct> =
            if (lightGlyph) onDark else onLight

        private fun load(path: String): List<IconPixmapStruct> {
            val source = runCatching {
                StatusNotifierTray::class.java.getResourceAsStream(path)
                    ?.use { ImageIO.read(it) }
            }.getOrNull() ?: return emptyList()

            return sizes.mapNotNull { size ->
                runCatching { render(source, size) }.getOrNull()
            }
        }

        private fun render(source: BufferedImage, size: Int): IconPixmapStruct {
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
                graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
                )
                graphics.drawImage(source, 0, 0, size, size, null)
            } finally {
                graphics.dispose()
            }

            // SNI IconPixmap uses ARGB32 in network byte order.
            val data = ByteArray(size * size * 4)
            var offset = 0
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val argb = image.getRGB(x, y)
                    data[offset++] = ((argb ushr 24) and 0xFF).toByte()
                    data[offset++] = ((argb ushr 16) and 0xFF).toByte()
                    data[offset++] = ((argb ushr 8) and 0xFF).toByte()
                    data[offset++] = (argb and 0xFF).toByte()
                }
            }
            return IconPixmapStruct(size, size, data)
        }
    }
}
