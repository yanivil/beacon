#!/bin/bash
# Step 1, test 3: tries each route for showing the full-screen blink, triggered from the background
# by a test notification, first over another app and then on the lock screen.
# Waits 15 s before each trigger so Android's short "recently in the foreground" allowance has
# expired and the result reflects a real background start.
# Refuses to run unless the phone is unlocked and the listener is really connected, because without
# the listener no route can fire and the run would prove nothing.
# Usage: test3-fullscreen.sh <evidence-dir> <label-for-this-phase>
set -u
export LC_ALL=C
OUT="$1"; PHASE="$2"; PKG=dev.beacon.spike
mkdir -p "$OUT"

log() { adb shell run-as "$PKG" cat files/spike.log 2>/dev/null | tr -d '\r'; }

state() {
  adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -a -E 'mCurrentFocus' | head -1 | sed -E 's/^ +/  /'
  adb shell dumpsys power 2>/dev/null | tr -d '\r' | grep -a -E 'mWakefulness=' | head -1 | sed -E 's/^ +/  /'
}

run() {
  local label="$1" route="$2"
  echo "=== $(date +%H:%M:%S) $PHASE $label route=$route"
  adb shell "cmd notification post -t BeaconTest beacon-test 'armed=true route=$route host=LISTENER preset=MEDIUM torch=false seconds=8'" >/dev/null
  sleep 3
  adb exec-out screencap -p > "$OUT/$PHASE-$label-$route-1.png"
  adb exec-out screencap -p > "$OUT/$PHASE-$label-$route-2.png"
  state
  sleep 7
}

if adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -a -q 'isKeyguardShowing=true'; then
  echo "ABORT: the phone is locked. It has to be unlocked to start this test."
  exit 3
fi

adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 4
LIVE=$(adb shell dumpsys notification 2>/dev/null | tr -d '\r' | grep -a -c -E "ComponentInfo\{$PKG/$PKG.AlertListener\} \(user")
echo "listener live entries: $LIVE"
if [ "$LIVE" = "0" ]; then
  echo "ABORT: the notification listener is not connected, so no route could fire."
  log | tail -5
  exit 4
fi

echo "##### over another app (Settings), phone unlocked"
adb shell am start -a android.settings.SETTINGS >/dev/null
sleep 15
state
run over ACTIVITY; sleep 15
run over FULL_SCREEN_INTENT; sleep 15
run over OVERLAY; sleep 3

echo "##### lock screen, screen off"
adb shell input keyevent KEYCODE_SLEEP; sleep 15
state
run lockoff ACTIVITY
adb shell input keyevent KEYCODE_SLEEP; sleep 15
run lockoff FULL_SCREEN_INTENT
adb shell input keyevent KEYCODE_SLEEP; sleep 15
run lockoff OVERLAY

echo "##### lock screen showing, screen on"
adb shell input keyevent KEYCODE_SLEEP; sleep 5
adb shell input keyevent KEYCODE_WAKEUP; sleep 3
state
run lockon OVERLAY

echo "##### log"
log | grep -a -E 'listener\.|alert\.(fired|skipped|alreadyRunning|notArmed)|blink\.|overlay\.|fullScreen\.' \
  | sed -E 's/ (tempC|plugged|thermal|idle|powerSave|batteryOptExempt|battery)=[^ ]*//g'
echo "##### system refusals"
adb logcat -d -t 3000 2>/dev/null | tr -d '\r' | grep -a -E "dev\.beacon\.spike" | grep -a -i -E 'blocked|Background activity|BAL|denied|Reject|abort' | tail -12 | cut -c1-200
