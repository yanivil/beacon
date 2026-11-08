#!/bin/bash
# Waits for the owner to unlock the phone, then arms real alerts for test 1. Android won't deliver
# a command to the app's screen while the phone is locked, and only the owner can unlock it.
# Gives up after 15 minutes rather than leaving a loop running.
set -u
export LC_ALL=C
PKG=dev.beacon.spike
for _ in $(seq 1 180); do
  if ! adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -a -q 'isKeyguardShowing=true'; then
    adb shell am start -n "$PKG/.MainActivity" --es cmd config --ez armed true --es route ACTIVITY \
      --es host LISTENER --es preset MEDIUM --ez torch true --ei seconds 6 >/dev/null 2>&1
    sleep 3
    adb shell input keyevent KEYCODE_HOME
    sleep 1
    echo "listener live: $(adb shell dumpsys notification 2>/dev/null | tr -d '\r' | grep -a -c -E "ComponentInfo\{$PKG/$PKG.AlertListener\} \(user")"
    adb shell run-as "$PKG" cat files/spike.log | tr -d '\r' | grep -a -E 'config.saved|listener\.' | tail -3 | cut -c1-170
    exit 0
  fi
  sleep 5
done
echo "Gave up: the phone stayed locked for 15 minutes."
exit 5
