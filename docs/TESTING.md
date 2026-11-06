# Testing

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

- **`AutostartTest`** (Phase 2, planned)
  - `.desktop` content and Windows registry value formatting.

- **`AutoChangeSchedulerTest`** (Phase 2, planned)
  - Fake clock + fake backend; schedule, dedupe, catch-up, interval change, failure.

## Manual Linux checklist

On KDE Plasma Wayland (verified 2026-11-07 unless noted):

1. [x] `./gradlew :desktopApp:run` opens the window.
2. [x] Preview renders a wallpaper with no visual corruption.
3. [x] Randomize changes the wallpaper.
4. [x] Pattern gallery shows all 80 localized cards.
5. [x] Palette gallery shows 41 palettes and swatches.
6. [x] True-black slider updates the preview and measured percentage.
7. [x] Single/range modes are present.
8. [x] Normal/center-weighted controls are present.
9. [x] Solid/gradient controls are present.
10. [x] Display selector shows eDP-1 at 1920×1080.
11. [x] Apply wallpaper changes the desktop (status: 90% true black).
12. [x] PNG export covered by `WallpaperFileStoreTest` (UI save not manually clicked).
13. [x] Restart restores settings and current wallpaper preview.
14. [x] Language selector switches EN/ID and persists.
15. [x] Backend status shows KDE Plasma / per-display supported.
16. [x] No Compose runtime error dialog after the scroll fix.
17. [ ] Multi-monitor apply on real hardware (not available).
18. [ ] Windows backend (Phase 3).

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
