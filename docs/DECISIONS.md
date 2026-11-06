# Architecture Decision Records

## ADR-001 — Compose Multiplatform Desktop

**Decision:** Build the desktop app with Compose Desktop (Kotlin/JVM).

**Why:** The engine is Kotlin; the Android UI is Compose; Skia is the shared renderer; jpackage
produces native installers.

**Alternatives rejected:** Electron/Tauri (engine rewrite, fidelity risk), Qt (new language/UI),
Swing (poor modern UI velocity).

## ADR-002 — Synced engine copy instead of KMP monorepo

**Decision:** Copy the engine into `core/` with Gradle sync tasks; keep the Android repo
untouched.

**Why:** Avoid destabilizing the Android build/release pipeline. The engine is explicitly
portable and requires zero edits. Tests detect drift.

**Future:** Extract `rimalux-core` as a shared KMP library once both apps are stable.

## ADR-003 — Desktop-native UI

**Decision:** New responsive desktop layout; do not port the mobile gesture UI.

**Why:** Swipe decks, bottom sheets, and phone mockups are touch/phone-specific. A desktop layout
is better UX and less work than adapting gesture physics to mouse.

## ADR-004 — Per-monitor wallpapers

**Decision:** Generate a `WallpaperSet` with one item per monitor; support per-monitor apply where
the OS/DE allows it.

**Why:** Desktop users commonly have multiple monitors with different aspect ratios. The engine is
aspect-aware, so generating per-monitor is natural.

**Limitations:** GNOME/Cinnamon/MATE/Budgie use one global wallpaper.

## ADR-005 — Tray + Start at Login for auto-change

**Decision:** Primary auto-change mode is a tray-resident app with opt-in autostart. An optional
OS scheduler mode may be added later.

**Why:** Desktop has no cross-platform WorkManager equivalent. A tray app is simple and reliable
for most users. OS schedulers add complexity and environment issues.

## ADR-006 — English + Indonesian from day one

**Decision:** Reuse Android `strings.xml` and `values-in` via Compose Resources; add desktop keys
in both languages.

**Why:** The Android app is already bilingual; the user base and system locale include Indonesian.
The strings are reusable, so the cost is low.

## ADR-007 — Windows verification via CI + human checklist

**Decision:** Implement the Windows backend with unit tests and a CI self-test, but require a
human Windows verification before declaring it stable.

**Why:** The development machine is Linux. CI can build installers and exercise some APIs, but
real wallpaper/per-monitor behavior needs an interactive Windows session.

## ADR-008 — JSON settings instead of DataStore

**Decision:** Use kotlinx.serialization JSON with atomic file writes.

**Why:** DataStore is Android-specific; JSON is inspectable, portable, and easy to back up. DTOs
keep engine enums free of serialization annotations.

## ADR-009 — Skia renderer instead of AWT

**Decision:** Rasterize with Skia (via Compose Desktop/Skiko).

**Why:** Android Canvas is Skia; matching fill rules, gradients, and anti-aliasing is easier.
Skia also encodes PNG directly.

## ADR-010 — No lock-screen wallpaper promise

**Decision:** Desktop app applies to the desktop wallpaper only.

**Why:** Windows does not expose a lock-screen wallpaper API; Linux DEs are inconsistent. Avoid
misleading controls.

## ADR-011 — 4096 px cap preserved

**Decision:** Keep the engine’s 4096 px maximum axis.

**Why:** Consistency with Android, memory safety, and tested rendering behavior. Very high
resolution monitors are scaled proportionally.

## ADR-012 — Cache policy

**Decision:** Keep full-resolution PNGs for the current set and a few recent sets; store older
history as metadata and regenerate on demand.

**Why:** Full-resolution PNGs are large; metadata regeneration is cheap and keeps disk usage
bounded.

## ADR-013 — KDE per-monitor via Plasma scripting

**Decision:** Use `qdbus6`/`qdbus` `evaluateScript` with `desktop.screen` for per-monitor;
`plasma-apply-wallpaperimage` as global fallback.

**Why:** It is the supported Plasma scripting API and works on Wayland. `desktop.screen` may
require a mapping/reorder UI, planned for Phase 2.
