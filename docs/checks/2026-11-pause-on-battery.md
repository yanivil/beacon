# The widget's pause on battery: checks

This page records the owner's second cable-out check on the test phone, on
11 November 2026: the widget's 1-hour pause with the phone locked, on
battery and in Doze, and how alerts and the widget came back on after it
(spec section 1, "Done means" 4). Nothing in the code changed.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**.

## How it was recorded

The owner ran the check alone: one tap on the widget, the phone left locked
on battery for more than an hour, then a message. They reported that it
worked. The evidence was then read from the phone over adb, without
changing anything there:

- Android's battery history (`dumpsys batterystats --history`). It starts
  at 19:34:21, when the phone came off the cable.
- Android's log, whose app lines reach back to 19:17:43: Beacon's decisions
  (`D/Beacon`), the launcher's widget tap, the alarm being set, Beacon's
  screens and the widget's redraws.
- Android's alarm records (`dumpsys alarm`), which keep Beacon's last
  removed alarm.
- The times of Beacon's stored files, its stored settings, and its
  Autostart app-op.
- One screenshot of the home screen at 20:47, after the check.

The battery history and the log keep time separately: this time the log
runs about 1 second ahead (the camera light came on 0.98 seconds later in
the log than in the battery history, for both alerts). Push messages, the
screen, Doze and the charger below come from the battery history. Every
other time comes from the log, the alarm records or the file times, which
share the log's clock. Delays are measured on one clock.

| | |
|---|---|
| Phone | Redmi 15C 5G (model 2508CRN2BE), Android 16, HyperOS `OS3.0.306.0.WPOMIXM` |
| Build | `main`'s debug build at `d3675d8`, installed at 15:53:39, as in the [first cable-out check](2026-11-alerts-on-battery.md) |
| Beacon's settings | The defaults: yellow, Medium (60 blinks a minute), 1 minute. Only the pause is stored. |
| Autostart | On (`MIUIOP(10008): allow`) |
| Power | Off the cable from 19:34:21, at 100%. Plugged into a charger at 20:43:49. The level stayed at 100%. |

## What happened

| Time | Event |
|---|---|
| 19:34:15.732 | The widget was tapped: the launcher sent the tap to Beacon. At 19:34:15.771 Android logged Beacon's resume alarm (`RTC_WAKEUP`), set for 20:34:15.748, one hour on. At 19:34:16.1 the widget was redrawn. |
| 19:34:21 | The phone came off the cable, at 100%. |
| 19:34:29 | The screen went off. The phone stayed locked until 20:43:45. |
| 19:37:09 to 20:37:08, and 20:37:38 to 20:43:45 | Doze's deep idle (`device_idle=full`). Between the two stretches came a 30-second maintenance window. The screen waking for the first alert didn't end deep idle; the unlock did. |
| 19:34:21 to 20:34:53 | No push message reached WhatsApp or Google Messages, and Beacon logged no decision: no message came during the pause. |
| 20:34:15.748 | The pause's end, and the resume alarm's time. The alarm didn't fire: the phone was in deep idle. |
| 20:34:53.6 | A push message reached WhatsApp. |
| 20:34:55.797 | Beacon decided `Alert(source=WHATSAPP)`, 40 seconds after the pause's end. The 1-hour pause was still stored then: Beacon judged it by the clock. |
| 20:34:55.799 to 20:34:58.732 | Beacon asked for the alert's wake lock 2 ms after deciding, but the phone was just going to sleep. Android logged `Excessive delay in acquire_wake_lock(), duration=2932ms`, after a 2.7-second sleep. |
| 20:34:58.745 | The camera light came on, about 4 seconds after the push. At 20:34:58.8 the alert screen came to the front and the screen woke. |
| 20:34:59.131 | Beacon's stored pause became ON. At 20:34:59.165 the resume alarm, still waiting, was cancelled (`alarm_cancelled`), and at 20:34:59.620 the widget was redrawn. |
| 20:34:59 to 20:35:31 | Three more WhatsApp messages were decided `Alert` during the alert. As the spec says, they started no second alert. |
| 20:35:58.552 | The alert ended by itself, after 60 flashes in 1 minute. |
| 20:43:20.4 | A push message reached Google Messages, the SMS app. |
| 20:43:24.590 | Beacon decided `Alert(source=SMS)`, and the camera light came on 14 ms later, about 3 seconds after the push. The alert screen came to the front. At 20:43:34 a WhatsApp message during it started no second alert. |
| 20:43:38.5 | A tap on the alert screen closed it, after 14 flashes. |
| 20:43:45 | The phone was unlocked. |
| 20:43:49 | The phone was plugged into a charger. |
| 20:43:56 | A message that came while the phone was in use was ignored (`IN_USE`), as the owner decided. |
| 20:47 | The home screen's widget showed "Visual alerts" and "ON" ([01](pause-on-battery/01-widget-on-after-pause.png)). |

