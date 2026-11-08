# Step 1: feasibility results

Step 1 proves, on a real phone and with the smallest possible code, that
Beacon's core idea works before any screens are built. This page records what
works, which permissions each part needs, the Android and Google Play limits,
and what was not tested.

Every result is labelled **"Tested on <phone>, Android <version>"** or
**"Not tested"**. "Should work" counts as not tested.

## Test phone

| | |
|---|---|
| Phone | Redmi 15C 5G, model 2508CRN2BE (MediaTek MT6835), 6,000 mAh battery |
| System | HyperOS OS3.0.306.0.WPOMIXM, Android 16 (API 36), build BP2A.250605.031.A3, security patch 2026-10-01, Hebrew locale |
| Default apps | Google Phone (`com.google.android.dialer`), Google Messages (`com.google.android.apps.messaging`) |
| How the test app got there | Installed over USB with `adb install`, not from Google Play |
| Dates | 6 to 8 November 2026 |

## Summary

| # | What | Result | Label |
|---|---|---|---|
| 1 | Detect an SMS | Works, heard 0.26 to 0.30 s after the phone posted it | Tested on Redmi 15C 5G, Android 16 |
| 1 | Detect a WhatsApp message | Works, heard 0.27 to 0.30 s after the phone posted it | Tested on Redmi 15C 5G, Android 16 |
| 1 | Detect Telegram messages | Dropped from Step 1 by the owner. Built in Step 2 without a test. | Not tested |
| 1 | Detect phone calls, WhatsApp voice and video calls | Out of Beacon's scope (owner's decision, 8 November 2026) | Not tested |
| 2 | Camera light at Slow 30, Medium 60, Fast 90 | Works, measured 30.0, 60.0 and 90.1 blinks a minute, never more than 2 flashes in any second | Tested on Redmi 15C 5G, Android 16 |
| 2 | Camera light at Fast for 60 minutes | Works. Battery 100% to 96%, 29 to 30 °C, no thermal warning | Tested on Redmi 15C 5G, Android 16 |
| 3 | Full-screen blink over another app | Works with "Display over other apps", blocked without it | Tested on Redmi 15C 5G, Android 16 |
| 3 | Full-screen blink on the lock screen, screen off | Works with "Display over other apps" (wakes the screen), blocked without it | Tested on Redmi 15C 5G, Android 16 |
| 4 | Alerts with the screen off, on battery, HyperOS default battery setting | **Fails**: HyperOS freezes the app and alerts wait until the phone is charged, even with a foreground service | Tested on Redmi 15C 5G, Android 16 |
| 4 | Same, with the app's HyperOS battery setting at "No restrictions" | Works in deep Doze, with or without a foreground service | Tested on Redmi 15C 5G, Android 16 |

**In one sentence:** Beacon works on this phone if the person grants three
things: notification access, "Display over other apps", and the HyperOS
battery setting "No restrictions". Without the third, alerts don't arrive
while the phone sleeps.

## Permissions each part needs

| Part | Needs | Notes |
|---|---|---|
| Detecting messages | **Notification access** (`NotificationListenerService`) | Granted by the person in Settings. No SMS, call log or phone permissions are used. |
| Camera light | Nothing | `CameraManager.setTorchMode` needs no `CAMERA` permission. |
| Full-screen blink from the background | **Display over other apps** (`SYSTEM_ALERT_WINDOW`) | Without it Android blocks the blink screen, and only the camera light flashes. |
| Alerts while the phone sleeps (HyperOS) | **Battery saver: "No restrictions"** for the app | On this phone this one setting also puts the app on Android's own battery-optimisation exemption list. |
| Keeping the CPU awake during an alert | `WAKE_LOCK` (normal permission) | Granted at install. |
| Showing the app's own notifications | `POST_NOTIFICATIONS` | Only needed if Beacon shows a notification of its own. |
| Not needed on this phone | `FOREGROUND_SERVICE_SPECIAL_USE`, `USE_FULL_SCREEN_INTENT` | See test 4 and test 3. Both carry Google Play review costs. |

## Test 1: detecting messages

The test app's notification listener logs only the structure of each
notification (app, category, channel, layout, timing). It never logs message
text or titles.

| Message | Channel and layout | Heard after | Conditions |
|---|---|---|---|
| SMS (Google Messages) | `bugle_default_channel`, `category=msg`, MessagingStyle | 263 to 297 ms | Unlocked, locked, and in deep Doze |
| WhatsApp message | `individual_chat_defaults_4` or `group_chat_defaults_5`, MessagingStyle | 265 to 295 ms | Unlocked, locked, and in deep Doze |
| WhatsApp group summary | Same channel, `category=msg`, group summary | About 300 to 550 ms | Skipped, so each message alerts once |

