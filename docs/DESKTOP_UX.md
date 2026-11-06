# Desktop UX Specification

## Window

- Default size: 1280 × 820 dp.
- Minimum size: 1024 × 700 dp.
- Native window decorations; OLED black content.
- Left navigation rail: Studio, Patterns, Colors, Settings (Auto-Change added in Phase 2).
- Top app bar per screen with title and primary actions.
- Bottom status line for backend, resolution, and true-black feedback.
- Snackbar for transient status messages.

## Studio screen

```text
┌────────────┬─────────────────────────────────────────────────────────┐
│ Nav rail   │ Top bar: Rimalux Studio   [Randomize] [Apply] [Save]    │
│            ├──────────────────────────────┬──────────────────────────┤
│ Studio     │                              │ Pattern card             │
│ Patterns   │      Wallpaper preview       │ Colors card              │
│ Colors     │      (selected display)      │ True Black Ratio         │
│ Settings   │                              │ Pattern Layout           │
│            │  [Display 1 ▾] [Preview All] │ Color Fill               │
│            │                              │ Display Targets          │
│            │  History strip               │ Actions                  │
└────────────┴──────────────────────────────┴──────────────────────────┘
```

### Preview pane
- Shows the selected monitor's wallpaper at its aspect ratio.
- Pure black letterbox; subtle 1 dp outline and 12 dp rounded corners.
- Overlay label: display name, resolution, measured true-black percentage.
- Progress indicator while synthesizing.
- When more than one display is detected, a horizontal strip of all monitor previews appears
  below the main preview.
- History strip below the preview: last sets, click to restore the whole set.

### Control panel
- **Pattern card** — current pattern title/subtitle, seed, “Browse Patterns”.
- **Colors card** — palette name/swatches, “Browse Colors”.
- **True Black Ratio** — Single/Range segmented control, slider(s), percentage label.
- **Pattern Layout** — Normal / Center Weighted segmented control.
- **Color Fill** — Solid / Gradient segmented control.
- **Display Targets** — per-display support status, “Match design across displays” switch,
  and “Display Layout” button. Auto-change target checkboxes arrive in Phase 2.
- **Actions** — Apply to All Displays, Apply to This Display, Save PNG, Save All Displays.
- Disabled states must be visibly dimmed and non-interactive.

## Patterns screen

- Search field.
- Category filter chips: Flow & Waves, Symmetry & Sacred, Architectural, Tessellations.
- Randomize Patterns switch.
- Select All / Select None.
- Adaptive grid (`LazyVerticalGrid`, min 180 dp).
- Cards show preview image, localized title, subtitle, and a check overlay when selected.
- Click toggles selection; a preview icon applies that pattern to the current set.
- When randomize is on, individual selection is disabled with a short note.

## Colors screen

- Fill mode: Solid / Gradient.
- Palette mode: Random Harmony / My Palettes.
- Shuffle Harmony button (random mode).
- Create Palette button.
- Adaptive palette grid; cards show name and 5 swatches; selected border + check.
- Custom palettes can be deleted with confirmation.
- Create Palette dialog: name, 2–6 color slots, add/remove, HSV + hex color picker.
- Pure `#000000` is rejected as a palette color (OLED void is reserved).

## Settings screen

- Language: English (US) / Bahasa Indonesia.
- Wallpaper resolution: Auto / 1920×1080 / 2560×1440 / 3840×2160 / Custom.
- Fit mode: Fill / Fit / Center (backend-dependent; Fill default).
- Display layout and mapping.
- Backend status: detected backend, per-monitor support, Test button (Phase 1: apply a test image).
- Folders: config, cache, Pictures, logs; “Open” buttons.
- About: version, engine commit, license, support link.
- Phase status note for auto-change (Phase 2).

## Display Layout dialog

- Visual rectangles positioned by monitor bounds, scaled to fit.
- Labels: Display 1, resolution, primary marker.
- Click a rectangle to select it for preview.
- Checkboxes for target displays.
- “Match design across displays” switch.
- “Refresh displays” button.
- KDE: screen order follows the Plasma output order; a reorder/identify workflow is planned for
  Phase 2/4.
- GNOME: note that the DE applies one wallpaper to all displays.

## Terminology and copy

- Title Case for screen titles, modal titles, section headers, buttons.
- Sentence case for descriptions and status messages.
- “Patterns”, “Colors”, “True Black Ratio”, “Apply to Desktop”.
- Do not use “Styles”, “Pitch Black Ratio”, or Android Home/Lock targets in desktop copy.

## Keyboard shortcuts

| Key | Action |
|---|---|
| `Space` | Randomize |
| `←` / `→` | Previous / next history set |
| `Ctrl/Cmd + S` | Save PNG |
| `Ctrl/Cmd + Enter` | Apply wallpaper |
| `Ctrl/Cmd + ,` | Settings |
| `F11` | Fullscreen preview (Phase 4) |
| `Esc` | Close dialog / exit fullscreen |

## Accessibility

- All controls keyboard reachable with visible focus.
- Content descriptions for icon-only buttons.
- Minimum interactive size 40 dp on desktop, 48 dp where practical.
- Do not rely on color alone for selection; use check icons and borders.
- Respect system font scaling where possible.

## Localization

- Compose Resources select `values-id` when the JVM locale is Indonesian.
- Language selector changes `Locale.setDefault` and wraps the UI in `key(languageTag)`.
- Pattern titles/subtitles and palette names resolve from synced Android strings.
- Desktop-only strings are maintained in both English and Indonesian.
