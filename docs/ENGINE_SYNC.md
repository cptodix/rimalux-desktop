# Engine Sync Contract

## Source of truth

The Android repository is the source of truth for the wallpaper engine:

```text
../rimalux/app/src/main/java/com/cptodix/rimalux/oled/wallpaper/engine/
```

The desktop copy lives at:

```text
core/src/main/kotlin/com/cptodix/rimalux/oled/wallpaper/engine/
```

The package name is intentionally kept identical so the engine files require zero edits.

## Sync commands

```bash
# Everything
./gradlew syncAll

# Individual assets
./gradlew syncEngine
./gradlew syncStrings
./gradlew syncPreviews
./gradlew syncFonts
./gradlew syncBrand
```

Optional override if the Android checkout is elsewhere:

```bash
./gradlew syncAll -PandroidProjectDir=/path/to/rimalux
```

The default is `../rimalux` relative to this repository.

## What is copied

| Task | Source | Destination |
|---|---|---|
| `syncEngine` | `app/.../engine/*.kt` | `core/src/main/kotlin/.../engine/` |
| `syncStrings` | `res/values/strings.xml` | `composeResources/values/strings.xml` |
| `syncStrings` | `res/values-in/strings.xml` | `composeResources/values-id/strings.xml` |
| `syncPreviews` | `res/drawable-nodpi/*.webp` | `composeResources/drawable/` |
| `syncFonts` | `res/font/plus_jakarta_sans.ttf` | `composeResources/font/` |
| `syncBrand` | launcher PNG | `composeResources/drawable/rimalux_icon.png` |

## Do not edit synced files

- `core/.../engine/**` — change the Android engine, then sync.
- `composeResources/values/strings.xml` and `values-id/strings.xml` — change Android strings,
  then sync.
- Desktop-only strings go in `values/desktop.xml` and `values-id/desktop.xml`.
- Desktop-only drawables/fonts are fine in their respective Compose Resources folders.

## Engine invariants

These must remain true after every sync:

1. Engine imports only `kotlin.*`.
2. 80 `ArtPattern` entries and 41 starter palettes.
3. Every pattern passes through `CoverageCalibrator.calibrate(...)`.
4. Rendered true black is within ±7% of target at 25/65/85% and monotonic.
5. Non-black palettes never contain `#000000`.
6. Random intricacy stays in `[0.40, 0.60]`.
7. Max wallpaper axis is 4096 px.
8. Viewport transform and primitive fill rules are unchanged.

## Verification after sync

```bash
./gradlew test
```

Key tests:

- `CoreEnginePortabilityTest` — scans engine sources for forbidden imports, verifies all 80
  patterns in both weight modes, color safeguards, and dimension clamping.
- `SkiaRendererCalibrationTest` — renders patterns with Skia and checks pixel true-black coverage.
- `SettingsStoreTest`, `WallpaperFileStoreTest`, `WallpaperBackendTest` — persistence, export,
  and Linux backend contracts.

## Manifest

`core/ENGINE_COMMIT` records the Android commit used for the last sync. The About/Settings screen
and diagnostics show this value. Update it with the sync task or manually after syncing.

Recommended update flow:

```bash
cd ../rimalux
git rev-parse HEAD
cd ../rimalux-desktop
./gradlew syncAll
# write the commit hash to core/ENGINE_COMMIT
./gradlew test
```

## Adding a pattern

1. Add the pattern in the Android repo following its `AGENTS.md`.
2. Sync to desktop.
3. Run tests.
4. Verify the pattern appears in the desktop gallery with localized title/subtitle and preview.
5. If the Android preview asset is missing, add it there first.
