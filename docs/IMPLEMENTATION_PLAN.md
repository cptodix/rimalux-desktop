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
- [x] App shell / navigation rail.
- [x] Studio screen.
- [x] Preview pane + monitor selector + all-display strip.
- [x] True-black slider/range.
- [x] Pattern/color summary cards.
- [x] Display layout dialog + match-design switch (auto-change targets in Phase 2).
- [x] History strip.
- [x] Patterns screen (search, categories, grid, selection).
- [x] Colors screen (fill/palette modes, grid, custom palette).
- [x] Settings screen (language, resolution, backend, folders, about).
- [x] Keyboard shortcuts.
- [x] Status snackbar.
- [x] EN/ID desktop strings.

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
- [x] RPM package builds with a full JDK (`RIMALUX_PACKAGING_JDK`).
- [x] Packaged RPM launches.
- [ ] DEB package (needs `dpkg-deb` on the build host).
- [ ] Multi-monitor hardware test (machine has one display).
- [ ] Windows backend test (Phase 3).

## Phase 2 — Auto-change

- [ ] `AutoChangeScheduler`
- [ ] Tray + menu
- [ ] Start at Login (Linux)
- [ ] Single instance
- [ ] Auto-Change screen
- [ ] Notifications/logs
- [ ] Scheduler tests
- [ ] Manual 1-minute rotation test

## Phase 3 — Windows

- [ ] Win32 global backend
- [ ] IDesktopWallpaper COM backend
- [ ] Windows monitor provider
- [ ] HKCU autostart
- [ ] Windows tray
- [ ] `--test-backend`
- [ ] CI MSI/EXE
- [ ] Human verification checklist

## Phase 4 — Release

- [ ] Fullscreen preview
- [ ] Identify/reorder displays
- [ ] Diagnostics bundle
- [ ] systemd/Task Scheduler mode
- [ ] Update checker
- [ ] Signing
- [ ] Release workflow

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
