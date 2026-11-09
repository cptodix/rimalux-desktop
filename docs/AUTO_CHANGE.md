# Auto-Change Design

> **Status: Phase 2 implemented and verified in normal use; optional OS scheduler implemented in
> Phase 4.** This document describes the auto-change/tray implementation.

## Desktop reality

Android uses WorkManager + AlarmManager to wake the app and rotate wallpapers. Desktop has no
equivalent that works uniformly across Linux and Windows. Rimalux therefore uses:

1. **Primary mode:** a tray-resident app with an opt-in Start at Login entry.
2. **Optional advanced mode:** an OS scheduler (systemd user timer or Windows Task Scheduler)
   that invokes `rimalux --rotate-now`.

## Settings

```text
AutoChangeSettings
├── enabled: Boolean
├── intervalMinutes: Int            # 1..10080
├── nextRunEpochMillis: Long
├── lastRunEpochMillis: Long
├── runAtLogin: Boolean
├── keepInTray: Boolean
└── lastError: String?
```

Target displays are stored in `DisplaySettings.targetMonitorIds` (empty = all displays).

## Scheduler loop

- Tick every 15 seconds while the app is running.
- On tick:
  - If disabled, do nothing.
  - If `nextRun == 0`, set `nextRun = now + interval`.
  - If `now >= nextRun`, call `rotate(force = false)`.
- `rotate` acquires a global rotation mutex.
- Dedupe window: `max(30_000, intervalMillis * 3 / 4)`.
  - If `now - lastRun < window` and not forced, skip and reschedule.
- On success:
  - Save the new `WallpaperSet`, cache PNGs, update history.
  - Set `lastRun = now`, `nextRun = now + interval`.
  - Update the UI preview if the window is open.
- On failure:
  - Keep the old wallpaper and current set.
  - Record `lastError`, log command/exit code/stderr.
  - Retry at the next interval (or a short 5-minute retry for transient failures).
- Manual apply resets `lastRun = now` and `nextRun = now + interval`.

## Startup and wake behavior

- Load settings and monitors.
- If auto-change is enabled and `nextRun` is in the past:
  - Rotate once if overdue by more than 60 seconds.
  - Otherwise reschedule from now.
- After system sleep, the timer resumes late; the wall-clock check triggers a catch-up rotation
  if due.
- If the app was force-quit and restarted, the persisted timestamps drive catch-up.

## Tray

- Linux/KDE: native **StatusNotifierItem** over DBus (`StatusNotifierTray`). AWT’s XEmbed tray
  icon appears on Plasma Wayland but does not receive click events, so it is only a fallback.
  - The item exports `IconPixmap` (the app icon, ARGB32) so Plasma always has a visible icon,
    and bound properties are declared on the DBus interface so `org.freedesktop.DBus.Properties`
    can read them.
  - The context menu is a `com.canonical.dbusmenu` object at `/MenuBar`, referenced by the
    item’s `Menu` property. Plasma renders it with its own native menu styling; the AWT popup is
    only a fallback for hosts that call `ContextMenu` instead.
- Windows: AWT `SystemTray` (`AwtTray`).
- Left click: open/focus the Rimalux window.
- Right-click context menu:
  1. Open Rimalux Desktop
  2. Auto-Change Wallpaper (checkable toggle)
  3. Quit Rimalux
- Close button behavior:
  - If auto-change is enabled or `keepInTray` is on, hide the window.
  - Otherwise quit.
- If the tray is unavailable (e.g., GNOME without AppIndicator), the window stays visible and
  closing quits; autostart still works but has no tray to return to.

## Start at Login

### Linux

Write `~/.config/autostart/rimalux.desktop`:

```ini
[Desktop Entry]
Type=Application
Name=Rimalux
Comment=Procedural OLED wallpaper studio
Exec="/path/to/rimalux" --background
Icon=rimalux
Terminal=false
X-GNOME-Autostart-enabled=true
```

Remove the file when disabled. Recreate it on startup if `runAtLogin` is true but the file is
missing (e.g., after reinstall).

### Windows

Write `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` value `Rimalux`:

```text
"C:\Path\To\Rimalux.exe" --background
```

Remove the value when disabled. Recreate on startup if missing.

## Single instance

- Acquire a lock file in the config directory.
- If the lock is held, connect to a localhost socket stored in `instance.port`, send `SHOW`,
  and exit.
- The running instance shows/focuses its window.
- Release the lock and delete the port file on exit.

## CLI modes

| Flag | Behavior |
|---|---|
| `--background` | Start hidden to tray |
| `--rotate-now` | Perform one rotation, write state, exit (for OS schedulers/tests) |
| `--test-backend` | Report backend availability and monitors without changing the wallpaper |
| `--diagnostics-bundle [path]` | Write a support ZIP with system/settings/monitor/backend/log data |
| `--check-updates` | Query GitHub releases and print whether a newer version exists |
| `--install-os-scheduler [minutes]` | Install the systemd user timer / Windows scheduled task |
| `--remove-os-scheduler` | Remove the OS-level scheduler |
| `--list-monitors` | Print monitors and detected backend |
| `--diagnostics` | Print OS, locale, backend, paths, engine commit |
| `--version` | Print version |

## Optional OS scheduler mode

The in-app tray scheduler is primary. The optional OS scheduler runs `--rotate-now` even when
Rimalux is not running. Install it from **Settings → Background Scheduler** or with
`--install-os-scheduler [minutes]`.

### Linux: systemd user timer

`~/.config/systemd/user/rimalux-rotate.service`:

```ini
[Unit]
Description=Rimalux wallpaper rotation

[Service]
Type=oneshot
ExecStart="/path/to/Rimalux" --rotate-now
```

`~/.config/systemd/user/rimalux-rotate.timer`:

```ini
[Unit]
Description=Rimalux wallpaper rotation timer

[Timer]
OnBootSec=2min
OnUnitActiveSec=60min
Persistent=true
Unit=rimalux-rotate.service

[Install]
WantedBy=timers.target
```

The app writes both files, runs `systemctl --user daemon-reload`, and enables the timer. Remove
it from the UI or with `--remove-os-scheduler`.

### Windows: Task Scheduler

```powershell
schtasks /Create /TN "RimaluxRotate" /TR "\"C:\Path\To\Rimalux.exe\" --rotate-now" /SC MINUTE /MO 60 /F
```

The task runs only while the user is logged on. Reinstall after changing the interval; remove
with `--remove-os-scheduler`.

## Reliability and diagnostics

- Log every rotation: timestamp, set id, monitors, patterns, resolutions, backend, duration,
  success/failure.
- Show “Last change”, “Next change”, and “Last error” in the Auto-Change section.
- **Settings → Support → Export Diagnostics** writes a ZIP with system info, redacted settings,
  monitor/backend state, and recent logs. Custom commands are redacted.
- **Settings → Support → Check for Updates** queries the GitHub releases API.
- Do not delete the currently applied wallpaper file.

## Tests

- Fake clock + fake backend:
  - enabling schedules the first run
  - due rotation applies once
  - dedupe prevents double rotation
  - missed run catch-up
  - interval change reschedules
  - manual apply resets next run
  - failure records error and keeps old state
- Autostart file/registry content tests.
- Single-instance lock/socket test.
- Manual Linux KDE 1-minute rotation test.
- Windows CI `--test-backend` smoke test.