What the real app must handle, found during the tests:

- **False alerts from WhatsApp's chat backup.** WhatsApp's nightly backup
  posts an ongoing progress notification (channel `chat_history_backup@1`, no
  message layout) and updates it several times a second. The test app treated
  each update as a message. It flashed 10 times at 02:36 on 7 November and
  6 times at 05:14 on 8 November. Beacon must ignore ongoing notifications,
  must alert only for message notifications, and must alert only once per
  notification, not on every update.
- **WhatsApp "silent" notifications.** WhatsApp posts some messages on a
  channel called `silent_notifications_6`. They have the same structure as
  its other messages (MessagingStyle, not ongoing), so they are received
  messages, and Beacon flashes for them.
- **Messages arriving together.** When several notifications arrive at once,
  the test app ran one alert and ignored the rest while it was running.

## Test 2: camera light

Measured by the app from the light's own on/off callbacks (screen on, app
open, on the charger). The owner confirmed the light blinked.

| Preset | Measured | Gap between changes | Most flashes in any second | Errors |
|---|---|---|---|---|
| Slow 30 | 30.0 a minute | 974 to 1,005 ms | 1 | 0 |
| Medium 60 | 60.0 a minute | 495 to 505 ms | 2 | 0 |
| Fast 90 | 90.1 a minute | 324 to 340 ms | 2 | 0 |

All three stay under WCAG 2.3.1's limit of 3 flashes a second.

### 60 minutes at Fast

Run from 18:46:35 to 19:46:35 on 6 November. The cable was out from 18:48:12,
so 58 of the 60 minutes were on battery. The phone's screen timeout was set to
"Never", so the screen stayed on and unlocked the whole hour. That uses more
battery and makes more heat than real use, so these figures are a worst case.

| What | Result |
|---|---|
| Light timing | 5,406 flashes, 90.1 a minute, gaps 322 to 343 ms, at most 2 flashes in any second, 0 errors |
| Battery | 100% to 96%. The charge counter shows 240 mAh used in the first 53 minutes. |
| Cost of the light alone | About 130 mAh, or 2% of the battery, per hour. The screen alone used 144 mAh an hour over the next 15 hours. |
| Temperature | Battery 29 to 30 °C for the whole hour |
| Overheating | Android's thermal status stayed at "none" and never changed. No warning was logged. |

## Test 3: full-screen blink

Each route was triggered from the background by a test notification, with a
15-second wait first, so Android's short "recently in the foreground"
allowance had expired. Medium preset, on the charger. HyperOS's own pop-up and
lock-screen settings were left untouched.

| Route | Over another app | On the lock screen, screen off |
|---|---|---|
| **Start the blink screen directly** (with "Display over other apps") | Yes, full screen in 0.14 s | Yes in 0.09 s, and it woke the screen |
| Start the blink screen directly, **without** "Display over other apps" | **No.** Android logged `Background activity launch blocked! ... (BAL_BLOCK)`. The light still flashed. | **No**, same block. The light still flashed. |
| Full-screen notification (`USE_FULL_SCREEN_INTENT`) | No, only a banner | Yes in 0.51 s |
| Overlay window (`TYPE_APPLICATION_OVERLAY`) | Partly: see-through, and the status and navigation bars stay uncovered | No |

The screen blink measured 60.0 a minute with at most 2 flashes in any second.

**Recommended route:** start the blink screen directly, with "Display over
other apps".

Found along the way:

- A leftover full-screen-notification banner opened the blink about 40 seconds
  later, once the phone locked. If Beacon ever uses that route, it must clear
  the notification when the alert ends.
- Once, the blink screen was rebuilt twice during one alert (22:11:57 and
  22:11:58 on 7 November), so it blinked for about 12 seconds instead of 6.
  The log doesn't show why. Beacon should keep the alert's timing outside the
  screen, so rebuilding the screen can't restart it.
- The app's own report can be wrong: once it said an overlay was shown when a
  screenshot showed only the lock screen. The tests trust screenshots and the
  owner's eyes over the app's own report.

## Test 4: screen off, on battery

The key test. Every run below used a locked phone with the screen off.

