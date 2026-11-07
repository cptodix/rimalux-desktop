# Windows Wallpaper Backends

> **Status: implemented in Phase 3.** The backend, autostart, tray, and CI packaging are in
> place. Actual wallpaper behavior must still be verified on Windows 10/11 hardware or a VM
> before calling the release stable.

## APIs

### Global wallpaper

Use `SystemParametersInfoW` with:

- `SPI_SETDESKWALLPAPER = 0x0014`
- `SPIF_UPDATEINIFILE = 0x01`
- `SPIF_SENDWININICHANGE = 0x02`
- Absolute path to a PNG file.

JNA declaration:

```kotlin
interface User32Ext : Library {
    fun SystemParametersInfoW(
        uiAction: Int,
        uiParam: Int,
        pvParam: WString,
        fWinIni: Int
    ): Boolean

    companion object {
        val INSTANCE: User32Ext = Native.load("user32", User32Ext::class.java)
    }
}
```

Also set the fit mode in the registry:

```text
HKCU\Control Panel\Desktop\WallpaperStyle = "10"   # Fill
HKCU\Control Panel\Desktop\TileWallpaper  = "0"
```

`WallpaperStyle` values: `10` fill, `6` fit, `0` center, `2` stretch, `22` span.
Do not change the user's fit mode unless the app’s fit setting is explicit.

### Per-monitor wallpaper

Use the `IDesktopWallpaper` COM interface (Windows 8+):

- CLSID: `{C2CF3110-460E-4fc1-B9D0-8A1C0C9CC4BD}`
- IID: `{B92B56A9-8B55-4E14-9A89-0199BBB6F93B}`

Methods used:

```text
SetWallpaper(monitorID, wallpaperPath)
GetWallpaper(monitorID, out wallpaperPath)
GetMonitorDevicePathCount(out count)
GetMonitorDevicePathAt(index, out monitorID)
GetMonitorRECT(monitorID, out rect)
SetPosition(DESKTOP_WALLPAPER_POSITION_FILL = 4)
Enable(TRUE)
```

Implementation approach (`WindowsWallpaperBackend`):

- Use JNA `Ole32.CoCreateInstance` plus a custom vtable wrapper for `IDesktopWallpaper`.
- Map rendered files to COM monitor device paths by screen index, clamped to the monitor count.
- Apply one PNG per monitor, set `DWPOS_FILL`/`FIT`/`CENTER`/`TILE`, and force a `#000000`
  desktop background.
- If COM fails, fall back to `SystemParametersInfoW` with the first image and report a global
  fallback warning.
- Release the COM interface and uninitialize the COM apartment on the calling thread.

## Lock screen

Windows does **not** provide a public API for apps to set the lock-screen image. Do not show a
lock-screen target. Users can change it manually in Settings → Personalization → Lock screen.

## Autostart

Write to:

```text
HKCU\Software\Microsoft\Windows\CurrentVersion\Run
```

Value name: `Rimalux`

Value data:

```text
"C:\Path\To\Rimalux.exe" --background
```

Use JNA `Advapi32Util.registrySetStringValue` / `registryDeleteValue`. Recreate the value on
startup if `runAtLogin` is true and the value is missing.

## Tray

- AWT `SystemTray` (`DesktopTray`), shared with Linux.
- Left click opens/focuses the window.
- Right-click menu: Open Rimalux Desktop, Auto-Change toggle, Quit Rimalux.
- Close-to-tray if auto-change is enabled or Keep in Tray is on.
- Notifications are optional; rotation failures are logged and shown as `lastError`.

## Packaging

- `compose.desktop.application.nativeDistributions.windows { perUserInstall = true }`
- Stable `upgradeUuid`.
- `menuGroup = "Rimalux"`.
- MSI and EXE must be built on a Windows runner.
- Unsigned installers trigger SmartScreen; code signing is recommended for release.

## Verification without a Windows machine

Because the developer may not have Windows hardware:

1. **Unit tests** cover the pure parts:
   - fit-mode → `DESKTOP_WALLPAPER_POSITION` mapping
   - monitor index clamping
   - autostart entry content
   - update-version comparison
2. **CI self-test** (`rimalux --test-backend`):
   - list monitors and backend availability
   - report whether `IDesktopWallpaper` is expected to be usable
   - never changes the wallpaper
3. **Human checklist** before stable release:
   - install MSI per-user
   - launch from Start menu
   - apply a wallpaper
   - verify fill mode and true black visually
   - restart and verify settings
   - enable auto-change, wait for a rotation
   - enable Start at Login, sign out/in, verify hidden start
   - test on a multi-monitor setup if possible
   - uninstall and verify no leftover autostart entry (or document cleanup)

## Known limitations

- Lock screen not settable.
- Per-monitor COM requires a real Windows desktop session; CI may not exercise it fully.
- Some enterprise policies override wallpaper changes.
- The engine caps images at 4096 px on the longest axis.
- Windows may transcode/copy the wallpaper; keep the source file until the next apply.
