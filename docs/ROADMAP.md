# Roadmap

## Phase 0 — Foundation ✅

- [x] Repository docs and agent rules
- [x] Gradle project (`core`, `desktopApp`)
- [x] Engine sync tasks
- [x] Engine portability test
- [x] Skia renderer
- [x] Monitor detection (AWT + KDE best-effort)
- [x] Headless render proof
- [x] Compose window scaffold

## Phase 1 — Desktop MVP ✅ (locked UI accepted)

- [x] OLED theme and typography
- [x] Locked full-bleed wallpaper layout (no menu bar / side rail)
- [x] Bottom media-player controls + info pill
- [x] Expanding options modal (Patterns/Colors/Settings)
- [x] Prefetched next-wallpaper buffer
- [x] Patterns/Colors/Settings panes
- [x] True-black single/range controls
- [x] Pattern layout and color fill
- [x] Per-monitor preview selection
- [x] KDE per-monitor apply
- [x] GNOME global apply
- [x] PNG export
- [x] JSON settings and history
- [x] English/Indonesian localization
- [x] Run documentation
- [x] Automated renderer/persistence/backend tests
- [x] Verified app launch, apply, and settings persistence on KDE Wayland
- [x] RPM package builds and packaged app launches
- [ ] DEB package (needs a Debian/Ubuntu host with `dpkg-deb`)
- [ ] Final visual polish pass
- [ ] Verify per-monitor apply on a multi-monitor setup

## Phase 2 — Auto-change ✅ (verified in normal use)

- [x] Scheduler with persisted next/last run
- [x] Dedupe + catch-up after sleep/restart
- [x] Tray icon and menu
- [x] Start at Login (XDG autostart + Windows Run key)
- [x] Single instance
- [x] Auto-Change section in the options modal
- [x] Target display selection, interval picker, Change Now
- [x] Failure logging + last error in UI
- [x] `--background` and `--rotate-now` CLI modes
- [x] Scheduler/policy/autostart/single-instance/auto-rotation tests
- [x] Tray + real auto-change verification
- [ ] Desktop notifications (optional follow-up)

## Phase 3 — Windows ✅ (implemented, hardware verification pending)

- [x] `SystemParametersInfoW` global backend
- [x] `IDesktopWallpaper` per-monitor backend
- [x] Windows monitor enumeration (AWT + COM device paths)
- [x] HKCU autostart
- [x] Windows tray (AWT `SystemTray`)
- [x] `--test-backend`
- [x] MSI/EXE CI build
- [ ] Human Windows checklist

## Phase 4 — Release polish ✅ (implemented)

- [x] Fullscreen preview (F11 / double-click)
- [x] Display identify workflow
- [x] Diagnostics bundle (`--diagnostics-bundle`, Settings → Support)
- [x] Optional systemd user timer / Windows Task Scheduler
- [x] Update checker (`--check-updates`, Settings → Support)
- [x] CI build + tag-driven release automation
- [ ] Signed installers (needs release certificate/secrets)
- [ ] Update checker download/install flow

## Non-goals

- 1:1 mobile gesture UI
- Electron/Tauri rewrite
- Engine rewrite in another language
- Windows lock-screen wallpaper
- Flatpak/Snap sandboxed wallpaper control
