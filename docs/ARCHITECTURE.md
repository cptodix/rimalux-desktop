# Architecture

## Module graph

```text
┌────────────────────────────────────────────────────────────────────┐
│ core/  (pure Kotlin, no platform imports)                          │
│                                                                    │
│  engine/  synced from ../rimalux                                   │
│  • WallpaperGeneratorEngine   • MathPatternGenerator + 80 patterns │
│  • CoverageCalibrator         • DeepBlackRatioEngine               │
│  • ColorHarmonyEngine         • WatermarkPlacementSolver           │
│  • WallpaperCoreModels        • VectorPrimitives                   │
└────────────────────────────────────▲───────────────────────────────┘
                                     │
┌────────────────────────────────────┴───────────────────────────────┐
│ desktopApp/  (Kotlin/JVM + Compose Desktop)                        │
│                                                                    │
│  model/      Desktop settings, monitor info, wallpaper sets        │
│  render/     Skia rasterizer, PNG encoder, black-ratio measurement │
│  platform/   Monitor detection, wallpaper backends, paths, export  │
│  data/       JSON settings store, atomic persistence               │
│  controller/ Studio state, synthesis jobs, apply/export actions    │
│  ui/         Theme, screens, components, dialogs                   │
│  scheduler/  Phase 2: auto-change scheduler                        │
└────────────────────────────────────────────────────────────────────┘
```

## Dependency direction

- `core` depends only on Kotlin stdlib.
- `desktopApp` depends on `core` + Compose Desktop + Skia (via Compose) + coroutines +
  kotlinx.serialization + JNA (Windows, Phase 3).
- No Android or Jetpack Compose Android artifacts are used.

## Runtime data flow

```text
SettingsStore (JSON)
      │
      ▼
StudioController ──► WallpaperGeneratorEngine.synthesize(...)
      │                       │
      │                       ▼
      │              SynthesizedWallpaper (VectorScene)
      │                       │
      │          ┌────────────┴────────────┐
      │          ▼                         ▼
      │  Skia preview bitmap       Skia full-res PNG
      │          │                         │
      │          ▼                         ▼
      │      Compose UI            WallpaperBackend / Pictures
      │
      ▼
WallpaperSet metadata + history + settings JSON
```

## Key classes

### `core`
- `WallpaperGeneratorEngine` — synthesis facade.
- `MathPatternGenerator` — seeded pattern dispatch.
- `CoverageCalibrator` — exact true-black calibration.
- `DeepBlackRatioEngine` — ratio resolution, visibility, viewport transform, pixel sampling.
- `ColorHarmonyEngine` — palette selection, HSV shading floor, gradient endpoints.
- `WallpaperCoreModels` — enums, `CoreGenerationSpec`, `SynthesizedWallpaper`, palettes.
- `VectorPrimitives` — `VectorScene`, polygons, arc sectors, polyline ribbons.

### `desktopApp.model`
- `GenerationConfig` — portable generation settings + `toCoreSpec()`.
- `MonitorInfo` — id, name, bounds, physical size, scale, primary flag.
- `WallpaperItem` — one monitor's wallpaper parameters.
- `WallpaperSet` — all monitor items generated together.
- `DesktopSettings` — generation + display + UI + auto-change + history + palettes.

### `desktopApp.render`
- `SkiaWallpaperRenderer` — rasterizes `VectorScene` to `org.jetbrains.skia.Image`, measures true
  black via Compose `PixelMap`, and encodes lossless PNGs.

### `desktopApp.platform`
- `PlatformPaths` — XDG/AppData config, cache, pictures, logs.
- `MonitorProvider` / `AwtMonitorProvider` / `KdeMonitorProvider` — display discovery.
- `WallpaperBackend` — apply abstraction.
- `KdeWallpaperBackend` — Plasma scripting per-monitor + global fallback.
- `GnomeWallpaperBackend` — `gsettings` global apply.
- `CustomCommandBackend` — `{file}`/`{monitor}` template.
- `WallpaperFileStore` — renders cache PNGs and saves lossless PNGs to Pictures.
- `ProcessRunner` — safe command execution with timeout and logs.

### `desktopApp.data`
- `SettingsStore` — `StateFlow<DesktopSettings>`, JSON load/save, atomic writes.
- `SettingsDto` — serializable DTOs (engine enums stored as string IDs).

### `desktopApp.controller`
- `StudioController` — UI state, synthesis jobs, monitor refresh, apply/export, prefetched
  next-wallpaper buffer (target 3), and previous rendered-set cache.
