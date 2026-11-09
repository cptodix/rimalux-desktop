# Testing

> **Read [`../RULES.md`](../RULES.md) first.** Do not take repeated screenshots, do not run the
> GUI app in the foreground without the user present, and ask before any UI verification.
> Prefer automated/headless tests, logs, JSON state, and temporary directories.

## Automated tests

Run all:

```bash
./gradlew test
```

### `core`

- **`CoreEnginePortabilityTest`**
  - Scans engine sources for forbidden imports.
  - Verifies the engine has at least 17 Kotlin files.
  - Synthesizes all 80 patterns in both weight modes.
  - Checks distinct seeds, palettes, and gradient math.

- The all-80 synthesis, center-weighted margin, color safeguard, and 4K/5K dimension-clamping
  checks are covered by `CoreEnginePortabilityTest`.

### `desktopApp`

- **`SkiaRendererCalibrationTest`**
  - Renders all 80 patterns at 420×840 with solid and gradient fills.
  - Targets 25/65/85% true black.
  - Asserts measured black is within ±7 percentage points and monotonic.
  - Verifies both `NORMAL` and `CENTER_WEIGHTED` modes.
  - This is the main guard against renderer drift.

- **`SettingsStoreTest`**
  - JSON round-trip persistence, corrupt-file backup, default recovery, language/screen state.

- **`WallpaperFileStoreTest`**
  - Renders a full-resolution set, writes cache PNGs, copies to Pictures, and checks measured black.

- **`WallpaperBackendTest`**
  - KDE Plasma script generation, URI escaping, screen mapping, custom-command tokenizer.

- **`StudioControllerPrefetchTest`**
  - Boots the controller against a temporary config/cache directory, waits for the initial set,
    and verifies the next-wallpaper buffer fills (1–3 sets) without a GUI.

- **`AutoChangePolicyTest`**
  - Interval clamping, dedupe windows, due detection, next-run calculations.

- **`AutostartManagerTest`**
  - Linux XDG autostart entry creation/removal and content.

- **`SingleInstanceManagerTest`**
  - First instance acquires the lock; second instance fails and notifies the first.

- **`StatusNotifierTrayTest`**
  - On a session bus, installs the SNI tray, registers it with
    `org.kde.StatusNotifierWatcher`, and verifies that `Activate` invokes the open callback.
  - Verifies `org.freedesktop.DBus.Properties` exposes `IconPixmap`/`Menu`, that
    `com.canonical.dbusmenu.GetLayout` returns the four native menu entries, and that menu
    `clicked` events drive open and the auto-change toggle.

- **`StudioControllerAutoChangeTest`**
  - Fake clock + fake backend; due rotation generates and applies a new set and advances
    last/next run timestamps.

- **`WindowsWallpaperBackendTest`**
  - Fit-mode → `DESKTOP_WALLPAPER_POSITION` mapping, monitor index clamping, availability flag.

- **`UpdateCheckerTest`**
  - Dotted version comparison and pre-release handling.

- **`OsSchedulerTest`**
  - systemd service/timer content and interval clamping.

- **`DiagnosticsBundleTest`**
  - ZIP contains system/settings/monitor/backend reports and redacts custom commands.

## Manual Linux checklist

Baseline Phase 0/1 checks were verified on KDE Plasma Wayland. The locked full-bleed UI was
accepted by the user on 2026-11-07.

1. [x] Baseline app launched and rendered a wallpaper.
2. [x] Baseline patterns/colors/settings content worked.
3. [x] True-black single/range controls worked.
4. [x] KDE apply wallpaper worked (90% true black status).
5. [x] PNG export covered by `WallpaperFileStoreTest`.
6. [x] Settings and language persisted across restart.
7. [x] RPM package built and packaged app launched.
8. [x] Locked full-bleed layout: wallpaper fills window.
9. [x] Bottom controls: previous / apply / save / next.
10. [x] Next buffer: first press instant, buffer refills, empty-buffer fallback shows progress.
11. [x] Info pill: pattern, palette dots, true-black percentage.
12. [x] Options modal matches Android sheet layout.
13. [x] Patterns/Colors pages expand to the right in the modal.
14. [x] Quick Pattern Layout / Color Fill popups.
15. [x] Desktop Settings opens from the options list row.
16. [x] Narrow window layout remains usable.
17. [x] High-resolution 1920 px preview.
18. [ ] Multi-monitor apply on real hardware (not available).
19. [x] Windows backend implemented; hardware verification pending.

