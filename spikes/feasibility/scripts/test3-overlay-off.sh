#!/bin/bash
# Step 1, extra check: does the blink screen still open from the background without the
# "Display over other apps" permission? Android lists that permission as an exemption from its
# background-start block, so without it the screen blink may be refused while the light still works.
# Case A: phone unlocked, another app in front. Case B: screen off and locked.
# The phone stays on the charger: this check is about the background-start rule, not about sleep.
# Aborts if the permission is still on, so a pass can't come from the wrong setup.
# Usage: test3-overlay-off.sh <evidence-dir>
set -u
export LC_ALL=C
PKG=dev.beacon.spike
OUT="$1"
mkdir -p "$OUT"

log() { adb shell run-as "$PKG" cat files/spike.log 2>/dev/null | tr -d '\r'; }
overlay() { adb shell appops get "$PKG" SYSTEM_ALERT_WINDOW | tr -d '\r'; }
# The whole command is quoted so the phone's shell, not the Mac's, sees the format as one argument.
phone_time() { adb shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r'; }

echo "overlay: $(overlay)"
if overlay | grep -q 'allow'; then
  echo "ABORT: the overlay permission is still on."
  exit 4
fi
echo "listener live: $(adb shell dumpsys notification | tr -d '\r' | grep -a -c -E "ComponentInfo\{$PKG/$PKG.AlertListener\} \(user")"

# Posts one test notification and reports what the app and Android did with it.
fire() {
  local label="$1" tag="$2" mark since
  mark=$(log | wc -l | tr -d ' ')
  since=$(phone_time)
  echo "=== $label"
  echo "  in front: $(adb shell dumpsys activity activities | tr -d '\r' | grep -a -m1 -E 'mResumedActivity|topResumedActivity' | grep -a -o -E '[a-z0-9.]+/[A-Za-z0-9.$]+' | head -1)"
  echo "  locked: $(adb shell dumpsys window | tr -d '\r' | grep -a -o -E 'isKeyguardShowing=[a-z]+' | head -1)"
  adb shell "cmd notification post -t BeaconTest $tag 'armed=true route=ACTIVITY host=LISTENER preset=MEDIUM torch=true seconds=6'" >/dev/null
  sleep 2
  adb exec-out screencap -p > "$OUT/$tag.png"
  sleep 7
  log | tail -n +"$((mark + 1))" | grep -a -E 'notification.posted|alert.fired|blink.created|blink.finished|torch.stopped' \
    | sed -E 's/ (pkg|category|channel|template|ongoing|fullScreen|summary|callType|callIsVideo|actions|titleLen|textLen|callText|tempC|thermal|powerSave|battery)=[^ ]*//g' \
    | cut -c1-170 | sed -E 's/^/  /'
  echo "  Android's background-start lines:"
  adb logcat -d -v time -T "$since" 2>/dev/null | tr -d '\r' \
    | grep -a -i -E 'background activity|abort background|BAL_|balAllow|bal_' | grep -a -E "$PKG|10291" \
    | cut -c1-220 | sed -E 's/^/    /' | head -6
}

fire "A: unlocked, another app in front" beacon-overlay-a
echo "turning the screen off"
adb shell input keyevent KEYCODE_SLEEP
sleep 6
fire "B: screen off and locked" beacon-overlay-b
