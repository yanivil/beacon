# The widget's pause on battery: checks

This page records the owner's second cable-out check on the test phone, on
11 November 2026. One tap on the widget set a 1-hour pause just before the
phone came off the cable. The phone then lay locked on battery, mostly in
Doze's deep idle, and then messages came. The page records how alerts and
the widget came back on after the pause (spec section 1, "Done means" 4).
Later that evening a second pause showed a message being held back during
a pause ([below](#a-message-during-a-pause-later-that-evening)). Nothing in
the code changed.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**.

## How it was recorded

The owner ran the check alone: tap the widget, leave the
phone locked on battery for more than an hour, then send a message. They
reported only that it worked. The tap came 6 seconds before the phone left
the cable, and the power button put the phone to sleep a few seconds after
that.

The evidence was then read from the phone over adb, without changing
anything there:

- Android's battery history (`dumpsys batterystats --history`). It starts
  at 19:34:21, when the phone came off the cable.
- Android's log, whose app lines reach back to 19:17:43. It holds Beacon's
  decisions (`D/Beacon`), the launcher's widget tap, the alarm being set,
  Beacon's screens and the widget's redraws.
- Android's alarm records (`dumpsys alarm`), which keep Beacon's last
  removed alarm.
- The times of Beacon's stored files, its stored settings, and its
  Autostart app-op.
- One screenshot of the home screen at 20:47, after the check.

The battery history and the log keep time separately. This time the log
runs about 1 second ahead: the camera light came on 0.98 and 0.97 seconds
later in the log than in the battery history, for the two alerts. In the
table below, times marked † come from the battery history. Every other
time comes from the log, the alarm records or the file times, which share
the log's clock. Delays are measured on one clock.

| | |
|---|---|
| Phone | Redmi 15C 5G (model 2508CRN2BE), Android 16, HyperOS `OS3.0.306.0.WPOMIXM` |
| Build | `main`'s debug build at `d3675d8`: the same install as in the [first cable-out check](2026-11-alerts-on-battery.md), at 15:53:39 |
| Beacon's settings | The defaults: yellow, Medium (60 blinks a minute), 1 minute. Only the pause is stored. |
| Autostart | On (`MIUIOP(10008): allow`) |
| Power | Off the cable from 19:34:21, at 100%. Plugged into a charger at 20:43:49. The level stayed at 100%. |

## What happened

| Time | Event |
|---|---|
| 19:34:15.732 | The widget was tapped, with the phone still on the cable: the launcher sent the tap to Beacon. At 19:34:15.771 Android logged Beacon's resume alarm (`RTC_WAKEUP`), set for 20:34:15.748, one hour on. Beacon sets this alarm for the end of the pause it has just stored, so the stored pause was a 1-hour pause. At 19:34:16.1 the widget was redrawn. |
| 19:34:21 † | The phone came off the cable, at 100%. |
| 19:34:26.7 | The power button put the phone to sleep. The display lit briefly at 19:34:27.3 without the phone waking, and went off for good at 19:34:30.7. The phone stayed locked until 20:43:45. |
| 19:37:09 to 20:37:08 †, and 20:37:38 to 20:43:45 † | Doze's deep idle (`device_idle=full`), the phone's deepest sleep. Between the two stretches came a 30-second maintenance window. The screen waking for the first alert didn't end deep idle; the unlock did. |
| 19:34:21 to 20:34:53 † | No push message reached WhatsApp or Google Messages, and Beacon logged no decision: no message came during the pause. |
| 20:34:15.748 | The pause's end, and the start of the resume alarm's 10-minute window, which runs to 20:44:15.748. |
| 20:34:53.6 † | A push message reached WhatsApp. |
| 20:34:55.797 | Beacon decided `Alert(source=WHATSAPP)`, 40 seconds after the pause's end. Nothing had cleaned up the stored pause yet (that came at 20:34:59.131), so the alert didn't wait for a clean-up. By Beacon's code it checks the pause's end time on each message (rule 8 in `MessageClassifier`). No message came during this pause; a second pause later showed that check holding one back ([below](#a-message-during-a-pause-later-that-evening)). |
| 20:34:55.799 to 20:34:58.732 | Beacon asked for the alert's wake lock 2 ms after deciding, and waited 2.9 seconds for it. Android logged `Excessive delay in acquire_wake_lock(), duration=2932ms`, together with the end of a 2.7-second sleep. |
| 20:34:58.745 | The camera light came on, 4.2 seconds after the push. At 20:34:58.8 the alert screen came to the front and the screen woke. |
| 20:34:59.131 | Beacon's stored pause became ON. At 20:34:59.165 the resume alarm, which hadn't fired, was cancelled (`alarm_cancelled`), and at 20:34:59.620 the widget was redrawn. |
| 20:34:59 to 20:35:31 | Three more WhatsApp messages were decided `Alert` during the alert. As the spec says, they started no second alert. |
| 20:35:58.552 | The alert ended by itself, after 60 flashes in 1 minute. |
| 20:43:20.4 † | A push message reached Google Messages, the SMS app. |
| 20:43:24.590 | Beacon decided `Alert(source=SMS)`, and the camera light came on 14 ms later, 3.2 seconds after the push. The alert screen came to the front. At 20:43:34 a WhatsApp message during it started no second alert. |
| 20:43:38.5 | A tap on the alert screen closed it, after 14 flashes. |
| 20:43:45 † | The phone was unlocked. |
| 20:43:49 † | The phone was plugged into a charger. |
| 20:43:56 | A message that came while the phone was in use was ignored (`IN_USE`), as the owner decided. |
| 20:47 | The home screen's widget showed "Visual alerts" and "ON" ([01](pause-on-battery/01-widget-on-after-pause.png)). |

### What ended the stored pause

Four things in Beacon clean up a timed pause that is over: the resume
alarm, Beacon's Home coming to the front, the screen turning on, and a
time, time-zone or language change. Here it was the screen turning on, for
the alert:

- **Not the resume alarm.** Android's alarm records show it removed with
  `alarm_cancelled` at 20:34:59.165. Its receiver (`ResumeReceiver`)
  appears nowhere in the log, and the battery history has no alarm wake
  lock for Beacon. It never fired.
- **Not Home.** Beacon's Home (`MainActivity`) didn't come to the front
  from the tap until after the check; only the alert screen
  (`AlertActivity`) did.
- **No time, time-zone or language change.** Nothing in the log points to
  one after the tap, and the phone's clock was last set at 15:25:56.

Something did change the stored pause at 20:34:59.131: Beacon's settings
file was written then, and it now holds ON. Jetpack DataStore, which keeps
the settings, skips a write when the new value equals the stored one (from
its source; not seen on the phone). Of the clean-up's rules, only one
changes the stored value: a timed pause that is over becomes ON. The
widget was also redrawn, which the clean-up does only when the stored pause
changed.

## Results

| Check | Result |
|---|---|
| One tap on the widget sets a 1-hour pause | Tested on Redmi 15C 5G, Android 16. The tap set the resume alarm for one hour on, the end of the pause just stored, and the widget was redrawn. |
| Alerts come back on by themselves after the pause, on battery and in Doze | Tested on Redmi 15C 5G, Android 16. The first message after the pause's end flashed, with the phone locked and in deep idle, before anything had cleaned up the stored pause and without the resume alarm. The alert flashed 60 times in its 1 minute, and the screen woke. |
| The widget back to ON after the pause | Tested on Redmi 15C 5G, Android 16, when an alert woke the screen. Less than a second later, the screen-on clean-up stored ON, cancelled the resume alarm, and redrew the widget. At 20:47 it showed ON ([01](pause-on-battery/01-widget-on-after-pause.png)). |
| A later message to the SMS app | Tested on Redmi 15C 5G, Android 16. At 20:43 it flashed, 3.2 seconds after its push. |
| Messages during an alert | Tested on Redmi 15C 5G, Android 16. Three WhatsApp messages during the first alert and one during the second started no second alert, and the first still ended after its 1 minute. |
| A tap ends an alert | Tested on Redmi 15C 5G, Android 16. A tap on the second alert's screen closed it, and the light stopped. |

## Seen once

- **The first alert's light came on 4.2 seconds after its push.** In the
  first cable-out check this took about 2 seconds. Beacon decided
  1.2 seconds after the push and asked for its wake lock 2 ms later, but
  the wake lock took 2.9 seconds, and Android's log ends a 2.7-second sleep
  at the same moment. So the likely cause is the phone going to sleep just
  as Beacon asked.
- **The SMS alert's 3.2 seconds** all came before Beacon decided, with no
  wake-lock wait. Its cause wasn't looked at.

## A message during a pause, later that evening

The owner set a second 1-hour pause and sent messages during it: first
with the phone unlocked and still on the cable, then, after it came off the
cable 106 seconds after the tap, with it locked. They first reported that a
message didn't flash, and then that the check with the phone locked was
done. The evidence comes from the same records. The battery history starts
again at 21:06:47, when the phone came off the cable once more. This time
its clock and the log agree to within about a tenth of a second (0.03 to
0.11 seconds).

| Time | Event |
|---|---|
| 21:04:59.7 | The phone was unlocked with its password. |
| 21:05:00.810 | The widget was tapped. Beacon's settings file was written at 21:05:00.825 and holds a timed pause until 22:05:00.824, and the resume alarm was set for that time. The widget was redrawn at 21:05:01.1. |
| 21:05:11.979 | A WhatsApp message came with the phone unlocked and in use, and Beacon ignored it as `IN_USE`. Beacon checks whether the phone is in use before it checks the pause, so this message doesn't show the pause at work. |
| 21:06:47 | The phone came off the cable. |
| 21:06:48.8 | The power button put the phone to sleep, and the lock screen came up at 21:06:49.2. |
| 21:09:34 | Doze's deep idle began. It lasted until the unlock at 21:13:38.7. |
| 21:13:30.571 | WhatsApp posted a message notification. |
| 21:13:30.831 | Beacon ignored it as `PAUSED`, a quarter of a second later. Nothing flashed: after 21:05 the log has no alert wake lock, no camera light and no alert screen from Beacon. |
| 21:13:31.1 | The screen woke for WhatsApp's notification: Android logged the wake as coming from the system's notifications (`details=com.android.systemui:NOTIFICATION`), not from Beacon. At 21:13:31.6 Beacon set its resume alarm again, for the same 22:05:00.824. Its settings file wasn't written, and the widget wasn't redrawn. |
| 21:13:38.6 | The phone was unlocked. |
| 21:13:41 | The phone was plugged into a charger. |

Beacon decides `PAUSED` only after a notification has passed its earlier
checks: it comes from a message app, it is a new message and not the
person's own, and the phone isn't in use. So the message at 21:13:30 was a
new incoming WhatsApp message. WhatsApp's own switch is checked only after
the pause, so the log can't show it was ON. But the settings hold nothing
but the pause, so every app is at its default, and WhatsApp messages
flashed at 20:34 and 20:43 with the same settings. So it would otherwise
have flashed.

The resume alarm set again at 21:13:31.6 came about half a second after
the screen woke. That is what the screen-on catch-up does while a pause is
still running: it keeps the pause and sets the alarm again. The log doesn't
name the catch-up, but nothing else in the log could have set the alarm: Beacon's
Home didn't come to the front, and nothing in the log points to a time,
time-zone or language change.

| Check | Result |
|---|---|
| A message during a pause, with the phone locked, asleep and on battery | Tested on Redmi 15C 5G, Android 16. A WhatsApp message was ignored as `PAUSED`, in deep idle, and nothing flashed. |
| The screen-on catch-up while a pause is still running | Tested on Redmi 15C 5G, Android 16, by timing and by ruling out the other triggers. It set the resume alarm again for the same time, didn't change the stored pause, and didn't redraw the widget. |

## Not tested

- **A message during the first pause.** None came. The second pause showed
  one being held back ([above](#a-message-during-a-pause-later-that-evening)).
- **The widget showing PAUSED during either pause.** Nobody looked at it. The
  phone checks saw PAUSED after a tap
  ([`2026-11-phone-checks.md`](2026-11-phone-checks.md)).
- **The resume alarm firing in Doze.** It never fired. It was cancelled at
  20:34:59.165, 43 seconds into its 10-minute window, so this doesn't show
  Doze holding it back. Spec section 10 says it waits for Doze's next
  maintenance window, which here began at 20:37:08: still not seen.
- **The widget back to ON with no message.** Here an alert woke the screen.
  Pressing the power button runs the same screen-on clean-up, but that
  wasn't seen on the phone. Without a screen-on, the widget would have kept
  showing PAUSED, unseen, until the alarm fired (in Doze, at a maintenance
  window), Beacon's Home came to the front, or the time, time zone or
  language changed.
- **A plain SMS from the phone network.** Both messages to Google Messages
  in the cable-out checks came as push messages, so they may have been RCS.