### Phase 2 manual checklist (verified)

1. [x] Enable Auto-Change; verify next-run appears.
2. [x] Change interval; verify next-run updates.
3. [ ] Set Apply To to specific displays; verify only those change (single display available).
4. [ ] Start at Login creates `~/.config/autostart/rimalux.desktop` (installed build).
5. [x] Close window with Auto-Change on; verify it hides to tray and keeps running.
6. [x] AWT tray left-click/right-click behavior (Windows/fallback).
6b. [ ] Native SNI tray left-click/right-click on KDE Wayland.
7. [ ] Run `--rotate-now`; verify one wallpaper change.
8. [ ] Launch a second instance; verify it focuses the first.
9. [x] Wait for one interval; verify automatic change.
10. [ ] Sleep/restart catch-up behavior.

### Phase 3/4 manual checklist

1. [ ] Fullscreen preview via F11 / double-click, exit with Esc (implemented).
2. [ ] Options modal floats above the media controls, animates from the bottom, has no
       title/close/back row, and the left sheet keeps its width when a page opens or closes.
3. [ ] Apply modal opens above the apply button, grows upward, has no cancel row, and lists
       displays, All Displays, All Displays Separate, then a divider and Save Wallpaper.
3a. [ ] Apply button is circular with a thick checkmark and no separate save button in the pill.
3d. [ ] Popups use a shadow, not a full-window dark scrim; clicking outside dismisses them.
3b. [ ] Page rows show a left chevron while open; quick-choice rows keep a right chevron.
3c. [ ] Choice and Auto-Change popups are horizontally centered on their triggering row.
4. [ ] Preview stays display-agnostic; no display selector/layout icons.
5. [ ] Advanced Settings: Appearance card uses popup rows for Language, Resolution (`W×H`), and
       Fit Mode; folder grid is usable; buttons/cards match the Android shape/hierarchy rules.
6. [ ] Auto Match uses native resolutions; 4K fallback when detection fails.
7. [ ] Auto-Change Status section shows next/last/Change Now and whether rotation is active via
       the app, tray, or OS scheduler.
2. [ ] Display Identify overlay from Display Layout (implemented).
3. [x] Diagnostics bundle export and redaction (`DiagnosticsBundleTest`).
4. [x] Update-check version comparison (`UpdateCheckerTest`).
5. [x] systemd unit content (`OsSchedulerTest`).
6. [ ] Install/remove the systemd user timer on a real session.
7. [ ] Windows `--test-backend` on Windows hardware.
8. [ ] Windows MSI/EXE install and per-monitor apply.

## Manual GNOME checklist

1. Backend detects GNOME.
2. Apply changes the wallpaper.
3. UI states that GNOME uses one wallpaper for all displays.

## Manual XFCE checklist

1. Backend detects XFCE.
2. Apply changes the wallpaper.
3. Per-monitor properties update for each monitor/workspace.

## Manual Windows checklist (Phase 3)

1. Install MSI per-user.
2. Launch from Start menu.
3. Apply wallpaper; verify fill and true black.
4. Restart; settings restored.
5. Auto-change rotates after the interval.
6. Start at Login creates HKCU Run entry; sign out/in starts hidden.
7. Multi-monitor per-monitor apply (if available).
8. Uninstall; verify no leftover autostart entry (or document).
9. `rimalux --test-backend` report matches expectations.

## CI

- Linux: `./gradlew test` + `packageDeb` + `packageRpm`.
- Windows: `./gradlew.bat test` + `packageMsi` + `packageExe` + `--test-backend`.
- UI tests are optional; a launch smoke test under Xvfb can be added later.

## True-black acceptance

For each pattern, fill mode, and weight mode:

- Target 25% → measured 18–32%
- Target 65% → measured 58–72%
- Target 85% → measured 78–92%
- Measured values must increase monotonically with target.

If a pattern fails, check:

- surface cleared to opaque black
- viewport transform
- primitive order/visibility
- polygon winding
- annulus even-odd fill
- polyline stroke width/cap/join
- micro-seam threshold
- gradient endpoints
- anti-aliasing differences between Android Canvas and Skia