### What ended the stored pause

Four things in Beacon end a timed pause that is over: the resume alarm,
Beacon's Home coming to the front, the screen turning on, and a time,
time-zone or language change. Here it was the screen turning on, for the
alert:

- **Not the resume alarm.** Android's alarm records show the alarm removed
  with `alarm_cancelled` at 20:34:59.165, so it was still waiting when it
  was cancelled. It never fired.
- **Not Home.** Only the alert screen (`AlertActivity`) started; Beacon's
  Home (`MainActivity`) didn't, from the tap until after the alert.
- **No time, time-zone or language change.** Nothing in the log points to
  one after the tap, and the phone's clock was last set at 15:25:56.

The write itself shows that the stored pause was the timed one. Beacon's
settings file changed at 20:34:59.131, and Android's DataStore writes only
a changed value. The catch-up changes only a timed pause that is over, and
redraws the widget only when the stored pause changed, as it did at
20:34:59.620.

## Results

| Check | Result |
|---|---|
| One tap on the widget pauses alerts for 1 hour | Tested on Redmi 15C 5G, Android 16. The tap at 19:34:15.7 set the resume alarm for 20:34:15.748, one hour on, and the widget was redrawn. |
| Alerts come back on by themselves, on battery and in Doze | Tested on Redmi 15C 5G, Android 16. The first message after the pause's end flashed, with the phone locked and in deep idle. Beacon decided while the 1-hour pause was still stored, so alerts came back by the clock, without the resume alarm. The alert flashed 60 times in its 1 minute, and the screen woke. |
| The widget comes back to ON | Tested on Redmi 15C 5G, Android 16. Less than a second after the alert woke the screen, the screen-on catch-up stored ON, cancelled the waiting resume alarm, and redrew the widget. At 20:47 it showed ON ([01](pause-on-battery/01-widget-on-after-pause.png)). |
| A later message to the SMS app | Tested on Redmi 15C 5G, Android 16. At 20:43 it flashed, about 3 seconds after its push. |
| Messages during an alert | Tested on Redmi 15C 5G, Android 16. Three WhatsApp messages during the first alert and one during the second started no second alert, and the first still ended after its 1 minute. |
| A tap ends an alert | Tested on Redmi 15C 5G, Android 16. A tap on the second alert's screen closed it, and the light stopped. |

## Seen once

- **The first alert's light came on about 4 seconds after its push.** In
  the first cable-out check this took about 2 seconds, and here the SMS
  alert took about 3. The phone was going to sleep just as Beacon decided,
  and taking the alert's wake lock waited 2.9 seconds for it to wake again.
  Beacon asked for the wake lock 2 ms after deciding, so the wait was
  Android's.

## Not tested

- **The resume alarm firing in Doze.** It never fired. At 20:34:59.165,
  44 seconds after its time, it was still waiting, and the screen-on
  catch-up cancelled it. The phone was in deep idle then. The alarm was
  also still inside its 10-minute window, so this doesn't show Doze holding
  it back. Spec section 10 says it waits for Doze's next maintenance window,
  which here began at 20:37:08: still not seen. The screen turning on
  catches the widget up, as here, so the alarm only changes a widget that
  nobody is looking at.
- **The screen turned on by the person after a pause.** Here an alert woke
  the screen. Pressing the power button runs the same screen-on catch-up,
  but that wasn't seen on the phone.
- **A message during the pause.** None came, so a paused message wasn't
  seen being ignored on the phone. It was on the emulator (PR 2's check
  12).
- **A plain SMS from the phone network.** Both messages to Google Messages
  in the cable-out checks came as push messages, so they may have been RCS.
