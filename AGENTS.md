# AGENTS.md — Rimalux Desktop Handover Guide

Read this file completely before modifying, extending, or debugging Rimalux Desktop.
It defines the architecture, portability contracts, UI rules, platform rules, and verification
requirements that keep the desktop port aligned with the Android app.

---

## 1. Project identity

- **App name:** Rimalux
- **Desktop package root:** `com.cptodix.rimalux.desktop`
- **Engine package:** `com.cptodix.rimalux.oled.wallpaper.engine` (kept identical to Android for sync)
- **Repository:** `cptodix/rimalux-desktop`
- **Origin:** Android app `cptodix/rimalux`
- **License:** GPL-3.0
- **Current phase:** Phase 0 + Phase 1 complete (desktop MVP, verified on KDE Wayland)
- **Next phase:** Phase 2 (tray + auto-change)
- **Target platforms:** Linux (KDE Plasma verified, GNOME/XFCE supported) and Windows (Phase 3)

Rimalux generates procedural mathematical vector wallpapers with an exact `#000000` True Black
Ratio, 80 patterns, 41 curated palettes, gradient/solid fills, per-monitor wallpapers, PNG export,
and background auto-change.

---

## 2. Non-negotiable engine contracts

The `core/` module is a **synced copy** of the Android engine. Do not edit it by hand.

1. **Engine purity**
   - `core/src/main/kotlin/com/cptodix/rimalux/oled/wallpaper/engine/` must import only `kotlin.*`.
   - No Android, Compose, Java AWT, Skia, resource, or app-layer imports.
   - `CoreEnginePortabilityTest` enforces this. Keep it passing.

2. **Exact True Black calibration**
   - Every pattern must pass through `CoverageCalibrator.calibrate(...)`.
   - A primitive is drawn iff `primitive.threshold >= clampedBlackRatio`.
   - Measured `#000000` pixels must stay within `±7%` of target at `25%`, `65%`, `85%`,
     and increase monotonically across all 80 patterns in both `NORMAL` and `CENTER_WEIGHTED` modes.

3. **Non-black palette floor**
   - Starter and random palettes must never contain `#000000`.
   - `ColorHarmonyEngine.resolveArgbColor` clamps near-black non-black swatches to `(0,0,4)`.

4. **Intricacy window**
   - Random intricacy is sampled uniformly in `[0.40f, 0.60f]`.

5. **4096 px cap**
   - `DeepBlackRatioEngine.MAX_WALLPAPER_AXIS_PX = 4096`.
   - Higher monitor resolutions are scaled down proportionally.

6. **Rendering order and fill rules**
   - Preserve primitive order, viewport transform, non-zero polygon winding, even-odd annulus,
     round stroke caps/joins, micro-seam logic, and gradient endpoints.
   - The Skia renderer must match the Android renderer closely enough for the pixel test above.

---

## 3. Module boundaries

```text
core/          Pure Kotlin engine + portable engine models. No platform imports.
desktopApp/    Compose Desktop UI, Skia renderer, platform adapters, persistence, scheduler.
```

Rules:

- `core` must not depend on `desktopApp`.
- `desktopApp` may depend on `core`.
- Android-specific concepts (`WallpaperManager`, `DataStore`, `WorkManager`, `AlarmManager`,
  `MediaStore`, Play Billing, phone gestures) must not leak into `desktopApp`.
- Desktop models live in `com.cptodix.rimalux.desktop.model`.
- Platform code lives in `com.cptodix.rimalux.desktop.platform`.
- Rendering code lives in `com.cptodix.rimalux.desktop.render`.
- UI code lives in `com.cptodix.rimalux.desktop.ui`.

---

## 4. Engine sync rules

- Source of truth: `../rimalux/app/src/main/java/com/cptodix/rimalux/oled/wallpaper/engine/`
- Sync command: `./gradlew syncAll`
- Individual tasks: `syncEngine`, `syncStrings`, `syncPreviews`, `syncFonts`, `syncBrand`.
- Do not manually patch `core/` or the copied `strings.xml` files. Put desktop-only strings in
  `desktopApp/src/main/composeResources/values/desktop.xml` and `values-id/desktop.xml`.
- After syncing, run `./gradlew test` and the renderer calibration test.
- Never modify `../rimalux` from this repository.

See [`docs/ENGINE_SYNC.md`](docs/ENGINE_SYNC.md).

---

## 5. Desktop UI rules

1. **Desktop-native, not mobile**
   - No swipe deck, bottom sheet, or phone mockups.
   - Use a left navigation rail, top app bar, preview pane, and control panel.
   - Responsive: stack controls below the preview on narrow windows.

2. **OLED Material 3 theme**
   - Background/surface are pure `#000000`.
   - Reuse `ui/theme/Color.kt`, `Theme.kt`, and the shape scale.
   - Accent is derived from the active palette via `createDynamicColorScheme`.
   - Keep corner radii restrained (`6/8/12/14/16 dp`).

3. **Terminology**
   - Call procedural designs **Patterns**.
   - Call the palette page **Colors**.
   - Call `#000000` coverage **True Black Ratio**.
   - Use **Apply to Desktop** / **Apply to All Displays**, not Android Home/Lock targets.
   - Do not promise lock-screen wallpaper support on Windows.

