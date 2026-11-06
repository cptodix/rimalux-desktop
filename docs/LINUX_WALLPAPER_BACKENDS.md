# Linux Wallpaper Backends

## Support matrix

| Desktop | Global | Per-monitor | Primary backend |
|---|---|---|---|
| KDE Plasma 5.26+/6 | Yes | Yes | `qdbus6`/`qdbus` PlasmaShell script |
| GNOME 42+ | Yes | No | `gsettings` |
| XFCE 4.16+ | Yes | Yes | `xfconf-query` |
| Cinnamon | Yes | No | `gsettings` |
| MATE | Yes | No | `gsettings` |
| Budgie | Yes | No | `gsettings` |
| Sway / wlroots | Yes | Yes, best-effort | `swaybg` / `swww` |
| Hyprland | Yes | Yes, best-effort | `hyprpaper` / `swww` |
| Unknown | Custom command | Custom command | `{file}`, `{monitor}` |

## Detection

Detect in this order:

1. `XDG_CURRENT_DESKTOP` / `DESKTOP_SESSION` contains `KDE` → KDE backend.
2. Contains `GNOME` / `Unity` / `Budgie` → GNOME backend.
3. Contains `XFCE` → XFCE backend.
4. Contains `X-Cinnamon` → Cinnamon backend.
5. Contains `MATE` → MATE backend.
6. `SWAYSOCK` set or `HYPRLAND_INSTANCE_SIGNATURE` set → compositor backend.
7. Fallback → custom command backend (disabled until configured).

Also verify the required commands exist (`qdbus6`, `gsettings`, `xfconf-query`, etc.) before
reporting the backend as available.

## KDE Plasma

### Per-monitor via Plasma scripting

```bash
qdbus6 org.kde.plasmashell /PlasmaShell org.kde.PlasmaShell.evaluateScript '
  var images = ["file:///path/display0.png", "file:///path/display1.png"];
  var ds = desktops();
  for (var i = 0; i < ds.length; i++) {
    var d = ds[i];
    var idx = d.screen;
    if (idx < 0 || idx >= images.length) idx = 0;
    d.wallpaperPlugin = "org.kde.image";
    d.currentConfigGroup = ["Wallpaper", "org.kde.image", "General"];
    d.writeConfig("Image", images[idx]);
    d.writeConfig("FillMode", 2);
  }
'
```

- `d.screen` is the Plasma screen index.
- `FillMode` values: `0` stretch, `1` preserve aspect fit, `2` preserve aspect crop,
  `3` tile, `4` center, `5` pad. Use `2` for fill.
- Escape file URIs before embedding in the script. Use `Path.toUri().toString()` and escape
  backslashes/quotes.
- If `d.screen` is unavailable in a Plasma version, fall back to `images[0]` for all desktops
  and use the global command.

### Global fallback

```bash
plasma-apply-wallpaperimage -f preserveAspectCrop /path/wallpaper.png
```

This is the simplest path for a single monitor or a global wallpaper. It does not guarantee
per-monitor mapping.

### Screen order

KDE screen indices should match the QScreen order, which usually matches `kscreen-doctor`
output order. If they do not, provide a mapping/reorder control in the Display Layout dialog.

### Monitor resolution

Use `kscreen-doctor -o -j` to read connected outputs, current mode sizes, geometry, and rotation.
Fall back to AWT bounds.

## GNOME

```bash
gsettings set org.gnome.desktop.background picture-uri "file:///path/wallpaper.png"
gsettings set org.gnome.desktop.background picture-uri-dark "file:///path/wallpaper.png"
gsettings set org.gnome.desktop.background picture-options "zoom"
```

- GNOME uses one image for all monitors. The UI must say so.
- `picture-uri-dark` exists on GNOME 42+; ignore errors on older versions.
- Lock screen: not part of the Phase 1 scope.

## XFCE

Discover properties:

```bash
xfconf-query -c xfce4-desktop -l
```

Set each monitor's `last-image`:

```bash
xfconf-query -c xfce4-desktop \
  -p /backdrop/screen0/monitorDP-1/workspace0/last-image \
  -s /path/wallpaper.png
```

Set image style to zoom:

```bash
xfconf-query -c xfce4-desktop \
  -p /backdrop/screen0/monitorDP-1/workspace0/image-style \
  -s 5
```

Apply to every workspace property for the matching monitor. If monitor names cannot be mapped,
set all `last-image` properties.

## Cinnamon

```bash
gsettings set org.cinnamon.desktop.background picture-uri "file:///path/wallpaper.png"
gsettings set org.cinnamon.desktop.background picture-options "zoom"
```

Global only.

## MATE

```bash
gsettings set org.mate.background picture-filename "/path/wallpaper.png"
```

Global only.

## Budgie

Budgie uses the GNOME background schema:

```bash
gsettings set org.gnome.desktop.background picture-uri "file:///path/wallpaper.png"
```

Global only.

## Sway / wlroots

Preferred: `swaybg`

```bash
swaybg -o DP-1 -i /path/display0.png -m fill
swaybg -o HDMI-A-1 -i /path/display1.png -m fill
```

Run one managed process with multiple `-o` pairs, or restart `swaybg` when applying.
Alternative: `swww img /path/display0.png --outputs DP-1`.

## Hyprland

If `hyprpaper` is running:

```bash
hyprctl hyprpaper preload /path/display0.png
hyprctl hyprpaper wallpaper "DP-1,/path/display0.png"
```

Alternative: `swww img /path/display0.png --outputs DP-1`.

## Custom command

Settings field supports:

- `{file}` — absolute PNG path
- `{monitor}` — monitor name/id

Example:

```bash
feh --bg-fill {file}
```

The command is parsed into arguments and executed with `ProcessBuilder`; no shell is used by
default. If shell behavior is required, the user can explicitly use `sh -c '...'`.

## Security

- Never concatenate untrusted paths into a shell string.
- Use `ProcessBuilder` argument lists.
- Escape JavaScript string literals for Plasma scripts.
- Set a command timeout (e.g., 15 seconds).
- Log the command, exit code, stdout, and stderr.

## Troubleshooting

| Symptom | Check |
|---|---|
| Nothing changes | Backend detection; run Test; check logs |
| KDE changes all monitors to one image | `desktop.screen` mapping; use Display Layout reorder |
| GNOME only changes one image | GNOME limitation: one wallpaper for all displays |
| Command not found | Install the required tool or use a custom command |
| Works in terminal, not in app | Environment variables (`DBUS_SESSION_BUS_ADDRESS`, `DISPLAY`, `WAYLAND_DISPLAY`) |
| Wallpaper resets after login | Start Rimalux after login (Phase 2 autostart) |
| Flatpak/Snap build cannot change wallpaper | Sandbox restriction; use deb/rpm |