| Run | Battery | HyperOS battery setting | Foreground service | Result |
|---|---|---|---|---|
| a | Simulated (`dumpsys battery unplug`) | Default | No | Held 86 to 141 s, until charging or the screen came back |
| b | Simulated | Default | Yes | Heard in 0.23 s. **Misleading**, see below. |
| c | Real, cable out 2 h 27 min, deep Doze | Default | Yes | **Fails.** Held until the phone was plugged in |
| d | Real, cable out 22 min, deep Doze | No restrictions | Yes | **Works.** SMS 0.30 s, WhatsApp 0.27 s |
| e | Real, cable out 11 h 57 min, deep Doze | No restrictions | No | **Works.** WhatsApp 0.30 s, SMS 0.28 s |

### Run c: why the default setting fails

Timeline on 7 November, from the phone's battery history and system log:

| Time | What happened |
|---|---|
| 19:02:00 | Unplugged |
| 19:05:44 | Screen off |
| 19:05:51 | HyperOS marked the app for freezing: `delayFreeze true reason= screen off uid=10291` |
| 19:08:36 | Deep Doze |
| 21:28:14 to 21:28:40 | WhatsApp and Google Messages posted the owner's two WhatsApp messages and SMS. The test app heard nothing. |
| 21:28:59 | Plugged in |
| 21:29:00.511 | HyperOS thawed exactly two apps, WhatsApp and the test app: `THAW uid = 10291 pid = [ 21991 ] reason : enable:28-thawAll` |
| 21:29:00.788 | The test app heard all three messages at once, 20 to 46 s late |

HyperOS has its own app freezer (`GreezeManager`). On battery with the screen
off, it froze the test app even though the app had a foreground service
running. Stock Android doesn't freeze apps that have a foreground service.
The messages reached the phone on time: WhatsApp was thawed for its push
message and posted its notification. Only the test app didn't hear it. The
log shows when the app was marked for freezing and that it was frozen at
21:29:00, but not the exact moment it froze.

Run b passed only because a simulated unplug doesn't start HyperOS's freezer.
**On HyperOS, only a real cable-out test counts.** Android's own Doze test
command (`dumpsys deviceidle force-idle`) also failed on this phone. It
stopped at `QUICK_DOZE_DELAY` and never reached deep Doze.

### Runs d and e: "No restrictions"

With the app's HyperOS battery setting at "No restrictions", the system log
shows no freeze of the test app in either run. At the end of run d, HyperOS
thawed only WhatsApp when the phone was plugged in. The app's own record
shows `idle=true` (deep Doze) at the moment each message was heard. The owner
saw the screen and the camera light flash for the messages in both runs.

Run e had no foreground service at all, and no permanent notification. The
same app process stayed alive from 6 November 18:37 through the whole test.
In run e the whole phone used 177 mAh (3%) in 11 hours 46 minutes on battery,
including 8 alerts. That's the phone's total standby use, not Beacon's own
share.

**So on this phone, Beacon doesn't need a foreground service**, as long as
"No restrictions" is on. That avoids a permanent notification and Google
Play's review of the `specialUse` service type. The open risk is what happens
if the process dies (see "Not tested").

### Other HyperOS behaviour

- **Autostart.** With Autostart off (the default), HyperOS refused to
  reconnect the notification listener after the app was reinstalled
  (`AutStart Unable to bind notification listener service`). Android retried
  three times in 10 seconds, then gave up. `requestRebind()` didn't help.
  Turning the listener component off and on again from inside the app did
  reconnect it within about a second, whether from the app's screen or from a
  service on a locked phone.
- **Restricted settings.** Granting notification access to the adb-installed
  test app showed no "restricted setting" block.

## Android and Google Play limits

