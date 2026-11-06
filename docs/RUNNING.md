# Running Rimalux Desktop

## Quick start

```bash
cd /home/cptodix/WorkingFolder/Programming/rimalux-desktop
./gradlew :desktopApp:run
```

The first build downloads Gradle dependencies and may take a few minutes.

## Requirements

- JDK 17+ (`JAVA_HOME` should point to JDK 17 or newer).
- Linux desktop session (KDE Plasma verified; GNOME/XFCE supported).
- For wallpaper apply:
  - KDE: `plasma-apply-wallpaperimage`, `qdbus6` or `qdbus`
  - GNOME: `gsettings`
  - XFCE: `xfconf-query`

## Current environment (verified)

- openSUSE Tumbleweed
- KDE Plasma 6.7.5, Wayland
- JDK 17 at `/usr/lib64/jvm/java-17-openjdk-17`
- Display: 1920×1080 eDP-1
- Verified on 2026-11-07: app launch, Studio/Patterns/Colors/Settings screens, language switch,
  settings restart, and KDE wallpaper apply. See [`VERIFICATION.md`](VERIFICATION.md).

## First run

1. The app generates a random wallpaper and shows the Studio screen.
2. Pick a pattern in **Patterns**.
3. Pick colors in **Colors**.
4. Adjust **True Black Ratio**.
5. Click **Apply to All Displays** or **Apply to This Display**.
6. Click **Save PNG** to write to `~/Pictures/Rimalux`.

The first launch may open on the last screen from a previous run (persisted in settings).
Use the left navigation rail to switch screens.

### Restore your previous wallpaper

Applying a wallpaper changes your live KDE desktop. To restore the previous Plasma wallpaper,
use **System Settings → Appearance → Wallpaper**, or run:

```bash
plasma-apply-wallpaperimage /usr/share/wallpapers/openSUSEdefault/contents/images/default.png
```

### If the window opens behind other windows

Use the taskbar or:

```bash
wmctrl -a Rimalux
```

Settings are stored at:

```text
~/.config/rimalux/settings.json
```

Cache:

```text
~/.cache/rimalux/
```

## CLI diagnostics

```bash
# List monitors and detected backend
./gradlew :desktopApp:run --args="--list-monitors"

# Print diagnostics
./gradlew :desktopApp:run --args="--diagnostics"

# Headless render test (writes PNGs)
./gradlew :desktopApp:run --args="--render-test /tmp/rimalux-render-test"
```

## Run from an installed package

```bash
sudo zypper install ./desktopApp/build/compose/binaries/main/rpm/*.rpm
rimalux
```

or:

```bash
sudo apt install ./desktopApp/build/compose/binaries/main/deb/*.deb
rimalux
```

## Troubleshooting

### Window does not open

- Check `JAVA_HOME` and `java -version`.
- Try running from a terminal to see logs.
- On Wayland, try `SKIKO_RENDER_API=SOFTWARE` as a fallback.
- Ensure `DISPLAY`/`WAYLAND_DISPLAY` are set.

### Wallpaper does not change

- Check the backend shown in Settings.
- On KDE, verify `qdbus6 org.kde.plasmashell` works.
- On GNOME, verify `gsettings` works.
- Check `~/.local/state/rimalux/logs/` (or the log path shown in Settings).
- Use **Save PNG** and set the wallpaper manually to isolate the issue.

### Only one monitor changes

- KDE: per-monitor mapping uses `desktop.screen`; check the Display Layout mapping.
- GNOME: one wallpaper for all displays is a GNOME limitation.
- XFCE: verify `xfconf-query -c xfce4-desktop -l`.

### Fonts or text look wrong

- Ensure `plus_jakarta_sans.ttf` was synced:
  `./gradlew syncFonts`.
- Rebuild: `./gradlew :desktopApp:run`.

### Settings are corrupted

The app backs up a corrupt settings file and recreates defaults. The backup is next to
`settings.json` with a `.bak` suffix.

### Wallpaper applied but I want the original back

Rimalux does not store your previous desktop wallpaper. Restore it from KDE System Settings or
re-apply a known image with `plasma-apply-wallpaperimage`.

### Reset settings

Delete or rename:

```text
~/.config/rimalux/settings.json
```

## Development commands

```bash
./gradlew test
./gradlew :desktopApp:run
./gradlew :desktopApp:packageDeb
./gradlew :desktopApp:packageRpm
./gradlew syncAll
```

## Phase status

- Phase 0 + 1: implemented and verified on KDE Wayland (this document).
- Phase 2: tray + auto-change (next).
- Phase 3: Windows backend + installers.
- Phase 4: release polish.

See [`VERIFICATION.md`](VERIFICATION.md) for the exact test log.
