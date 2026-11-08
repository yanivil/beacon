#!/bin/bash
# Step 1, test 2: runs the camera light at each preset and prints the app's own measurements.
# Usage: test2-torch.sh <apk>
set -u
APK="$1"
PKG=dev.beacon.spike
adb install -r "$APK" 2>&1 | tail -1
adb shell run-as "$PKG" rm -f files/spike.log
for preset in SLOW MEDIUM FAST; do
  echo "== $preset"
  adb shell am start -n "$PKG/.MainActivity" --es cmd torch --es preset "$preset" --ei seconds 30 2>&1 | grep -E 'Error|Warning' || true
  sleep 33
done
echo "== log"
adb shell run-as "$PKG" cat files/spike.log | tr -d '\r' | grep -E 'torch\.|alert\.(started|stopped)' | sed -E 's/ startState=.*//'