| Rule | What it says | What it means for Beacon |
|---|---|---|
| [Full-screen notifications](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en) ([Android limits](https://source.android.com/docs/core/permissions/fsi-limits)) | Granted by default only to apps whose core function is alarms or calls. Others must ask the person, and declare it in Play Console. | Beacon handles no calls or alarms, so it won't get this by default. The direct route doesn't need it. |
| [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types) | `specialUse` is allowed but reviewed: a description, the impact if the work is deferred, and a demo video. The [abuse policy](https://support.google.com/googleplay/android-developer/answer/16559646) wants it user-started and visible. | Run e shows Beacon doesn't need one on this phone. The approval bar isn't published. |
| [SMS and call log permissions](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en) | Only default SMS or phone apps may use them. | Beacon doesn't need them. Notification access covers detection. |
| [Starting activities from the background](https://developer.android.com/guide/components/activities/background-starts) | Blocked, except for listed exemptions such as a granted "Display over other apps". | Matches test 3: the blink screen needs that permission. Android 10 Go phones can't grant it. |
| [Foreground services from the background](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) ([Android 15 changes](https://developer.android.com/about/versions/15/behavior-changes-15)) | Since Android 15, "Display over other apps" only helps while an overlay is actually showing. | Only matters if Beacon uses a foreground service. |
| [Sensitive permissions](https://support.google.com/googleplay/android-developer/answer/16558241) | No rule names notification access. Sensitive access must serve the app's core function, with clear consent. | **Not confirmed as allowed.** It needs a clear explanation to the person, and a check before publishing. |
| [Restricted settings](https://support.google.com/android/answer/12623953?hl=en) ([CDD](https://source.android.com/docs/compatibility/15/android-15-cdd)) | For sideloaded apps, Android can block sensitive settings, including notification access, until the person allows them. | Not seen on this phone. A Play install isn't affected. |
| [Sensitive notification content](https://developer.android.com/about/versions/15/behavior-changes-all) | Since Android 15, untrusted listeners don't see one-time codes. | Probably harmless, since Beacon only needs to know a message arrived. Not tested. |
| [Target API level](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en) | New apps must target API 36 or higher since 31 August 2026. | `targetSdk 36` meets it. |

## Owner's decisions (8 November 2026)

- **Phone calls and WhatsApp video calls are dropped from Beacon**, even
  though the design shows them. Beacon alerts for messages only.
- **Flash only when a message is received.** Never for backups, progress
  bars or any other app activity.
- **Telegram stays, for messages only.** Its row is built in Step 2 without
  a Step 1 test, so Telegram detection stays "Not tested" until it's tried
  on a phone.
- **"How long to flash" defaults to 1 minute.** The 6-second alerts in these
  tests were only for testing.
- **Headings follow the project rules** (at least 28sp and bold), so the
  design's 20px uppercase "VISUAL ALERTS" and "TEST ALERT" headings are
  enlarged in Step 2.

## Requirements this adds for Step 2

1. **Setup checks three things and shows each as ON or OFF:** notification
   access, "Display over other apps", and the battery setting "No
   restrictions" (with a guide for HyperOS). On this phone Android reports
   the battery setting through `isIgnoringBatteryOptimizations`. Setup
   explains, in plain words, why each one is needed.
2. **Alert only for received messages:** a notification counts only if it
   uses the message layout (MessagingStyle, or category `msg` when there's
   no layout), isn't ongoing, isn't a group summary, and hasn't already been
   alerted for. That rules out WhatsApp's backup progress, group summaries
   and repeated updates.
3. **One alert at a time:** notifications that arrive during an alert don't
   start another one.
4. **Keep the alert's timing outside the blink screen**, so rebuilding the
   screen can't restart or lengthen it.
5. **Reconnect the listener** when the app opens, if the phone has dropped it.
6. **Show the blink screen directly**, not through a full-screen
   notification or an overlay window.

## Not tested

- Telegram messages (dropped from Step 1 by the owner, built in Step 2
  anyway). Phone calls and WhatsApp calls are out of Beacon's scope.
- Restarting the phone. With Autostart off, Beacon may not come back until
  it's opened.
- Swiping the app away from recent apps, or the system killing it for memory.
- Any other phone, brand or Android version (Android 10 to 15, Samsung,
  Pixel).
- Installing from Google Play.
- An SMS with a one-time code (Android 15+ hides those from listeners).
- The default HyperOS battery setting without a foreground service on real
  battery. Run c failed even with one, so it's expected to fail.
- Beacon's own battery use apart from the rest of the phone.

## Open questions before Step 2

None. The owner's answers are under "Owner's decisions" above.

## How to repeat the tests

The test app is in [`spikes/feasibility/`](../spikes/feasibility). It's a
throwaway: Views instead of Compose, no libraries, package `dev.beacon.spike`.
It is not part of Beacon.

```bash
cd spikes/feasibility && ./gradlew assembleDebug
```

```bash
adb install -r spikes/feasibility/build/outputs/apk/debug/beacon-feasibility-spike-debug.apk
```

Then open "Beacon test" on the phone and grant the permissions with its
buttons. The scripts in [`spikes/feasibility/scripts/`](../spikes/feasibility/scripts)
drive each test over USB. The app writes its log to its private storage:

```bash
adb shell run-as dev.beacon.spike cat files/spike.log
```

A test notification that works even on a locked phone:

```bash
adb shell "cmd notification post -t BeaconTest beacon-test 'armed=true route=ACTIVITY host=LISTENER preset=MEDIUM torch=true seconds=6'"
```

Test 4 has no script: lock the phone, unplug it, wait at least 20 minutes,
send the messages from a second phone, then plug it back in and read the log,
`dumpsys batterystats --history` (screen, charging and `device_idle` changes)
and `logcat` (lines from `GreezeManager`).