- `StudioUiState` — current rendered set, selected monitor, config, palettes, history,
  buffer count, status, backend info.

### `desktopApp.ui`
- `RimaluxDesktopApp` — full-window shell, options state, status snackbar.
- `StudioScreen` — full-bleed wallpaper, info pill, media-player controls, display selector.
- `OptionsModal` — Android-sheet-style left column that expands right into Patterns / Colors /
  Settings panes.
- `PatternsScreen`, `ColorsScreen`, `SettingsScreen` — used as the expanded right panes.
- Components: pattern/palette cards, true-black controls, display layout, color picker, dialogs.
- Theme: OLED Material 3, dynamic accent from active palette.

## Threading

- UI state is a `StateFlow` updated from background coroutines; Compose collects on Main.
- Synthesis and Skia rendering run on `Dispatchers.Default`.
- File writes and wallpaper commands run on `Dispatchers.IO`.
- A `Mutex` serializes synthesis and apply operations.
- Slider changes are debounced (~150 ms) before regeneration.
- A background prefetch job fills up to 3 upcoming rendered sets. Interactive generation cancels
  and clears the buffer; consuming a buffered set refills it without blocking the UI.
- Apply/export are cancellable but never run concurrently with a rotation.

## Rendering contract

Given a `SynthesizedWallpaper` and target `(widthPx, heightPx)`:

1. Clamp dimensions with `DeepBlackRatioEngine.clampDimensionsToMaxAxis`.
2. Clear the surface to opaque `#000000`.
3. Compute `DeepBlackRatioEngine.computeViewportTransform`.
4. Iterate `scene.primitives` in order; skip invisible primitives.
5. Fill with solid ARGB or a linear gradient from `ColorHarmonyEngine.computeLinearGradient`.
6. Polygon: non-zero winding; optional black micro-seam stroke.
7. Arc sector: circle / pie / even-odd annulus / annular sector.
8. Polyline: round cap/join stroke, width `max(norm * scale, 1.3)`.
9. Measure true black via `DeepBlackRatioEngine.measureTrueBlackPercentage`.

This mirrors the Android `WallpaperRenderer` and the cross-platform porting spec.

## Persistence

Settings file:

- Linux: `${XDG_CONFIG_HOME:-~/.config}/rimalux/settings.json`
- Windows: `%APPDATA%\Rimalux\settings.json`

Cache:

- Linux: `${XDG_CACHE_HOME:-~/.cache}/rimalux/`
- Windows: `%LOCALAPPDATA%\Rimalux\cache\`

Exports:

- Linux: `xdg-user-dir PICTURES` (fallback `~/Pictures`)/Rimalux
- Windows: `%USERPROFILE%\Pictures\Rimalux`

Writes are atomic (temp file + `ATOMIC_MOVE`). Corrupt settings are backed up and replaced with
defaults. History stores metadata only; previews are regenerated from seeds.

## UI navigation

- Studio
- Patterns
- Colors
- Settings

Auto-Change is added as a fifth destination in Phase 2.

## Localization

Compose Resources:

- `desktopApp/src/main/composeResources/values/strings.xml` (synced Android English)
- `values-id/strings.xml` (synced Android Indonesian)
- `values/desktop.xml` + `values-id/desktop.xml` (desktop-only keys)
- `drawable/pattern_preview_*.webp` (synced)
- `font/plus_jakarta_sans.ttf` (synced)

Runtime language switching uses `Locale.setDefault(...)` and a `key(languageTag)` wrapper so
Compose Resources re-resolves the environment.

## Platform support

| Capability | Linux | Windows |
|---|---|---|
| Monitor detection | AWT + KDE `kscreen-doctor` | AWT; Win32 in Phase 3 |
| Global wallpaper | KDE, GNOME, XFCE, custom | Phase 3 |
| Per-monitor wallpaper | KDE, XFCE, Sway/Hyprland best-effort | Phase 3 |
| Autostart | XDG `.desktop` (Phase 2) | HKCU Run (Phase 2/3) |
| Tray | Phase 2 | Phase 2/3 |
| Lock screen | Not promised | Not supported by Windows |

See [`LINUX_WALLPAPER_BACKENDS.md`](LINUX_WALLPAPER_BACKENDS.md) and
[`WINDOWS_WALLPAPER_BACKENDS.md`](WINDOWS_WALLPAPER_BACKENDS.md).
