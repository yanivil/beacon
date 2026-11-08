#!/bin/bash
# Step 1, 60-minute run: waits for the owner to unlock the phone, then starts the foreground watcher
# and the camera light at Fast for 60 minutes. The app logs battery, temperature and thermal status
# every minute, so the run needs no cable. Android won't deliver a command to the app's screen while
# the phone is locked. Gives up after 30 minutes rather than leaving a loop running.
set -u
export LC_ALL=C
PKG=dev.beacon.spike
log() { adb shell run-as "$PKG" cat files/spike.log 2>/dev/null | tr -d '\r'; }
for _ in $(seq 1 360); do
  if ! adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -a -q 'isKeyguardShowing=true'; then
    adb shell run-as "$PKG" rm -f files/spike.log
    adb shell am start-foreground-service -n "$PKG/.WatchService" 2>&1 | tr -d '\r' | cut -c1-100
    adb shell am start -n "$PKG/.MainActivity" --es cmd config --ez armed true --es route ACTIVITY \
      --es host LISTENER --es preset MEDIUM --ez torch true --ei seconds 6 >/dev/null 2>&1
    sleep 3
    adb shell am start -n "$PKG/.MainActivity" --es cmd run60 >/dev/null 2>&1
    sleep 5
    adb shell input keyevent KEYCODE_HOME
    echo "started at $(date +%H:%M:%S)"
    echo "listener live: $(adb shell dumpsys notification 2>/dev/null | tr -d '\r' | grep -a -c -E "ComponentInfo\{$PKG/$PKG.AlertListener\} \(user")"
    log | grep -a -E 'watch\.|config\.saved|service\.|alert\.started|torch\.(started|commandError)' | cut -c1-200
    exit 0
  fi
  sleep 5
done
echo "Gave up: the phone stayed locked for 30 minutes."
exit 5