4. **Interaction**
   - All interactive controls need keyboard focus and visible focus states.
   - Provide keyboard shortcuts: `Space` randomize, `←/→` history, `Ctrl+S` save,
     `Ctrl+Enter` apply, `F11` fullscreen preview (when implemented), `Esc` close.
   - Show progress for synthesis, apply, and export.
   - Use a snackbar/status line, not blocking dialogs for routine status.

5. **Localization**
   - English and Bahasa Indonesia are first-class.
   - All user-facing strings come from Compose Resources.
   - Dynamic pattern/palette titles use `Res.allStringResources` with a fallback to
     `ArtPattern.defaultTitle` / `ColorPalette.name`.
   - Keep copy in Title Case for titles/buttons and sentence case for descriptions.

6. **File modularity**
   - Keep files focused; aim for < 500 lines.
   - Split large screens into components/dialogs.

---

## 6. Platform backend rules

- All external commands use `ProcessBuilder` with argument lists. **Never** use `sh -c` or
  string interpolation for untrusted paths.
- Detect the desktop environment before choosing a backend.
- Provide a custom-command fallback with `{file}` and `{monitor}` placeholders.
- Per-monitor support is backend-dependent:
  - KDE Plasma: yes, via Plasma scripting (`qdbus6`/`qdbus`).
  - Windows: yes, via `IDesktopWallpaper` (Phase 3).
  - XFCE: yes, via `xfconf-query`.
  - Sway/Hyprland: best-effort via `swaybg`/`swww`/`hyprpaper`.
  - GNOME/Cinnamon/MATE/Budgie: global only; state this in the UI.
- Windows lock-screen wallpaper is not app-settable; do not add a fake control.
- Log command, exit code, and stderr. Never log secrets.
- Keep the currently applied wallpaper file in cache; some DEs reference it.

See [`docs/LINUX_WALLPAPER_BACKENDS.md`](docs/LINUX_WALLPAPER_BACKENDS.md) and
[`docs/WINDOWS_WALLPAPER_BACKENDS.md`](docs/WINDOWS_WALLPAPER_BACKENDS.md).

---

## 7. Auto-change rules (Phase 2)

- Auto-change runs in a tray-resident app, or via an optional OS scheduler.
- `nextRunEpochMillis` and `lastRunEpochMillis` are persisted in settings.
- Enforce a dedupe window of `max(30s, interval * 3/4)` so startup + timer + wake do not double-rotate.
- On startup/wake, if `nextRun` is overdue, rotate once and then schedule the next interval.
- If a rotation fails, keep the old wallpaper, log the error, and retry on the next interval.
- Start at Login:
  - Linux: `~/.config/autostart/rimalux.desktop`
  - Windows: `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
- Single instance: second launch focuses the existing window.
- Quit warns if auto-change is enabled.
- `--background` starts hidden; `--rotate-now` performs one headless rotation.

See [`docs/AUTO_CHANGE.md`](docs/AUTO_CHANGE.md).

---

## 8. Build and verification

Use the Gradle wrapper:

```bash
./gradlew test
./gradlew :desktopApp:run
RIMALUX_PACKAGING_JDK=/path/to/full-jdk-21 ./gradlew :desktopApp:packageRpm
RIMALUX_PACKAGING_JDK=/path/to/full-jdk-21 ./gradlew :desktopApp:packageDeb
```

- JDK 17+ is required. The project targets JVM 17.
- Native packaging needs a full JDK with `jmods`; the openSUSE runtime JDK may lack them.
  Use `RIMALUX_PACKAGING_JDK` to point at Temurin 21 or another full JDK.
- Windows packaging must run on Windows (jpackage limitation); use the CI matrix.
- Do not run two Gradle builds concurrently on this machine.
- Do not commit generated `build/`, `.gradle/`, or cache files.
- After UI changes, launch the app and inspect the window; after renderer changes, run
  `SkiaRendererCalibrationTest`.

Verification checklist:

1. `./gradlew test` passes.
2. `CoreEnginePortabilityTest` passes.
3. `SkiaRendererCalibrationTest` passes (all 80 patterns, both fill modes, both weight modes).
4. `SettingsStoreTest`, `WallpaperFileStoreTest`, `WallpaperBackendTest` pass.
5. `./gradlew :desktopApp:run` launches the window on KDE Wayland.
6. Preview, pattern selection, palette selection, true-black slider, apply, and PNG export work.
7. Settings survive restart.
8. RPM packaging works with `RIMALUX_PACKAGING_JDK` (verified on this machine).
9. Docs updated for any behavior change.

---

## 9. Documentation maintenance

Update these documents when behavior changes:

- `README.md` — user-facing status/features/quick start.
- `docs/ROADMAP.md` and `docs/IMPLEMENTATION_PLAN.md` — phase status.
- `docs/ARCHITECTURE.md` — modules/data flow.
- `docs/ENGINE_SYNC.md` — sync procedure.
- `docs/DESKTOP_UX.md` — UI behavior.
- `docs/AUTO_CHANGE.md` — scheduler behavior.
- `docs/LINUX_WALLPAPER_BACKENDS.md` / `docs/WINDOWS_WALLPAPER_BACKENDS.md` — platform commands.
- `docs/TESTING.md` — test coverage and manual checks.
- `docs/DECISIONS.md` — new architecture decisions.

Keep docs factual. Mark planned features clearly as **Phase 2/3**, not as implemented.
