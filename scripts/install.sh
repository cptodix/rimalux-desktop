#!/usr/bin/env bash
# Rimalux Desktop installer for Linux.
#
#   curl -fsSL https://raw.githubusercontent.com/cptodix/rimalux-desktop/main/scripts/install.sh | bash
#
# Optional:
#   RIMALUX_VERSION=0.1.2   install a specific version
#   RIMALUX_BIN_DIR=...     where the launcher symlink goes (default ~/.local/bin)
set -euo pipefail

REPO="${RIMALUX_REPOSITORY:-cptodix/rimalux-desktop}"
VERSION="${RIMALUX_VERSION:-${1:-latest}}"
DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
APP_DIR="$DATA_HOME/rimalux"
APP_IMAGE="$APP_DIR/rimalux.AppImage"
BIN_DIR="${RIMALUX_BIN_DIR:-$HOME/.local/bin}"
ICON_DIR="$DATA_HOME/icons/hicolor/256x256/apps"
DESKTOP_DIR="$DATA_HOME/applications"

fail() {
    echo "error: $*" >&2
    exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v tar >/dev/null 2>&1 || true

case "$(uname -m)" in
    x86_64 | amd64) ;;
    *) fail "only x86_64 AppImages are published right now (found $(uname -m))" ;;
esac

if [ "$VERSION" = "latest" ]; then
    API="https://api.github.com/repos/$REPO/releases/latest"
else
    API="https://api.github.com/repos/$REPO/releases/tags/v${VERSION#v}"
fi

echo "Resolving Rimalux release ($VERSION)…"
RELEASE_JSON="$(curl -fsSL -H 'Accept: application/vnd.github+json' \
    -H 'User-Agent: Rimalux-Installer' "$API")" || fail "could not reach the GitHub release API"

TAG="$(printf '%s' "$RELEASE_JSON" |
    sed -n 's/.*"tag_name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
ASSET_URL="$(printf '%s' "$RELEASE_JSON" |
    grep -o '"browser_download_url"[[:space:]]*:[[:space:]]*"[^"]*\.AppImage"' |
    head -1 | sed 's/.*"\(https[^"]*\)"$/\1/')"

[ -n "$TAG" ] || fail "release has no tag"
[ -n "$ASSET_URL" ] || fail "release $TAG has no AppImage asset"

echo "Downloading $TAG…"
mkdir -p "$APP_DIR" "$BIN_DIR" "$ICON_DIR" "$DESKTOP_DIR"
TMP="$APP_DIR/.rimalux.AppImage.part"
trap 'rm -f "$TMP"' EXIT
curl -fL --progress-bar -o "$TMP" -H 'User-Agent: Rimalux-Installer' "$ASSET_URL"
chmod 755 "$TMP"
mv -f "$TMP" "$APP_IMAGE"
trap - EXIT

ln -sfn "$APP_IMAGE" "$BIN_DIR/rimalux"

ICON_URL="https://raw.githubusercontent.com/$REPO/$TAG/desktopApp/packaging/rimalux.png"
if curl -fsSL -o "$ICON_DIR/rimalux.png" "$ICON_URL" 2>/dev/null; then
    command -v gtk-update-icon-cache >/dev/null 2>&1 &&
        gtk-update-icon-cache -f -t "$DATA_HOME/icons/hicolor" >/dev/null 2>&1 || true
fi

cat > "$DESKTOP_DIR/rimalux.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Rimalux
Comment=Procedural OLED wallpaper studio
Exec=$APP_IMAGE %U
Icon=rimalux
Terminal=false
Categories=Graphics;
StartupWMClass=Rimalux
EOF

command -v update-desktop-database >/dev/null 2>&1 &&
    update-desktop-database "$DESKTOP_DIR" >/dev/null 2>&1 || true

echo
echo "Rimalux $TAG installed:"
echo "  app:     $APP_IMAGE"
echo "  launcher: $BIN_DIR/rimalux"
echo "  menu:     $DESKTOP_DIR/rimalux.desktop"

case ":$PATH:" in
    *":$BIN_DIR:"*) ;;
    *) echo
       echo "note: $BIN_DIR is not on PATH; run $APP_IMAGE directly or add it to PATH." ;;
esac
