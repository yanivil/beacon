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
- Beacon's debug log (`D/Beacon`), which names each decision, ignored
  notifications included. When first read, the phone's log reached back to
  17:05; it has since moved on to 17:28.
- Beacon's stored settings and its Autostart app-op.

The battery history and the log keep time separately: the log runs about
0.5 to 0.8 seconds ahead. Decision times below come from the log, and the
rest from the battery history. Delays are measured on the battery history's
clock alone.

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
| 15:59:07 to 16:59:06, 16:59:36 to 18:59:36, and 19:06:48 to 19:12:29 | Doze's deep idle (`device_idle=full`). Between the last two stretches, from 18:59:36 to 19:06:48, the phone was out of it. The last stretch ended only when the phone was unlocked, at 19:12:29: a screen woken over the lock screen doesn't end it. |
| 19:09:40 | A push message reached WhatsApp (`c2dm.intent.RECEIVE`), 2 minutes 52 seconds into the last stretch of deep idle. Android let WhatsApp run for it. |
| 19:09:42.975 | Beacon decided `Alert(source=WHATSAPP)`. The screen woke and Beacon's alert came to the front. At 19:09:43.569 WhatsApp's group summary for it was ignored (`Ignore(reason=GROUP_SUMMARY)`), so it started no second alert. |
| 19:09:42 to 19:10:42 | The camera light flashed 60 times, once a second. The alert's wake lock was released at 19:10:42, and the screen went off at 19:10:53. |
| 19:12:16 | A push message reached Google Messages, the SMS app, at 19:12:15.9, and it woke. A push suggests an RCS chat message rather than a plain SMS; Beacon counts either as SMS, because it comes from the SMS app. |
| 19:12:19.863 | Beacon decided `Alert(source=SMS)`. The screen woke and the alert came to the front. |
| 19:12:19 to 19:12:24 | The camera light flashed 6 times, then stopped: the alert ended after about 5 seconds, before its minute. |
| 19:12:29 | The phone was unlocked, and the home screen came to the front. |
| 19:12:33 | The phone was plugged into a charger. |
| 19:12:34 | Three more notifications were ignored, as not from a message app (`NOT_A_SOURCE`). |

## Results

| Check | Result |
|---|---|
| A WhatsApp message after more than three hours asleep on battery, in deep Doze | Tested on Redmi 15C 5G, Android 16. The screen had been off for 3 hours 13 minutes, and deep idle had started again 2 minutes 52 seconds before. The camera light started about 2 seconds after the push reached WhatsApp, and the screen woke. |
| A message to the SMS app on battery | Tested on Redmi 15C 5G, Android 16. The camera light started about 3 seconds after the push reached Google Messages, and the screen woke. The alert ended after about 5 seconds (see Not tested). |
| Speed and length | Tested on Redmi 15C 5G, Android 16. The WhatsApp alert flashed once a second, Medium's 60 blinks a minute, and ended by itself after its 1 minute. |
| The alert screen | Tested on Redmi 15C 5G, Android 16, for both alerts: the screen woke and Beacon's alert came to the front. Its colours aren't in the records (see Not tested). |
| No message missed | Tested on Redmi 15C 5G, Android 16, as far as the records show. Before the two that alerted, the battery history shows no push message to either app. Until then, WhatsApp and Google Messages ran only their own jobs, syncs and wake locks. Beacon's log, as first read, shows no notification at all from 17:05 until the first alert. |
| Battery on the history's 1% steps | Tested on Redmi 15C 5G, Android 16. The level stayed at 100% from 15:56:24 to 19:12:33. The charge counter read 5868 mAh at the start; its one later reading, 5874 mAh at 17:21, came before the alerts. |
| Heat during the alerts | Tested on Redmi 15C 5G, Android 16. The battery read 26.0 to 27.0 °C. The highest reading, 29.0 °C, came at the start, as the phone left the charger. |

## Not tested

- **The widget's 1-hour pause on battery, and its resume alarm in Doze.**
  No pause was set during the check: Beacon's stored settings weren't
  written after 12:34. Spec section 10 says the alarm waits for Doze's next
  maintenance window, and that is still not seen on the phone.
- **How the second alert ended.** The records show only that its light
  stopped after about 5 seconds, not what ended it.
- **The blink's colours on the phone.** The owner's "looks like it works"
  was about the check as a whole.
- **What an alert costs in battery.** About 70 seconds of light and two
  screen wakes are too small for the history's 1% steps.
- **A plain SMS from the phone network.** The second message came as a
  push, so it may have been RCS.
- **Alarms that Doze holds back.** Both messages came as push messages,
  which Android lets through Doze. That says nothing about Beacon's own
  resume alarm.
- **The lock screen showing** with the screen on: both alerts started with
  the screen off.
- **Telegram:** it isn't installed on the test phone.
