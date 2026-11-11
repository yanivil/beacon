# Alerts on battery: checks

This page records the owner's cable-out check on the test phone, on
11 November 2026: Step 2's app alerting with the phone asleep, in Doze and
on battery. Nothing in the code changed.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**. A result the owner reported, and the phone's records didn't
show, says so.

## How it was recorded

The owner ran the check alone, with the phone off the cable, and reported
that it "looks like it works". The evidence was then read from the phone's
own records over adb, without changing anything there:

- Android's battery history (`dumpsys batterystats --history`). It starts
  at 15:56:24, when the phone came off the cable.
- Beacon's debug log (`D/Beacon`), which names each decision. The phone's
  log held only what came after 17:05.
- Beacon's stored settings and its Autostart app-op.

The battery history and the log keep time separately, about a second
apart. Decision times below come from the log, and the rest from the
battery history.

| | |
|---|---|
| Phone | Redmi 15C 5G (model 2508CRN2BE), Android 16, HyperOS `OS3.0.306.0.WPOMIXM` |
| Build | `main`'s debug build at `d3675d8`, installed at 15:53:39 |
| Beacon's settings | The defaults: yellow, Medium (60 blinks a minute), 1 minute. Only `pause_kind = NONE` is stored, last written at 12:34. |
| Autostart | On (`MIUIOP(10008): allow`) |
| Power | Off the cable from 15:56:24, at 100%. Plugged into a charger at 19:12:33. |

## What happened

| Time | Event |
|---|---|
| 15:56:24 | The phone came off the cable, at 100%. |
| 15:56:27 | The screen went off. It stayed off for 3 hours and 13 minutes. |
| 15:59:07 to 16:59:06, 16:59:36 to 18:59:36, and 19:06:48 to 19:12:29 | Doze's deep idle (`device_idle=full`). The last stretch ended only when the phone was unlocked, at 19:12:29, so the history reports deep idle through both alerts. |
| 19:09:40 | WhatsApp woke for a message, in that last stretch of deep idle. |
| 19:09:42.975 | Beacon decided `Alert(source=WHATSAPP)`. The screen woke and Beacon's alert came to the front. |
| 19:09:42 to 19:10:42 | The camera light flashed 60 times, once a second. The alert's wake lock was released at 19:10:42, and the screen went off at 19:10:53. |
| 19:12:16 | Google Messages, the SMS app, woke. |
| 19:12:19.863 | Beacon decided `Alert(source=SMS)`. The screen woke and the alert came to the front. |
| 19:12:19 to 19:12:24 | The camera light flashed 6 times, then stopped: the alert ended after about 5 seconds, before its minute. |
| 19:12:29 | The phone was unlocked, and the home screen came to the front. |
| 19:12:33 | The phone was plugged into a charger. |

## Results

| Check | Result |
|---|---|
| A WhatsApp message after more than three hours asleep on battery, in deep Doze | Tested on Redmi 15C 5G, Android 16. It flashed about 2 seconds after WhatsApp woke, with the screen and the camera light. |
| An SMS on battery | Tested on Redmi 15C 5G, Android 16. It flashed about 2 seconds after Google Messages woke. |
| Speed and length | Tested on Redmi 15C 5G, Android 16. The WhatsApp alert flashed once a second, Medium's 60 blinks a minute, and ended by itself after its 1 minute. |
| The screen's blink | Tested on Redmi 15C 5G, Android 16, by the owner's report. The records show the screen woke and the alert was in front; they don't show its colours. |
| No message missed | Tested on Redmi 15C 5G, Android 16, as far as the records show. Before 19:09:40, WhatsApp ran only its own scheduled job, three times, and Google Messages didn't wake, so nothing suggests a message arrived that Beacon missed. |
| Battery drop and heat | Tested on Redmi 15C 5G, Android 16. The level read 100% at both ends, and the charge counter read 5868 mAh at the start and 5874 mAh at its last reading, at 17:21, so no drop shows. The battery's temperature was at most 29.0 °C, at the start. |

## Not tested

- **The widget's 1-hour pause on battery, and its resume alarm in Doze.**
  No pause was set during the check: Beacon's stored settings weren't
  written after 12:34. Spec section 10 says the alarm waits for Doze's next
  maintenance window, and that is still not seen on the phone.
- **How the SMS alert ended.** The records show only that its light
  stopped after about 5 seconds, not what ended it.
- **The lock screen showing** with the screen on: both alerts started with
  the screen off.
- **Telegram:** it isn't installed on the test phone.
