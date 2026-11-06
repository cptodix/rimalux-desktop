# Project Context

## Origin

Rimalux is an Android OLED wallpaper studio built with Kotlin and Jetpack Compose. Its most
valuable asset is a **pure Kotlin mathematical engine** that generates 80 procedural vector
patterns, calibrates exact `#000000` coverage, resolves palettes, and computes gradients and
watermark placement.

The Android app is mobile-first: swipe deck, bottom sheet, phone mockups, WorkManager +
AlarmManager scheduling, `WallpaperManager`, MediaStore, and Play Billing.

This repository ports the product to **Linux and Windows** as a desktop-native application.

## User goals

- Use Rimalux on Linux and Windows.
- Keep the same visual identity, patterns, palettes, and true-black accuracy.
- Support **per-monitor wallpapers**.
- Support **wallpaper auto-change** with background operation.
- Keep English and Bahasa Indonesia localization.
- Keep the Android project untouched as the source of truth for the engine.

## Locked decisions

| Decision | Choice |
|---|---|
| UI | Desktop-native Compose Multiplatform layout (no mobile gesture UI) |
| Code sharing | Engine copied/synced into `core/`; Android repo untouched |
| Auto-change | Tray-resident app + opt-in Start at Login (Phase 2) |
| Multi-monitor | Per-monitor from day one where the OS/DE supports it |
| Localization | English + Bahasa Indonesia from day one |
| Windows verification | CI build + self-test; human Windows verification before stable release |

## Current environment

- Linux: openSUSE Tumbleweed
- Desktop: KDE Plasma 6.7.5, Wayland session
- Java: JDK 17 (`JAVA_HOME=/usr/lib64/jvm/java-17-openjdk-17`)
- Available tools: `plasma-apply-wallpaperimage`, `qdbus6`, `gsettings`, `feh`,
  `kscreen-doctor`, `xrandr`
- System locale: Indonesian (the app should default to Bahasa Indonesia when appropriate)
- Current display: 1920×1080 (eDP-1)

## Current status (2026-11-07)

Phase 0 + Phase 1 are implemented and verified on KDE Plasma Wayland. See
[`VERIFICATION.md`](VERIFICATION.md). Auto-change/tray is Phase 2; Windows is Phase 3.

## Scope of Phase 0 + Phase 1

Implemented:

- Gradle project with `core` + `desktopApp`.
- Engine/string/preview/font sync from `../rimalux`.
- Pure-JVM portability and synthesis tests.
- Skia renderer for previews and full-resolution PNGs.
- Monitor detection (AWT + KDE `kscreen-doctor` best-effort).
- Compose Desktop UI: Studio, Patterns, Colors, Settings.
- True-black single/range controls, pattern layout, fill mode.
- Per-monitor preview selection and target checkboxes.
- KDE per-monitor apply, GNOME global apply, custom-command fallback.
- PNG export to `~/Pictures/Rimalux`.
- JSON settings and wallpaper-set history.
- English/Indonesian resources.

Not in Phase 1:

- Tray, auto-change scheduler, start at login (Phase 2).
- Windows `IDesktopWallpaper` backend and installers (Phase 3).
- Systemd/Task Scheduler mode (Phase 4).
- Fullscreen preview, update checker (Phase 4).
- Lock-screen wallpaper (not supported on Windows; inconsistent on Linux).

## Non-goals

- A 1:1 copy of the Android mobile UI.
- Electron/Tauri rewrite.
- Reimplementing the engine in another language.
- Changing the Android app's build or engine behavior from this repository.

## Related documents

- [`ARCHITECTURE.md`](ARCHITECTURE.md)
- [`PORTING_STRATEGY.md`](PORTING_STRATEGY.md)
- [`ENGINE_SYNC.md`](ENGINE_SYNC.md)
- [`KNOWLEDGE_BASE.md`](KNOWLEDGE_BASE.md)
- [`ROADMAP.md`](ROADMAP.md)
- [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md)
