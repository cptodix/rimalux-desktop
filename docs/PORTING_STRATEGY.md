# Porting Strategy

## Goal

Ship a first-class Linux and Windows desktop app without forking or destabilizing the Android
project. Reuse the hard part — the mathematical engine — and rebuild only the platform layers
that cannot be shared.

## Reuse vs rebuild

| Android layer | Desktop decision |
|---|---|
| `engine/` (80 patterns, calibration, colors, true black, watermark solver) | **Reuse 100%**, synced into `core/` |
| `WallpaperCoreModels` / `CoreGenerationSpec` / `SynthesizedWallpaper` | Reuse directly |
| Theme color tokens, shape scale, typography tokens | Reuse, adapted to Compose Resources fonts |
| `strings.xml` / `values-in` | Reuse via Compose Resources, plus desktop-only keys |
| 80 pattern preview WebP assets | Reuse in the pattern gallery |
| `ComposeSceneRenderer` drawing rules | Reimplemented in the Skia renderer with identical geometry |
| `WallpaperRenderer` (Android Canvas) | Replaced by `SkiaWallpaperRenderer` |
| `WallpaperManager` apply | Replaced by Linux/Windows wallpaper backends |
| DataStore preferences | Replaced by JSON settings store |
| WorkManager + AlarmManager | Replaced by tray-resident scheduler + optional OS scheduler |
| MediaStore / FileProvider share | Replaced by Pictures export, folder open, clipboard |
| Play Billing tip jar | Removed; optional support links later |
| Mobile gesture UI / phone mockups | Replaced by desktop-native UI |
| OEM battery help | Replaced by desktop backend/autostart help |
| Home/Lock/Both targets | Replaced by Desktop / All Displays / per-monitor |

## Why Compose Multiplatform Desktop

- The engine is Kotlin; Compose Desktop consumes it with zero translation.
- The existing UI is already Compose; theme, typography, and component knowledge carry over.
- Skia is the renderer underneath Android Canvas and Compose Desktop, so the rasterization
  behavior is close to the original.
- jpackage produces `.deb`, `.rpm`, `.msi`, and `.exe` installers.
- A future KMP extraction remains possible without rewriting the engine.

## Why not Electron/Tauri

- The engine would need a TypeScript/Rust rewrite, with a high risk of diverging from the
  calibrated true-black output.
- The 80-pattern engine is the product; reimplementing it is the expensive part.
- Compose Desktop already gives a native JVM app with Skia rendering.

## Why not a 1:1 mobile UI port

The Android UI is optimized for touch and a phone-sized canvas. A literal port would:

- waste screen space on desktop,
- require reimplementing gesture physics for mouse,
- retain phone mockups that add no value on a desktop monitor,
- be more work than a responsive desktop layout.

The desktop UI keeps the **visual identity and terminology**, not the mobile navigation model.

## Why a separate repository now

The Android project has strict build/version restrictions and a working release pipeline. A
KMP monorepo refactor would require upgrading Kotlin/AGP/Compose and risking Android releases.
The desktop repo keeps a synced copy of the engine, with a manifest and tests to detect drift.

A future `rimalux-core` shared library can replace the copy without changing desktop UI code.

## Engine sync

```bash
./gradlew syncAll
```

Tasks copy:

- `engine/` → `core/src/main/kotlin/com/cptodix/rimalux/oled/wallpaper/engine/`
- `values/strings.xml` → `desktopApp/src/main/composeResources/values/strings.xml`
- `values-in/strings.xml` → `desktopApp/src/main/composeResources/values-id/strings.xml`
- `drawable-nodpi/pattern_preview_*.webp` → `composeResources/drawable/`
- `font/plus_jakarta_sans.ttf` → `composeResources/font/`
- launcher icon → `composeResources/drawable/rimalux_icon.png`

Desktop-only strings live in `values/desktop.xml` and `values-id/desktop.xml`, so syncing never
overwrites them.

## Per-monitor model

Android has one wallpaper target (`HOME`/`LOCK`/`BOTH`). Desktop can have multiple monitors,
each with its own resolution and aspect ratio. The desktop app therefore generates a
`WallpaperSet` containing one `WallpaperItem` per monitor.

- Matching design: same seed/spec across monitors; geometry adapts per aspect ratio.
- Unique design: distinct seeds/patterns/colors per monitor.
- Apply: backend-specific per-monitor command; global fallback where the DE does not support it.
- History: stores the whole set so previous/next restores all displays together.

## Phase mapping

| Phase | Scope |
|---|---|
| 0 | Scaffold, engine sync, Skia renderer, monitor detection, render proof |
| 1 | Desktop UI, patterns/colors, true black, per-monitor preview/apply, PNG export, settings, EN/ID |
| 2 | Tray, auto-change scheduler, start at login, single instance, dedupe/catch-up |
| 3 | Windows backend, MSI/EXE, CI, Windows self-test |
| 4 | Polish, diagnostics, optional system scheduler, fullscreen, release |
