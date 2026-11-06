# Auto-Change Design (Phase 2)

> **Status: not implemented in Phase 1.** This document is the implementation contract for
> Phase 2.

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
├── targetMonitorIds: Set<String>   # empty = all
├── matchAcrossDisplays: Boolean
├── backendId: String
├── customCommand: String
└── lastError: String?
```

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

- Menu:
  - Show Rimalux
  - Change Wallpaper Now
  - Auto-Change: On/Off
  - Start at Login: On/Off
  - Quit
- Close button behavior:
  - If auto-change is enabled or `keepInTray` is on, hide the window.
  - Otherwise quit.
- If the tray is unavailable (e.g., GNOME without AppIndicator), keep the window visible or run
  hidden with autostart; the app can still be reopened from the app menu.

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
| `--test-backend` | Apply a diagnostic wallpaper and report backend details |
| `--list-monitors` | Print monitors and detected backend |
| `--diagnostics` | Print OS, locale, backend, paths, engine commit |
| `--version` | Print version |

## Optional OS scheduler mode

### Linux: systemd user timer

`~/.config/systemd/user/rimalux-rotate.service`:

```ini
[Unit]
Description=Rimalux wallpaper rotation

[Service]
Type=oneshot
Environment=DBUS_SESSION_BUS_ADDRESS=unix:path=%t/bus
ExecStart=/path/to/rimalux --rotate-now
```

`~/.config/systemd/user/rimalux-rotate.timer`:

```ini
[Unit]
Description=Rimalux wallpaper rotation timer

[Timer]
OnBootSec=5min
OnUnitActiveSec=60min
Persistent=true

[Install]
WantedBy=timers.target
```

Enable:

```bash
systemctl --user daemon-reload
systemctl --user enable --now rimalux-rotate.timer
```

Rewrite the timer when the interval changes. Disable when auto-change is turned off.

### Windows: Task Scheduler

```powershell
schtasks /Create /TN "Rimalux Wallpaper Rotation" /TR "\"C:\Path\To\Rimalux.exe\" --rotate-now" /SC MINUTE /MO 60 /F
```

Run only when the user is logged on. Recreate on interval change; delete on disable.

## Reliability and diagnostics

- Log every rotation: timestamp, set id, monitors, patterns, resolutions, backend, duration,
  success/failure.
- Show “Last change”, “Next change”, and “Last error” in the Auto-Change screen.
- Add a “Test backend” button.
- Add “Copy diagnostics”.
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
