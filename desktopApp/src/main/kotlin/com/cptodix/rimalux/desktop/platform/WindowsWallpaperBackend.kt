package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.BackendInfo
import com.cptodix.rimalux.desktop.model.FitMode
import com.cptodix.rimalux.desktop.model.WallpaperFile
import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * Windows wallpaper backend.
 *
 * Uses the Windows 8+ `IDesktopWallpaper` COM interface for per-monitor wallpapers and falls back
 * to `SystemParametersInfoW(SPI_SETDESKWALLPAPER)` when COM is unavailable.
 */
class WindowsWallpaperBackend : WallpaperBackend {

    override val info: BackendInfo = BackendInfo(
        id = "windows",
        displayName = "Windows Desktop",
        supportsPerMonitor = true,
        available = isWindows(),
        detail = "IDesktopWallpaper per-monitor with SystemParametersInfoW fallback"
    )

    override suspend fun apply(files: List<WallpaperFile>, fitMode: FitMode): ApplyResult {
        if (files.isEmpty()) return ApplyResult(false, 0, "No wallpaper files to apply")
        if (!isWindows()) {
            return ApplyResult(false, 0, "Windows wallpaper backend is only available on Windows")
        }

        val errors = mutableListOf<String>()
        val position = windowsPosition(fitMode)

        val perMonitor = runCatching { applyPerMonitor(files, position) }
            .getOrElse { error ->
                errors += "IDesktopWallpaper failed: ${error.localizedMessage ?: error.javaClass.simpleName}"
                null
            }
        if (perMonitor != null && perMonitor > 0) {
            return ApplyResult(
                success = true,
                appliedCount = perMonitor,
                message = "Applied to $perMonitor display(s) via IDesktopWallpaper",
                errors = errors
            )
        }

        val global = applyGlobal(files.first().path.toAbsolutePath().toString())
        if (global) {
            return ApplyResult(
                success = true,
                appliedCount = 1,
                message = "Applied to Desktop via SystemParametersInfoW (one wallpaper for all displays)",
                errors = errors
            )
        }
        errors += "SystemParametersInfoW returned false"
        return ApplyResult(
            success = false,
            appliedCount = 0,
            message = "Could not apply Windows wallpaper",
            errors = errors
        )
    }

    private fun applyPerMonitor(files: List<WallpaperFile>, position: Int): Int? {
        val initResult = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_APARTMENTTHREADED)
        val shouldUninitialize = initResult == null || initResult.toInt() >= 0
        try {
            val wallpaper = DesktopWallpaperCom.create() ?: return null
            try {
                wallpaper.setPosition(position)
                wallpaper.setBackgroundColor(0x000000)

                val monitorCount = wallpaper.monitorCount()
                if (monitorCount <= 0) return null
                val monitorIds = (0 until monitorCount).mapNotNull { wallpaper.monitorDevicePathAt(it) }
                if (monitorIds.isEmpty()) return null

                if (files.size == 1) {
                    val hr = wallpaper.setWallpaper(null, files.first().path.toAbsolutePath().toString())
                    return if (succeeded(hr)) 1 else null
                }

                var applied = 0
                files.forEach { file ->
                    val index = monitorIndexFor(file.item.screenIndex, files.size, monitorIds.size)
                    val monitorId = monitorIds.getOrElse(index) { monitorIds.last() }
                    val hr = wallpaper.setWallpaper(
                        monitorId,
                        file.path.toAbsolutePath().toString()
                    )
                    if (succeeded(hr)) applied++
                }
                return applied
            } finally {
                wallpaper.release()
            }
        } finally {
            if (shouldUninitialize) {
                runCatching { Ole32.INSTANCE.CoUninitialize() }
            }
        }
    }

    private fun applyGlobal(path: String): Boolean = runCatching {
        User32Ext.INSTANCE.SystemParametersInfoW(
            SPI_SETDESKWALLPAPER,
            0,
            WString(path),
            SPIF_UPDATE_INI_FILE or SPIF_SEND_WIN_INI_CHANGE
        )
    }.getOrDefault(false)

    private fun succeeded(hr: Int): Boolean = hr >= 0

    companion object {
        private const val SPI_SETDESKWALLPAPER = 0x0014
        private const val SPIF_UPDATE_INI_FILE = 0x01
        private const val SPIF_SEND_WIN_INI_CHANGE = 0x02

        internal fun isWindows(): Boolean =
            System.getProperty("os.name").orEmpty().lowercase().contains("win")

        /** Maps the app's fit mode to DESKTOP_WALLPAPER_POSITION. */
        internal fun windowsPosition(fitMode: FitMode): Int = when (fitMode) {
            FitMode.FILL -> 4 // DWPOS_FILL
            FitMode.FIT -> 3 // DWPOS_FIT
            FitMode.CENTER -> 0 // DWPOS_CENTER
            FitMode.TILE -> 1 // DWPOS_TILE
        }

        /** Chooses the monitor path for a file, preferring its screen index. */
        internal fun monitorIndexFor(screenIndex: Int, fileCount: Int, monitorCount: Int): Int {
            if (monitorCount <= 0) return 0
            if (fileCount == monitorCount) return screenIndex.coerceIn(0, monitorCount - 1)
            return screenIndex.coerceIn(0, monitorCount - 1)
        }
    }
}

