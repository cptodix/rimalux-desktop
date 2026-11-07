# Verification Log

## 2026-11-07 — Phase 0 + Phase 1 on KDE Plasma Wayland

### Environment

- openSUSE Tumbleweed
- KDE Plasma 6.7.5, Wayland session
- JDK 17 (`JAVA_HOME=/usr/lib64/jvm/java-17-openjdk-17`)
- Display: eDP-1, 1920×1080
- Gradle 9.3.1 (wrapper), Kotlin 2.4.21, Compose Multiplatform 1.12.2
- Android engine commit: `8b2efe4c449e914e2bbd5bc900595d75b5305bb8`

### Commands run

```bash
./gradlew syncAll
./gradlew test
./gradlew :desktopApp:run --args="--render-test /tmp/opencode/rimalux-render-test"
./gradlew :desktopApp:run
```

### Automated results

| Test | Result |
|---|---|
| `CoreEnginePortabilityTest` | Pass |
| `SkiaRendererCalibrationTest` (80 patterns × 2 weight modes × 3 ratios) | Pass |
| `SettingsStoreTest` | Pass |
| `WallpaperFileStoreTest` | Pass |
| `WallpaperBackendTest` | Pass |
| `StudioControllerPrefetchTest` | Pass — next-wallpaper buffer fills headlessly |
| `AutoChangePolicyTest` | Pass — intervals, dedupe, due/next-run rules |
| `AutostartManagerTest` | Pass — Linux XDG autostart entry |
| `SingleInstanceManagerTest` | Pass — second instance notifies first |
| `StudioControllerAutoChangeTest` | Pass — fake clock/backend rotation |
| `./gradlew test` | BUILD SUCCESSFUL |

The headless render proof produced 1920×1080 PNGs with measured true black at 85–86% for a
90% target on the sampled patterns. The full calibration test passed for all 80 patterns in both
`NORMAL` and `CENTER_WEIGHTED` modes.

### Manual results — Phase 0/1 baseline

| Check | Result |
|---|---|
| App window launches on KDE Wayland (pre-lock layout) | Pass |
| Studio/Patterns/Colors/Settings baseline screens | Pass |
| Language switch EN ↔ ID | Pass (persisted in `settings.json`) |
| Settings survive restart | Pass |
| KDE apply wallpaper | Pass — in-app status: “Applied to 1 display(s) · 90% true black” |
| Per-monitor apply on multi-monitor hardware | Not tested — verification machine has one display |
| Windows MSI/EXE | Not tested — Phase 3 |
| Auto-change / tray / start at login | Implemented; user-guided runtime verification pending |
| Auto-change scheduler tests | Pass — policy, autostart, single instance, fake rotation |
| RPM packaging | Pass — `rimalux-0.1.0-1.x86_64.rpm` built with Temurin 21 |
| Packaged RPM launch | Pass — extracted `/opt/rimalux/bin/Rimalux` launched and appeared in the window list |
| DEB packaging | Not tested — `dpkg-deb` is not installed on this openSUSE machine |

### Locked UI redesign

The full-bleed wallpaper + media-player controls + expanding options modal is implemented.
User-guided visual verification was accepted on 2026-11-07. The UI now mirrors the Android
app’s structure while adapting it to desktop. No new screenshots were taken, per `RULES.md`.

### Notes

- The verification run changed the live desktop wallpaper to a generated Rimalux wallpaper.
  Restore the previous Plasma wallpaper from System Settings → Appearance → Wallpaper, or run:
  `plasma-apply-wallpaperimage /usr/share/wallpapers/openSUSEdefault/contents/images/default.png`
- The app is currently English after the language switch test; change it back in Settings →
  Language → Bahasa Indonesia.
- Multi-monitor behavior is implemented in the backend but cannot be hardware-verified on this
  single-display machine. The Display Layout dialog and KDE screen-index mapping are ready for
  testing on a multi-monitor setup.
