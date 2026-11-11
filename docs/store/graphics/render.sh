#!/usr/bin/env bash
# Renders Beacon's Play Store icon and feature graphic from the HTML in src/,
# at their exact pixel sizes, with headless Google Chrome (spec section 6).
#   icon-512.png          512 x 512 32-bit PNG (opaque alpha channel), as Play asks
#   feature-1024x500.jpg  1024 x 500 JPEG, no transparency
# Run from anywhere: bash docs/store/graphics/render.sh
set -euo pipefail

# Work from this script's folder, whatever the caller's folder.
cd "$(dirname "$0")"

CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
if [[ ! -x "$CHROME" ]]; then
  echo "Google Chrome not found at: $CHROME" >&2
  exit 1
fi

# One headless capture at device scale 1, so CSS pixels are image pixels.
capture() { # $1 source html, $2 width, $3 height, $4 output png, $5... extra flags
  local src="$1" w="$2" h="$3" out="$4"; shift 4
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars \
    --force-device-scale-factor=1 --window-size="$w,$h" "$@" \
    --screenshot="$PWD/$out" "file://$PWD/$src" >/dev/null 2>&1
}

# The icon: Chrome writes a fully opaque page as 24-bit RGB, and Play asks
# for a 32-bit PNG, so png_add_alpha.py adds an opaque alpha channel.
capture src/icon.html 512 512 icon-tmp.png
python3 -I png_add_alpha.py icon-tmp.png icon-512.png
rm -f icon-tmp.png

# The feature graphic: render to a temporary PNG, then convert to JPEG.
capture src/feature.html 1024 500 feature-tmp.png
sips -s format jpeg -s formatOptions 92 feature-tmp.png --out feature-1024x500.jpg >/dev/null
rm -f feature-tmp.png

echo "Wrote icon-512.png and feature-1024x500.jpg in $PWD"
