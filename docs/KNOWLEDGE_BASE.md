# Knowledge Base

Domain and implementation knowledge needed to work on Rimalux Desktop.

## Product terms

| Term | Meaning |
|---|---|
| Pattern | One of 80 procedural mathematical designs |
| Palette | A curated or user-created set of non-black colors |
| True Black Ratio | Percentage of the rendered wallpaper that is exactly `#000000` |
| Pattern Layout | `NORMAL` (full canvas) or `CENTER_WEIGHTED` (geometry compacted toward the center) |
| Color Fill | `SOLID` (one shaded color per primitive) or `GRADIENT` (linear two-color gradient) |
| Wallpaper Set | All per-monitor wallpaper items generated together |
| Wallpaper Item | The seed/pattern/palette/ratio/resolution for one monitor |
| Backend | Platform adapter that applies the wallpaper files |

## Engine pipeline

1. `WallpaperGeneratorEngine.synthesize(seed, spec, palettes, aspectRatio, ...)`
   - resolves pattern from seed/pool
   - resolves colors (random harmony or selected palette)
   - resolves target true-black ratio (single or range)
   - samples intricacy from seed (0.40–0.60)
2. `MathPatternGenerator.generate(...)`
   - seeded `Random(seed)`
   - builds `SculptField`
   - dispatches to one of 11 pattern suites
   - returns `VectorScene` with `VectorPrimitive`s
3. `CoverageCalibrator.calibrate(...)`
   - rasterizes a 200-wide coverage grid
   - assigns each primitive a threshold
   - adds a gapless underlay mosaic if needed
4. `DeepBlackRatioEngine`
   - visibility: `primitive.threshold >= clampedBlackRatio`
   - viewport transform: scale/offset to fit the target surface without distortion
   - pixel sampling for measured true black
5. Renderer draws primitives in order.

## Coordinate system

- `x ∈ [0, 1]`
- `y ∈ [0, aspectRatio]`, where `aspectRatio = height / width`, clamped to `[0.45, 3.2]`
- `1.0` unit in x equals `1.0` unit in y in physical pixel distance.
- Desktop landscape ratios (16:9 = 0.5625, 16:10 = 0.625) fit comfortably.
- Ultrawide 21:9 = 0.428 is clamped to 0.45; expect slight vertical crop in fill mode.

## Viewport transform

```text
scale   = max(widthPx / 1.0, heightPx / sceneAspectRatio)
offsetX = (widthPx - scale) * 0.5
offsetY = (heightPx - scale * sceneAspectRatio) * 0.5
drawMicroSeams = scale >= 220
```

Every normalized point maps to:

```text
px = offsetX + x * scale
py = offsetY + y * scale
```

## Primitive rendering rules

### Polygon
- Closed path.
- Non-zero winding fill.
- If `drawMicroSeams` and `strokeSeamNorm > 0`, stroke black outline with
  `clamp(strokeSeamNorm * scale, 0.5, 1.7)`.

### ArcSector
- Center/radii scaled by viewport scale.
- Skip if outer radius <= 0.5 px.
- `innerRadius <= 0.5` and sweep >= 359.5°: filled circle.
- `innerRadius <= 0.5`: pie sector.
- `innerRadius > 0.5` and sweep >= 359.5°: even-odd annulus.
- `innerRadius > 0.5` and sweep < 359.5°: annular sector, non-zero winding.

### PolylineRibbon
- Open or closed path.
- Round cap, round join.
- Stroke width `max(strokeWidthNorm * scale, 1.3)`.

## Color system

- Palettes never contain `#000000`.
- `ColorHarmonyEngine.resolveArgbColor` clamps near-black shaded colors to `#000004`.
- Solid fill uses the resolved shaded color.
- Gradient fill computes endpoints from the primitive pixel bounds and clamps them.
- Starter palettes: 41 curated sets, including monochromatic OLED.
- Random mode: HSV harmonies (complementary, analogous, triadic, split-complementary, etc.).

## True black measurement

`DeepBlackRatioEngine.measureTrueBlackPercentage(width, height) { x, y -> argb }` samples a grid
and counts pixels where `(argb and 0x00FFFFFF) == 0`. The Skia renderer exposes the same function
using Compose `PixelMap` or Skia bitmap reads.

Acceptance:

- Target 25% → measured within ±7 percentage points.
- Target 65% → measured within ±7.
- Target 85% → measured within ±7.
- Measured values increase monotonically with target.

## Resource conventions

### Strings
- Android keys are reused: `pattern_<id>`, `pattern_<id>_sub`, `palette_name_<id>`,
  `cat_flow`, `cat_symmetry`, `cat_architectural`, `cat_tessellation`.
- Desktop-only keys live in `desktop.xml` / `values-id/desktop.xml`.
- Dynamic lookup: `Res.allStringResources["pattern_${pattern.id}"]`, fallback to
  `ArtPattern.defaultTitle`.
- Palette lookup: `Res.allStringResources["palette_name_${palette.id}"]`, fallback to `name`.

### Previews
- Files: `pattern_preview_<id>.webp`.
- Dynamic lookup: `Res.allDrawableResources["pattern_preview_${pattern.id}"]`.

### Fonts
- `plus_jakarta_sans.ttf` is used for all Material 3 typography tokens.
- Use Compose Resources `Font(Res.font.plus_jakarta_sans, weight)` inside a composable
  `rememberRimaluxTypography()`.

## Persistence model

```text
DesktopSettings
├── generation: GenerationConfig
├── display: DisplaySettings
├── ui: UiSettings
├── autoChange: AutoChangeSettings (Phase 2)
├── currentSet: WallpaperSet?
├── history: List<WallpaperSet>
└── userPalettes: List<ColorPalette>
```

Engine enums are persisted as string IDs through DTOs, not serialized directly, because the
engine must remain free of kotlinx.serialization annotations.

## Naming

- Pattern IDs: snake_case (`silk_ribbons`).
- Palette IDs: snake_case (`obsidian_aurora`).
- Cache files: `rimalux_<pattern>_<seed-hex>_<width>x<height>.png`.
- Set IDs: timestamp + short random suffix.

## Desktop-specific terminology

| Android | Desktop |
|---|---|
| Home Screen / Lock Screen | Desktop / Display 1, Display 2, … |
| Apply To | Apply to Desktop / Apply to All Displays |
| Auto-Change Wallpaper | Auto-Change |
| OEM battery help | Desktop backend help / Start at Login help |
| Share | Save PNG / Copy Image / Open Folder |

## Known limitations

- Windows lock-screen wallpaper cannot be set by apps.
- GNOME/Cinnamon/MATE/Budgie use one wallpaper for all monitors.
- The engine caps wallpapers at 4096 px on the longest axis.
- Tray support on GNOME may require an AppIndicator extension (Phase 2).
- Flatpak/Snap sandboxing would block wallpaper commands; distribute deb/rpm/MSI/EXE.