private interface User32Ext : StdCallLibrary {
    fun SystemParametersInfoW(
        uiAction: Int,
        uiParam: Int,
        pvParam: WString?,
        fWinIni: Int
    ): Boolean

    companion object {
        val INSTANCE: User32Ext = Native.load(
            "user32",
            User32Ext::class.java,
            W32APIOptions.DEFAULT_OPTIONS
        )
    }
}

/**
 * Minimal vtable wrapper for `IDesktopWallpaper`.
 *
 * JNA does not ship a generated binding for this interface, so the methods are invoked by vtable
 * index. Indices follow `shobjidl_core.h`:
 * QueryInterface(0), AddRef(1), Release(2), SetWallpaper(3), GetWallpaper(4),
 * GetMonitorDevicePathAt(5), GetMonitorDevicePathCount(6), GetMonitorRECT(7),
 * SetBackgroundColor(8), GetBackgroundColor(9), SetPosition(10), GetPosition(11), ...
 */
private class DesktopWallpaperCom private constructor(private val pointer: Pointer) {

    private val vtable: Pointer = pointer.getPointer(0)

    private fun function(index: Int): Function =
        Function.getFunction(
            vtable.getPointer(index.toLong() * Native.POINTER_SIZE),
            Function.ALT_CONVENTION
        )

    fun setWallpaper(monitorId: String?, path: String): Int {
        val args = arrayOf<Any?>(
            pointer,
            monitorId?.let { WString(it) },
            WString(path)
        )
        return function(3).invokeInt(args)
    }

    fun monitorCount(): Int {
        val count = IntByReference()
        val hr = function(6).invokeInt(arrayOf<Any?>(pointer, count))
        return if (hr >= 0) count.value else -1
    }

    fun monitorDevicePathAt(index: Int): String? {
        val out = PointerByReference()
        val hr = function(5).invokeInt(
            arrayOf<Any?>(pointer, WinDef.UINT(index.toLong()), out)
        )
        if (hr < 0) return null
        val value = out.value ?: return null
        return try {
            value.getWideString(0)
        } finally {
            runCatching { Ole32.INSTANCE.CoTaskMemFree(value) }
        }
    }

    fun setBackgroundColor(color: Int) {
        function(8).invokeInt(arrayOf<Any?>(pointer, WinDef.DWORD(color.toLong())))
    }

    fun setPosition(position: Int) {
        function(10).invokeInt(arrayOf<Any?>(pointer, WinDef.DWORD(position.toLong())))
    }

    fun release() {
        runCatching { function(2).invokeInt(arrayOf<Any?>(pointer)) }
    }

    companion object {
        private const val CLSID_DESKTOP_WALLPAPER = "C2CF3110-460E-4FC1-B9D0-8A1C0C9CC4BD"
        private const val IID_IDESKTOP_WALLPAPER = "B92B56A9-8B55-4E14-9A89-0199BBB6F93B"

        fun create(): DesktopWallpaperCom? {
            val ppv = PointerByReference()
            val hr: WinNT.HRESULT = Ole32.INSTANCE.CoCreateInstance(
                Guid.CLSID(CLSID_DESKTOP_WALLPAPER),
                null,
                WTypes.CLSCTX_ALL,
                Guid.GUID(IID_IDESKTOP_WALLPAPER),
                ppv
            )
            if (hr.toInt() < 0) return null
            val pointer = ppv.value ?: return null
            return DesktopWallpaperCom(pointer)
        }
    }
}
