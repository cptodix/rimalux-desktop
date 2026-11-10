# Rimalux Desktop

Procedural OLED wallpaper studio for **Linux** and **Windows**, ported from the Android app
[`cptodix/rimalux`](https://github.com/cptodix/rimalux).

Rimalux generates mathematical vector wallpapers with an exact, user-selected `#000000`
True Black Ratio, 80 procedural patterns, 41 curated palettes, gradient/solid color fills,
normal and center-weighted layouts, per-monitor wallpapers, lossless PNG export, and
background auto-change.

> **Status:** Phase 0 + Phase 1 complete and accepted. Phase 2 (auto-change scheduler, tray,
> Start at Login, single instance, `--background`/`--rotate-now`) is implemented and verified in
> normal use. Phase 3 (Windows backend and CI packaging) and Phase 4 (fullscreen, identify,
> diagnostics, optional OS scheduler, update check, release automation) are implemented; Windows
> and real multi-monitor behavior still need hardware verification. See
> [`docs/ROADMAP.md`](docs/ROADMAP.md) and [`docs/VERIFICATION.md`](docs/VERIFICATION.md).

---

## Install

Linux (x86_64) — installs the AppImage plus a launcher and menu entry:

```bash
curl -fsSL https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.sh | bash
```

Windows — downloads the latest installer and runs it:

```powershell
irm https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.ps1 | iex
```

Pin a version with `RIMALUX_VERSION=0.1.2` (Linux) or `$env:RIMALUX_VERSION = "0.1.2"` (Windows).

Releases ship two assets: `rimalux_desktop_<version>_<YYYYMMDD>.AppImage` and
`rimalux_desktop_<version>_<YYYYMMDD>.exe`.

Rimalux checks GitHub releases from **Advanced Settings → Support**. When a newer release
exists, **Install Update** downloads it and hands over: the AppImage replaces itself and
relaunches, and the Windows installer runs after the app exits.

Uninstall with the same script plus the flag:

```bash
curl -fsSL https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.sh | bash -s -- -uninstall
```

```powershell
iex "& { $(irm https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.ps1) } -Uninstall"
```

Settings and cached wallpapers are left in place.

---

## What is implemented today

- Pure Kotlin wallpaper engine synced from the Android project.
- Full-bleed desktop studio with media-player controls, an info pill, and an expanding
  options modal modeled on the Android settings sheet.
- Crisp 1920 px long-edge previews and a prefetched next-wallpaper buffer (target 3 sets).
- Android-parity Patterns and Colors panes with multi-column desktop layouts.
- Auto-change scheduler with persisted next/last run, interval, target displays, dedupe, and
  catch-up after sleep/restart.
- Native tray icon: left click opens Rimalux, right click offers Open, Auto-Change toggle, and
  Quit. Linux uses a native StatusNotifierItem so KDE Wayland clicks work; Windows uses AWT.
  Start at Login, Keep in Tray, and single-instance activation are included.
- Windows wallpaper backend: `SystemParametersInfoW` global apply plus `IDesktopWallpaper`
  per-monitor apply, with HKCU Run autostart.
- Fullscreen preview (F11 or double-click) and a display Identify overlay.
- Apply modal with per-display, All Displays, All Displays Separate targets, and Save Wallpaper.
- Display-agnostic preview; Auto Match resolution uses each display's native resolution with a
  4K fallback.
- Support tooling: diagnostics bundle export, GitHub update check with in-place install, and an
  optional systemd user timer / Windows Task Scheduler mode.
- Single manual release workflow: bumps the version, creates the tag, builds the AppImage and
  Windows installer, and publishes the GitHub release.
- Live Skia-rendered previews with exact true-black measurement.
- 80 patterns, 41 palettes, custom palettes, true-black slider/range.
- Solid and gradient color fills.
- Normal and center-weighted pattern layouts.
- Monitor detection and per-monitor preview targets.
- KDE Plasma wallpaper apply (per-monitor via Plasma scripting) and GNOME global apply.
- Lossless PNG export to `~/Pictures/Rimalux`.
- English and Bahasa Indonesia localization.
- JSON settings persistence and wallpaper-set history.
- Options modal with Patterns/Colors/Settings expanding to the right like a tablet.

## What comes next

- Hardware verification on Windows 10/11 (global and per-monitor apply, autostart, tray).
- Multi-monitor verification on real Linux hardware.
- Iteration on the locked UI and release polish based on user feedback.

See [`docs/ROADMAP.md`](docs/ROADMAP.md) and [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md).

---

## Quick start (Linux)

Requirements:

- JDK 17 or newer (JDK 17 is used for the Gradle toolchain).
- A desktop session: KDE Plasma, GNOME, XFCE, Cinnamon, MATE, or Budgie.
- Internet access on the first build to download Gradle dependencies.

```bash
cd /home/cptodix/WorkingFolder/Programming/rimalux-desktop
./gradlew :desktopApp:run
```

If you need to regenerate the shared engine/string/preview assets from the Android checkout:

```bash
./gradlew syncAll
```

Environment used for baseline verification: openSUSE Tumbleweed, KDE Plasma 6.7.5 Wayland,
JDK 17, 1920×1080 display. Detailed run instructions and troubleshooting:
[`docs/RUNNING.md`](docs/RUNNING.md).

## Build installers

```bash
# Linux (run on Linux). A full JDK with jmods is required for jlink/jpackage.
RIMALUX_PACKAGING_JDK=/path/to/jdk-21 ./gradlew :desktopApp:packageRpm
RIMALUX_PACKAGING_JDK=/path/to/jdk-21 ./gradlew :desktopApp:packageDeb

# Windows (must run on Windows)
.\gradlew.bat :desktopApp:packageMsi
.\gradlew.bat :desktopApp:packageExe
```

RPM packaging was verified on this machine with Temurin 21. See
[`docs/BUILD_AND_RELEASE.md`](docs/BUILD_AND_RELEASE.md).

## Repository layout

```text
rimalux-desktop/
├── core/                       # Pure Kotlin engine synced from the Android app
├── desktopApp/                 # Compose Desktop application
│   └── src/main/
│       ├── kotlin/             # UI, renderer, platform backends, persistence
│       └── composeResources/   # Strings, previews, fonts, icons
├── docs/                       # Architecture, porting, rules, runbooks
├── AGENTS.md                   # Agent handover rules
└── gradle/                     # Version catalog and wrapper
```

## Documentation

| Document | Purpose |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Rules and handover guide for AI agents/contributors |
| [`RULES.md`](RULES.md) | Mandatory testing and visual verification rules |
| [`docs/CONTEXT.md`](docs/CONTEXT.md) | Product context, decisions, environment |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module and runtime architecture |
| [`docs/PORTING_STRATEGY.md`](docs/PORTING_STRATEGY.md) | What is reused vs rebuilt from Android |
| [`docs/ENGINE_SYNC.md`](docs/ENGINE_SYNC.md) | Engine sync contract and invariants |
| [`docs/KNOWLEDGE_BASE.md`](docs/KNOWLEDGE_BASE.md) | Engine math, terminology, resource conventions |
| [`docs/DESKTOP_UX.md`](docs/DESKTOP_UX.md) | Desktop UI specification |
| [`docs/AUTO_CHANGE.md`](docs/AUTO_CHANGE.md) | Auto-change, tray, and optional OS scheduler |
| [`docs/LINUX_WALLPAPER_BACKENDS.md`](docs/LINUX_WALLPAPER_BACKENDS.md) | Linux DE support matrix |
| [`docs/WINDOWS_WALLPAPER_BACKENDS.md`](docs/WINDOWS_WALLPAPER_BACKENDS.md) | Windows APIs and limitations |
| [`docs/BUILD_AND_RELEASE.md`](docs/BUILD_AND_RELEASE.md) | Build, package, CI, release |
| [`docs/TESTING.md`](docs/TESTING.md) | Test suites and manual verification |
| [`docs/RUNNING.md`](docs/RUNNING.md) | How to run and troubleshoot |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Phase status |
| [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) | Detailed agentic checklist |
| [`docs/VERIFICATION.md`](docs/VERIFICATION.md) | Phase 0/1 verification log |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Architecture decision records |

## License

PolyForm Perimeter License 1.0.1 — <https://polyformproject.org/licenses/perimeter/1.0.1>.

Required Notice: Copyright © 2026 Cptodix Studio

You may use, modify and distribute Rimalux for any purpose except providing a product that
competes with it. Third-party components keep their own licenses; see
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
