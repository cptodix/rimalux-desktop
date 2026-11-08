# Desktop UX Specification

> **Locked and accepted.** The UI reflects the original Android app’s structure and visual
> language, adapted to a desktop window: full-bleed wallpaper, media-player controls, and the
> expanding options modal. Do not reintroduce a menu bar, side rail, or generic dashboard layout.

## Main window — locked layout

The wallpaper is the centerpiece. There is no menu bar, no side navigation rail, and no
persistent settings panel.

```text
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│                                                                    │
│                     FULL-BLEED WALLPAPER PREVIEW                   │
│                                                                    │
│                                                                    │
│                 ┌────────────────────────────────┐                 │
│                 │ Pattern · ● ● ● ● · 90% Black  │  info pill      │
│                 └────────────────────────────────┘                 │
│  ┌────┐        ┌──────────────────────────────┐                       │
│  │ ☰  │        │  ⏮   [apply]   💾   ⏭        │                       │
│  └────┘        └──────────────────────────────┘                       │
└────────────────────────────────────────────────────────────────────┘
```

### Bottom controls

- **Bottom left:** floating options button → opens the options modal.
- **Bottom center:** media-player control pill:
  - Previous wallpaper (from history; disabled when history is empty).
  - Apply wallpaper (primary button; opens the Apply To popup).
  - Save PNG (lossless export).
  - Next wallpaper (prefetched; instant when the buffer is ready).
- The preview is not tied to a selected display. There is no display selector or layout button
  in the studio chrome.
- A subtle bottom gradient keeps controls readable without hiding the wallpaper.

### Apply To popup

Pressing Apply opens a compact popup above the button:

- Every detected display, with its native resolution.
- **All Displays** — the current preview design rendered at every display’s resolution.
- **All Displays Separate** — a distinct generated design per display. This changes the live
  wallpaper only; the preview stays on the current design.

The popup is disabled while applying and shows a spinner on the Apply button.

### Info pill

Above the bottom controls, a small translucent pill shows:

- Pattern name.
- Current palette dots (first 4 colors).
- Measured True Black percentage.

It must not block wallpaper viewing; it is compact and centered.

### Preview quality

- Previews are rendered at a **1920 px long edge** (aspect-preserving) instead of the old
  960 px preview, so the full-window wallpaper is crisp on 1080p and 1440p displays.
- Full-resolution apply/save still uses the target monitor resolution (capped at 4096 px).

### Prefetch buffer

- The controller keeps up to **3** upcoming rendered sets (`aheadQueue`).
- Pressing Next immediately promotes the first buffered set and starts refilling the buffer.
- If the buffer is empty, Next generates on demand and shows progress.
- Previous uses a small rendered-set cache when available, otherwise regenerates from history.
- Config/palette/layout changes clear the buffer and refill it with the new settings.

## Options modal

Opened by the bottom-left hamburger. Styled after the Android bottom sheet.

### Collapsed state

The modal is a floating dark panel anchored bottom-left. Its left column contains:

1. **Joined control card**
   - True Black Ratio: title, current percentage, Single/Range segmented control, slider/range.
   - Patterns row with summary and chevron.
   - Pattern Layout row with current mode and chevron (quick popup).
   - Colors row with palette summary and chevron.
   - Color Fill Style row with current mode and chevron (quick popup).
   - **Advanced Settings** row (same typography/height as the rows above).
2. **Auto-Change card**
   - Master switch, Apply To row, Interval row, Start at Login, Keep in Tray.
   - Apply To lists displays, **All Displays**, and **All Displays Separate**, matching the
     studio Apply popup.
   - Interval uses presets plus a custom minutes field.
3. **About footer** with version/credit.

The modal floats above the bottom media controls, so the options button and media controls
remain visible. It has no title row, no close button, and no back button. The left sheet keeps
the same width when a page opens; clicking the active row again collapses the right pane. Dismiss
via the scrim or `Esc`.

Language is not duplicated in the options modal; it lives in the Advanced Settings pane.

### Expanded tablet state

Selecting **Patterns** or **Colors** animates the modal wider to the right:

```text
┌───────────────────────────┬───────────────────────────────────────┐
│  Options left sheet       │  Patterns / Colors / Settings page    │
│  (Android sheet layout)   │  (same content as the desktop page)   │
└───────────────────────────┴───────────────────────────────────────┘
```

