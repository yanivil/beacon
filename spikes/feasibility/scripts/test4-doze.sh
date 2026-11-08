#!/bin/bash
# Step 1, test 4: alerts with the screen off, Doze forced on and battery optimisation on.
# Uses Android's documented Doze test commands. "battery unplug" only makes Android behave as if the
# cable were out, because Doze won't start while charging. The trap undoes both on any exit.
# Part 1 tries both places the alert can run. Part 2 re-enters Doze and waits up to 10 minutes for
# the owner's real SMS and WhatsApp message.
# Warning: on the Redmi 15C 5G (HyperOS) "force-idle" never reached deep Doze, and a simulated
# unplug doesn't start HyperOS's own app freezer. Only a real cable-out test counts there
# (docs/feasibility.md, test 4).
# Usage: test4-doze.sh
set -u
export LC_ALL=C
PKG=dev.beacon.spike

restore() {
  adb shell dumpsys deviceidle unforce >/dev/null 2>&1
  adb shell dumpsys battery reset >/dev/null 2>&1
  echo "restored: normal Doze and battery reporting"
}
trap restore EXIT

log() { adb shell run-as "$PKG" cat files/spike.log 2>/dev/null | tr -d '\r'; }
sleep_and_idle() {
  adb shell input keyevent KEYCODE_SLEEP
  sleep 4
  adb shell dumpsys deviceidle force-idle 2>&1 | tr -d '\r' | tail -1
  echo "deep state: $(adb shell dumpsys deviceidle get deep | tr -d '\r')"
}
post() {
  adb shell "cmd notification post -t BeaconTest beacon-test '$1'" >/dev/null
}

MARK=$(log | wc -l | tr -d ' ')
echo "battery optimisation exempt: $(adb shell dumpsys deviceidle whitelist 2>/dev/null | tr -d '\r' | grep -a -c "$PKG")"
adb shell dumpsys battery unplug

echo "##### part 1a: alert runs inside the listener, screen blink direct"
sleep_and_idle
post 'armed=true route=ACTIVITY host=LISTENER preset=FAST torch=true seconds=6'
sleep 12

echo "##### part 1b: alert runs in a foreground service started from the listener, light only"
sleep_and_idle
post 'armed=true route=NONE host=SERVICE preset=FAST torch=true seconds=6'
sleep 12

echo "##### part 1 log"
log | tail -n +"$((MARK + 1))" | grep -a -E 'alert\.|service\.|blink\.(created|resumed|finished)|torch\.(started|stopped|commandError|unavailable)|notification\.posted' \
  | sed -E 's/ startState=.*//; s/ (pkg|category|channel|template|ongoing|fullScreen|summary|callType|callIsVideo|actions|titleLen|textLen|callText)=[^ ]*//g; s/ (tempC|plugged|thermal|powerSave)=[^ ]*//g' | cut -c1-240

echo "##### part 2: waiting for real SMS and WhatsApp in Doze"
MARK2=$(log | wc -l | tr -d ' ')
sleep_and_idle
for _ in $(seq 1 60); do
  sleep 10
  NEW=$(log | tail -n +"$((MARK2 + 1))")
  SMS=$(echo "$NEW" | grep -a -c 'notification.posted source=SMS')
  WA=$(echo "$NEW" | grep -a -c 'notification.posted source=WHATSAPP_MESSAGE')
  if [ "$SMS" -ge 1 ] && [ "$WA" -ge 1 ]; then break; fi
  # An alert wakes the phone out of Doze, so put it back once the alert has ended.
  if ! echo "$NEW" | tail -40 | grep -a -q 'alert.started' || echo "$NEW" | tail -1 | grep -a -q -E 'blink.finished|torch.stopped|alert.stopped'; then
    if [ "$(adb shell dumpsys deviceidle get deep | tr -d '\r')" != "IDLE" ]; then
      echo "$(date +%H:%M:%S) back into Doze"
      sleep_and_idle >/dev/null
    fi
  fi
done
sleep 10
echo "##### part 2 log"
log | tail -n +"$((MARK2 + 1))" | grep -a -E 'notification\.posted|alert\.(fired|skipped|started|stopped)|service\.|blink\.(created|resumed|finished)|torch\.(started|stopped|commandError|unavailable)|listener\.' \
  | sed -E 's/ startState=.*//; s/ (pkg|category|channel|template|ongoing|fullScreen|summary|callType|callIsVideo|actions|titleLen|textLen|callText)=[^ ]*//g; s/ (tempC|plugged|thermal|powerSave)=[^ ]*//g' | cut -c1-240
