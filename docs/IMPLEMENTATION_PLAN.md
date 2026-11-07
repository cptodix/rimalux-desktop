# Implementation Plan (Agentic Checklist)

This is the working checklist for AI agents and contributors. Keep statuses current.

## Phase 0 — Foundation

- [x] Write README, AGENTS, context, architecture, porting, engine-sync, knowledge, UX, auto-change,
      Linux/Windows backend, build, testing, running, roadmap, decisions docs.
- [x] Create Gradle wrapper and version catalog.
- [x] Create `core` module.
- [x] Add `syncEngine`, `syncStrings`, `syncPreviews`, `syncFonts`, `syncBrand`, `syncAll` tasks.
- [x] Copy engine into `core`.
- [x] Port `CoreEnginePortabilityTest`.
- [x] Port `CoreEnginePortabilityTest` (includes all-80 synthesis and center-weighted checks).
- [x] Create `desktopApp` module.
- [x] Add Compose Desktop, Material 3, resources, coroutines, serialization, JNA dependencies.
- [x] Copy theme tokens/fonts/strings/previews/icon.
- [x] Implement `SkiaWallpaperRenderer`.
- [x] Implement true-black measurement.
- [x] Implement `SkiaRendererCalibrationTest`.
- [x] Implement AWT + KDE monitor detection.
- [x] Implement `--render-test` and `--list-monitors`.
- [x] Launch a Compose window.

## Phase 1 — Desktop MVP

### Models and persistence
- [x] `GenerationConfig` + `toCoreSpec()`.
- [x] `MonitorInfo`, `WallpaperItem`, `WallpaperSet`.
- [x] `DesktopSettings`, display/UI/auto-change settings.
- [x] Serializable DTOs with string enum IDs.
- [x] Atomic `SettingsStore` with corrupt-file backup.
- [x] Default settings and first-run generation.

### Controller
- [x] `StudioController` state and synthesis jobs.
- [x] Randomize / previous / next.
- [x] Pattern/palette/ratio/layout/fill updates.
- [x] Debounced regeneration.
- [x] Per-monitor set generation (match or unique).
- [x] Preview rendering (selected + all displays).
- [x] Apply to all/selected display.
- [x] Save PNG(s).
- [x] History sets.
- [x] Monitor refresh.
- [x] Language switching.
- [x] Status messages.

### Platform
- [x] `PlatformPaths` (XDG).
- [x] `ProcessRunner`.
- [x] `WallpaperBackend` abstraction.
- [x] `KdeWallpaperBackend` (Plasma script + global fallback).
- [x] `GnomeWallpaperBackend`.
- [x] `CustomCommandBackend`.
- [x] `WallpaperFileStore` (cache render + Pictures export).
- [x] Cache cleanup policy.

### UI
- [x] OLED theme and typography.
- [x] Full-bleed wallpaper layout (no menu bar, no side rail).
- [x] Bottom media-player controls: options, previous, apply, save, next, display selector.
- [x] Info pill: pattern, palette dots, true-black percentage.
- [x] Prefetched next-wallpaper buffer (target 3) + previous rendered cache.
- [x] Options modal styled after the Android sheet.
- [x] Modal expands right for Patterns / Colors / Settings.
- [x] Desktop Settings row above “Enjoying Rimalux?”; no duplicate language selector in the modal.
- [x] Quick Pattern Layout / Color Fill dialogs.
- [x] Display layout dialog + match-design switch (auto-change targets in Phase 2).
- [x] Patterns pane with Android parity (random/custom modes, category filters, select all/none, current-pattern highlight, no search).
- [x] Colors pane with Android parity (random/custom modes, add/remove palette, select all/none, no fill/shuffle controls).
- [x] Settings pane (language, resolution, backend, folders, about).
- [x] 1920 px long-edge preview rendering.
- [x] Keyboard shortcuts.
- [x] Status snackbar.
- [x] EN/ID desktop strings.
- [x] User-guided visual verification of the locked layout (accepted 2026-11-07).

### Verification
- [x] `./gradlew test` passes.
- [x] Headless render proof passes.
- [x] App launches on KDE Wayland.
- [x] Manual apply test on KDE (status confirmed: applied at 90% true black).
- [x] Automated PNG export test (`WallpaperFileStoreTest`).
- [x] Manual settings restart test.
- [x] Manual language switch test (EN ↔ ID persisted).
- [x] Settings persistence test (`SettingsStoreTest`).
- [x] KDE script/custom command tests (`WallpaperBackendTest`).
- [x] Prefetch buffer test (`StudioControllerPrefetchTest`).
- [x] RPM package builds with a full JDK (`RIMALUX_PACKAGING_JDK`).
- [x] Packaged RPM launches.
- [ ] DEB package (needs `dpkg-deb` on the build host).
- [ ] Multi-monitor hardware test (machine has one display).
- [ ] Windows backend test (Phase 3).

## Phase 2 — Auto-change ✅

- [x] `AutoChangePolicy` + controller scheduler loop
- [x] Persisted next/last run, dedupe, catch-up
- [x] Tray + menu
- [x] Start at Login (Linux + Windows)
- [x] Single instance
- [x] Auto-Change section in the options modal
- [x] Target displays, interval picker, Change Now
- [x] `--background` / `--rotate-now`
- [x] Logs + last error
- [x] Scheduler/policy/autostart/single-instance/auto-rotation tests
- [x] Manual tray + real auto-change verification
- [ ] Desktop notifications (optional follow-up)

## Phase 3 — Windows ✅ (implemented, hardware verification pending)

- [x] Win32 global backend (`SystemParametersInfoW`)
- [x] IDesktopWallpaper COM backend
- [x] Windows monitor enumeration (AWT + COM device paths)
- [x] HKCU autostart
- [x] Windows tray (AWT `SystemTray`)
- [x] `--test-backend`
- [x] CI MSI/EXE
- [ ] Human verification checklist

## Phase 4 — Release ✅ (implemented)

- [x] Fullscreen preview (F11 / double-click)
- [x] Identify displays
- [x] Diagnostics bundle
- [x] systemd/Task Scheduler mode
- [x] Update checker
- [ ] Signing (needs release certificate/secrets)
- [x] Release workflow

## File map

```text
core/
  src/main/kotlin/com/cptodix/rimalux/oled/wallpaper/engine/   # synced
  src/test/kotlin/...                                         # portability + synthesis

desktopApp/
  src/main/kotlin/com/cptodix/rimalux/desktop/
    Main.kt
    model/          GenerationConfig, MonitorInfo, WallpaperSet, DesktopSettings
    data/           SettingsStore, SettingsDto
    render/         SkiaWallpaperRenderer (raster + PNG + measurement)
    platform/       PlatformPaths, ProcessRunner, MonitorProvider,
                    WallpaperBackend, KdeWallpaperBackend, GnomeWallpaperBackend,
                    CustomCommandBackend, WallpaperFileStore
    controller/     StudioController, StudioUiState
    ui/             theme/, screens/, components/, dialogs/
  src/main/composeResources/
    values/, values-id/, drawable/, font/
  src/test/kotlin/...  # renderer, settings, backend tests
```

## Rules for agents

1. Read `AGENTS.md` first.
2. Do not modify `../rimalux`.
3. Do not edit synced engine/strings files; change the Android source and run `syncAll`.
4. Keep engine invariants and tests green.
5. Keep files focused (< 500 lines).
6. Update docs when behavior changes.
7. Run `./gradlew test` before claiming completion.
8. Launch the app and verify UI after UI changes.
