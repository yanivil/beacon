#!/bin/bash
# Step 1, test 4: measures how late the listener hears a notification with the screen off on battery,
# with and without the foreground service. Uses test notifications, which took the same path and
# showed the same delay as the owner's real SMS and WhatsApp message.
# "battery unplug" makes Android behave as if the cable were out, so adb stays connected. A pass here
# still has to be confirmed with the cable really out. The trap undoes it on any exit.
# Warning: on the Redmi 15C 5G (HyperOS) this script's pass with the foreground service was wrong.
# A simulated unplug doesn't start HyperOS's own app freezer, and the real cable-out test failed
# (docs/feasibility.md, test 4).
# Usage: test4-probe.sh <apk>
set -u
export LC_ALL=C
APK="$1"; PKG=dev.beacon.spike; WATCH="$PKG/.WatchService"

restore() {
  adb shell dumpsys battery reset >/dev/null 2>&1
  echo "restored: normal battery reporting"
}
trap restore EXIT

log() { adb shell run-as "$PKG" cat files/spike.log 2>/dev/null | tr -d '\r'; }
live() { adb shell dumpsys notification 2>/dev/null | tr -d '\r' | grep -a -c -E "ComponentInfo\{$PKG/$PKG.AlertListener\} \(user"; }
proc() {
  local pid; pid=$(adb shell pidof "$PKG" | tr -d '\r')
  echo "  process: pid=$pid $(adb shell dumpsys activity processes "$PKG" 2>/dev/null | tr -d '\r' | grep -a -E 'curProcState=' | head -1 | sed -E 's/^ +//; s/ mMaxProcState.*//') $(adb shell cat /proc/"$pid"/cgroup 2>/dev/null | tr -d '\r' | grep -a cpuset)"
}

# Screen off on battery, wait, post one test notification, then see when the listener hears it.
probe() {
  local label="$1" wait="$2" mark
  mark=$(log | wc -l | tr -d ' ')
  echo "=== $(date +%H:%M:%S) $label: screen off on battery, waiting ${wait}s"
  adb shell dumpsys battery unplug
  adb shell input keyevent KEYCODE_SLEEP
  sleep "$wait"
  proc
  adb shell "cmd notification post -t BeaconTest beacon-test 'armed=true route=NONE host=LISTENER preset=FAST torch=true seconds=3'" >/dev/null
  sleep 15
  local heard
  heard=$(log | tail -n +"$((mark + 1))" | grep -a 'notification.posted')
  if [ -n "$heard" ]; then
    echo "  HEARD with the screen off: $(echo "$heard" | grep -a -o -E 'delayMs=[0-9]+|screenOn=[a-z]+|plugged=[a-z]+|idle=[a-z]+' | tr '\n' ' ')"
    log | tail -n +"$((mark + 1))" | grep -a 'torch.stopped' | sed -E 's/^/  /' | cut -c1-150
  else
    echo "  NOT HEARD 15 s after posting"
  fi
  adb shell dumpsys battery reset
  sleep 5
  if [ -z "$heard" ]; then
    echo "  after restoring charging: $(log | tail -n +"$((mark + 1))" | grep -a 'notification.posted' | grep -a -o -E 'delayMs=[0-9]+' | tr '\n' ' ')"
  fi
  sleep 8
}

adb install -r "$APK" 2>&1 | tail -1
adb shell run-as "$PKG" rm -f files/spike.log
sleep 3

echo "##### can the service start the app and reconnect the listener on a locked phone, Autostart off?"
adb shell am start-foreground-service -n "$WATCH" 2>&1 | tr -d '\r' | cut -c1-150
sleep 6
echo "listener live: $(live)"
log | cut -c1-110
if [ "$(live)" = "0" ]; then
  echo "ABORT: listener not connected."
  log | tail -5
  exit 4
fi

echo "##### baseline: no foreground service"
adb shell am stopservice -n "$WATCH" 2>&1 | tr -d '\r' | cut -c1-120
sleep 4
probe baseline-60s 60

echo "##### with the foreground service"
adb shell am start-foreground-service -n "$WATCH" 2>&1 | tr -d '\r' | cut -c1-120
sleep 4
probe service-60s 60
probe service-180s 180

echo "##### full log"
log | grep -a -E 'watch\.|listener\.|notification\.posted|torch\.(stopped|commandError|unavailable)' \
  | sed -E 's/ (pkg|category|channel|template|ongoing|fullScreen|summary|callType|callIsVideo|actions|titleLen|textLen|callText)=[^ ]*//g; s/ (tempC|thermal|powerSave|batteryOptExempt)=[^ ]*//g' | cut -c1-170