- The left sheet stays visible and scrollable at the same width as the collapsed state.
- The right pane shows the existing Patterns, Colors, or Advanced Settings content.
- Clicking the active row again collapses the right pane.
- Clicking the scrim dismisses the modal.
- `Esc` dismisses the modal.
- The modal animates in from the bottom and slides/fades out on close.

### Quick popups

- Pattern Layout: Normal / Center Weighted single-choice dialog.
- Color Fill Style: Solid / Gradients single-choice dialog.
- Styled like the Android `RimaluxModalDialog` rows: selected border, check icon, cancel.

## Patterns pane (Android parity)

- No search field.
- Randomized vs Select Custom segmented control.
- Category chips only in Select Custom mode: All, Flow & Waves, Symmetry & Sacred,
  Architectural, Tessellations.
- Multi-select hint with selected/total count.
- Select All / Select None text buttons.
- Adaptive multi-column grid of 80 preview cards.
- Selected cards use a primary border and check badge.
- The currently rendered pattern gets a subtle primary-tinted border.
- In Randomized mode, cards have no selected state. Clicking a card switches to Select Custom
  with only that pattern selected, matching Android behavior.

## Colors pane (Android parity)

- Palette Mode: Randomize All / Select Custom segmented control.
- Add Palette button.
- Select All / Select None and active/total hint in Select Custom mode.
- Multi-column palette list cards with large solid swatches.
- Selected cards use a primary border and check badge.
- Remove action for any palette while more than one remains; deleted starter palettes stay
  deleted via persisted `deletedPaletteIds`.
- No color-fill selector or shuffle button here; Color Fill is a quick row in the options sheet.

## Settings pane

- Language.
- Wallpaper resolution as a popup list (Auto Match / 1920 / 2560 / 3840 / Custom), showing the
  full `W×H` resolution for each option.
- Fit mode (Fill / Fit / Center / Tile).
- Backend status, refresh, override chips, custom command, test.
- Displays list with per-monitor resolution and an Identify action.
- Folder shortcuts in a two-column grid with icons.
- Background scheduler, support (updates + diagnostics), about, version, engine commit,
  license, shortcuts.

## Terminology

- **Patterns**, **Colors**, **True Black Ratio**, **Apply to Desktop**.
- Avoid Android Home/Lock target wording on desktop.
- Do not promise Windows lock-screen wallpaper support.

## Keyboard shortcuts

| Key | Action |
|---|---|
| `Space` / `→` | Next wallpaper (prefetched) |
| `←` | Previous wallpaper |
| `Ctrl/Cmd + S` | Save PNG |
| `Ctrl/Cmd + Enter` | Apply wallpaper |
| `Ctrl/Cmd + ,` | Open Advanced Settings |
| `Esc` | Exit fullscreen, then close options modal |
| `F11` / double-click | Toggle fullscreen preview |

## Tray and background

- Left-clicking the tray icon opens/focuses the Rimalux window.
- Right-clicking opens a compact context menu:
  1. **Open Rimalux Desktop**
  2. **Auto-Change Wallpaper** (checkable toggle)
  3. **Quit Rimalux**
- Closing the window hides to tray when Auto-Change or Keep in Tray is enabled.
- `--background` starts hidden to tray.
- `--rotate-now` performs one headless rotation for OS schedulers/tests.
- Start at Login writes an XDG autostart entry on Linux and an HKCU Run value on Windows.
- A second launch focuses the running instance instead of starting another scheduler.

## Fullscreen and display identification

- Press `F11` or double-click the wallpaper to toggle fullscreen preview.
- `Esc` exits fullscreen (and then closes the options modal if it is open).
- In fullscreen, the bottom controls and info pill are hidden; the wallpaper fills the screen.
- Display Layout → Identify shows the selected display name and resolution as a temporary
  overlay so the user can map physical monitors to the app's display list.

## Accessibility

- All controls keyboard reachable with visible focus.
- Icon-only controls have content descriptions.
- Minimum interactive size 40–48 dp.
- Selection uses borders/check icons, not color alone.
- Keep the info pill and bottom controls readable over any wallpaper via the bottom scrim.
