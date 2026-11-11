# Phone checks: Step 2 on the test phone

This page records the first time Step 2's app ran on the test phone, on
11 November 2026, with the owner at the phone. It covers:
- what was checked, and what each check showed;
- the defects and observations it found;
- the decisions the owner made during the session;
- what is still not tested.

Every result is labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**. A result the owner reported, and the phone's log didn't show,
says so.

Nothing here changes Beacon's code. Each defect is fixed in a PR of its own.

## The phone

| | |
|---|---|
| Phone | Redmi 15C 5G (model 2508CRN2BE), MediaTek MT6835 |
| Android | 16 (API 36), build `BP2A.250605.031.A3` |
| HyperOS | `OS3.0.306.0.WPOMIXM` |
| The owner's settings, left as they were | Language Hebrew (right-to-left). Text size 1.45. Display size 336 dpi (the default is 320), so the screen is 343 dp wide. 3-button navigation. Light theme. Auto-rotate on. Google Messages is the SMS app. WhatsApp is installed, Telegram isn't. Beacon's Autostart (a HyperOS permission) is off, its default. |
| Power | Connected to the Mac by USB, and so charging, for every check. The USB link dropped for about three minutes around 12:01, during the backup check, until the owner replugged it. No alert ran on battery. |
| Date | 11 November 2026 |

## Builds

| Build | What it is | Installed |
|---|---|---|
| `main` at `2eb7577` | A clean debug build, 30,531,250 bytes, the same size as the compileSdk 37 PR's build | 10:20. HyperOS asks on the phone to confirm every install over USB. The first prompt closed after 12 seconds without installing, and the owner accepted the second. |
| Diagnostic | `main` plus one temporary log line that wrote only yes/no facts and counts about a notification's messages, never a name or text. Built in a throwaway copy of `main` and never committed. | 11:40 |
| Widget previews 1 and 2 | The diagnostic build plus smaller widget text (preview 1), then also less padding (preview 2), for the owner to look at. Also never committed. | 11:57 and 11:59. Preview 2 is still installed. |

Each install kept Beacon's settings and permissions. The message-handling
code was the same in every build, so the message results hold for `main`.

## The owner's decisions

Made during the session, on 11 November 2026:

1. **Flash only when the screen is off or locked.** The owner found the
   flash disruptive while using the phone ("everything turns flashing
   yellow", translated from Hebrew). A real alert should flash only when
   the screen is off, or when it's on but showing the lock screen. While
   the phone is unlocked and in use, Beacon does nothing at all: no screen
   flash and no camera light. The message app's own notification is the cue
   then. The test alert is not affected. Built afterwards: see
   [`2026-11-flash-when-not-in-use.md`](2026-11-flash-when-not-in-use.md).
2. **The widget's new look.** "PAUSED" at 40 sp and "ON" at 48 sp, instead
   of the design's 52 and 64, and 12 dp of padding instead of 16, so the
   resume time fits at 4 × 2 (defect P4). The owner chose this from preview
   2 on the phone. Built afterwards: see
   [`2026-11-widget-text-size.md`](2026-11-widget-text-size.md).
3. **A second tap on Stop needs no guard.** On an unlocked phone the second
   tap lands on whatever is behind the alert, such as a home-screen icon.
   The owner ruled that this isn't Beacon's problem.
4. **No check at the largest text size.** The owner was satisfied with the
   screens and the widget at their own text size, 1.45.

## Defects

### P1. English text is laid out right to left when the phone is in Hebrew

- **What happens:** Beacon mirrors its layout for right-to-left, as it
  should. But every English sentence also gets a right-to-left direction:
  - full stops move to the start of the line (".Beacon needs these to flash
    for your messages", ".OFF");
  - a line that starts with a number has its parts swapped: "1 minute"
    shows as "minute 1", "10 min" as "min 10", and "60 blinks a minute" as
    "blinks a minute 60".
- **Where:** Home, Alert settings, and every screen with such a line.
  Screenshots [02](phone-checks/02-rtl-home-minute.png) and
  [03](phone-checks/03-rtl-settings-blinks.png).
- **Why:** Compose's default text direction (`TextDirection.Unspecified`)
  follows the layout direction, not the text. Only the button helper in
  `Buttons.kt` sets `TextDirection.Content`.
- **Who sees it:** anyone whose phone is in Hebrew or Arabic. Beacon has no
  Hebrew translation yet, so they read English laid out right to left.
- **Fix:** each text's direction should follow its own words, which keeps a
  future Hebrew translation right to left.
- **Fixed** in the right-to-left text PR: see
  [`2026-11-rtl-text-direction.md`](2026-11-rtl-text-direction.md).

### P2. The Xiaomi battery step names the wrong row on Hebrew HyperOS

- Beacon says "Tap Battery saver, then No restrictions."
- On this phone, Beacon's button opens HyperOS's App info page, where the
  row to tap is called **"חשמל"** (Power). "חסכון בסוללה" (Battery saver)
  is only the heading on the next page, above **"אין הגבלות"** (No
  restrictions), which does match. Screenshot
  [05](phone-checks/05-hyperos-battery-page.png).
- The phone's log shows the owner's first visit to App info didn't open the
  battery page; the second did.
- Not checked: HyperOS in English, which would need a language change.
- **Fixed** in the Xiaomi battery step PR, which read HyperOS's own labels
  instead: see [`2026-11-xiaomi-battery-step.md`](2026-11-xiaomi-battery-step.md).

### P3. Beacon flashes for the owner's own reply sent from a WhatsApp notification

- **What happens:** the owner replied to a WhatsApp message from inside its
  notification. WhatsApp then posts the notification again with the reply
  added, and Beacon flashed for it (11:34:22, and again in the diagnostic
  run at 11:43:01).
- **Why:** Beacon treats the newest message as the owner's own only if it
  has no sender, or if its sender's key equals the owner's key. The
  diagnostic build showed that WhatsApp sets no keys at all. Its reply
  carries a sender whose name equals the owner's name in the notification.
  WhatsApp also doesn't fill in Android's reply-history field.
- **Fix:** for WhatsApp only, when neither the sender nor the owner has a
  key, compare their names. The names are compared in memory only, and
  never stored or logged. If WhatsApp's label for the owner is their
  profile name (the label wasn't logged), a contact or group member with
  exactly that name would then not flash.
- Google Messages wasn't tried this way.
- **Fixed** in the own-reply PR: see
  [`2026-11-own-whatsapp-reply.md`](2026-11-own-whatsapp-reply.md).

### P4. The widget hides the resume time at the owner's text size

- At 4 × 2 the widget is 302 × 155 dp on this phone, and HyperOS reports
  that size to Beacon correctly.
- At text size 1.45 the line "Back ON by itself at 12:54" wraps onto two
  lines (69 dp). Next to "PAUSED" there was no room for it, so the widget
  showed only "PAUSED" ([06](phone-checks/06-widget-main.png)).
- The spoken label still had the time ("Visual alerts are PAUSED until
  12:54").
- Preview 2 shows both ([07](phone-checks/07-widget-preview.png)). The
  owner chose that look (decision 2).
- **Fixed** with the owner's sizes: see
  [`2026-11-widget-text-size.md`](2026-11-widget-text-size.md).

### P5. After a restart, Beacon is silent until it's opened

- This was Step 1's open risk. It is now confirmed on Step 2's app.
- **After the update to the diagnostic build:** with Autostart off, HyperOS
  refused to connect Beacon's notification listener ("AutoStartManagerService:
  Reject service … BeaconListener").
- **After a restart:** the same refusal at 12:07:53. An SMS at 12:10:37,
  with the phone locked, got no decision at all: Beacon never saw it.
- **Opening Beacon fixes it at once:** at 12:10:54 it switched its listener
  off and on, and was connected a second later. It recorded the missed SMS
  as already shown, so that SMS didn't flash late. The next SMS, at
  12:11:05, flashed at once.
- **What's needed:** this needs the owner's decision. One option is a
  fourth setup row on Xiaomi phones, for HyperOS's Autostart permission.
- **Fixed** with a Background autostart row, as the owner decided: see
  [`2026-11-xiaomi-autostart.md`](2026-11-xiaomi-autostart.md).

## Observations

- **PO1.** The setup card's text column is narrow at 343 dp and text size
  1.45, with one or two words per line ("So alerts / still arrive / while
  the / phone"). It's readable, and nothing is cut off.
- **PO2.** Beacon flashes for WhatsApp group messages too. During the
  session, several alerts came from the owner's real groups. That follows
  the rule (messages), but a busy group flashes often.
- **PO3.** Google Messages' notification banner sits over the alert's top
  card ("New message", the app's name) while it flashes. The flash and Stop
  aren't affected. This answers the question PR 2's checks asked as O1, for
  HyperOS.
- **PO4.** HyperOS's widget picker lists Beacon only under its second tab,
  "ווידג'טים נוספים" (More widgets). The first tab shows 10 recommendations.
  The picker shows Beacon's icon as the preview, because the widget has no
  preview image of its own.
- **PO5.** HyperOS's launcher draws other apps' widgets at 0.9 times the
  screen density (`autoScale … scale 0.9`). The widget's fit measures at
  full density, so on HyperOS it leaves a little room to spare.

## Results

### The app at the owner's settings

| Check | Result |
|---|---|
| Icon on HyperOS's launcher | Tested on Redmi 15C 5G, Android 16. The yellow bolt, in HyperOS's own rounded-square shape, labelled "Beacon" ([01](phone-checks/01-icon.png)). Not themed. |
| Home and Alert settings in portrait | Tested on Redmi 15C 5G, Android 16. Every row wraps and scrolls to the end, and nothing is cut off. Home keeps "Test alert" above the bar. Apart from P1, every line reads correctly. |
| The same in landscape | Tested on Redmi 15C 5G, Android 16. The phone lay on its side with auto-rotate on (343 dp tall). Both screens scroll to the end, and nothing is cut off. |
| Right-to-left layout | Tested on Redmi 15C 5G, Android 16. Rows, switches, the bottom bar and the chevron are mirrored correctly. Text direction is defect P1. |
| Contrast, measured from screenshots | Tested on Redmi 15C 5G, Android 16. White on #0B0B0D 19.66:1, white on #1B1B20 17.15:1, #D9D9DF on #1B1B20 12.20:1, white on #141418 18.37:1, #0B0B0D on #FFD60A 13.93:1, #D9D9DF on #0B0B0D 13.99:1. The colours on screen are exactly the theme's, so HyperOS doesn't change them. |
| Largest text size | Not tested: the owner's decision 4. |

### The test alert and the camera light

| Check | Result |
|---|---|
| Test alert at Medium | Tested on Redmi 15C 5G, Android 16. The camera service turned the light on for Beacon three times, each time for 0.45 to 0.5 s in a 1-second cycle (60 a minute). It ended off. |
| The flash | Tested on Redmi 15C 5G, Android 16. The screen alternates #FFD60A and black. HyperOS's status and navigation bars stay black with white icons on both ([04](phone-checks/04-test-alert-flash.png)). |

### Setup

| Check | Result |
|---|---|
| Notification access | Tested on Redmi 15C 5G, Android 16. Beacon's button opens Android's own page for Beacon ("הגדרת התראות"). The owner switched it on and confirmed HyperOS's "סכנה" (Danger) warning, with its checkbox and countdown. |
| Display over other apps | Tested on Redmi 15C 5G, Android 16. Beacon's button opens the list of apps (Android 11 and later ignore the app in the request). The owner chose Beacon and switched it on. |
| Battery: No restrictions | Tested on Redmi 15C 5G, Android 16. Beacon's button opens HyperOS's App info. The row is named differently (P2). After the change, Beacon is on the system's battery whitelist. |
| The card goes away | Tested on Redmi 15C 5G, Android 16. With all three on, Home opens on "Visual alerts ON". |

### Real messages

All with the phone connected to the Mac by USB, so charging.

| Check | Result |
|---|---|
| SMS, phone unlocked | Tested on Redmi 15C 5G, Android 16. The alert screen opened 26 ms after Beacon's decision, and the light came on 17 ms later. Stop was in view without scrolling. One tap ended the alert in 7 ms, and the light went off 4 ms after that. |
| WhatsApp, phone asleep and locked | Tested on Redmi 15C 5G, Android 16. Beacon woke the screen (`WAKE_REASON_APPLICATION`) and showed the alert over the lock screen. WhatsApp's group summary was ignored, so each message flashed once. |
| SMS, phone asleep and locked | Tested on Redmi 15C 5G, Android 16, three times, as above. |
| The 1-minute default | Tested on Redmi 15C 5G, Android 16. Alerts left alone ended 60.0 s after they started. |
| A message during an alert | Tested on Redmi 15C 5G, Android 16. It doesn't start a second alert or lengthen the one running. |
| A second quick tap on Stop | Tested on Redmi 15C 5G, Android 16. Phone locked (twice): the second tap lands on the lock screen and does nothing. Phone unlocked: the owner reports it pressed a home-screen icon (the owner's report, not seen in the log), which they accept (decision 3). |
| The owner's own reply from a WhatsApp notification | Tested on Redmi 15C 5G, Android 16: it flashes. Defect P3. |
| WhatsApp's backup | Tested on Redmi 15C 5G, Android 16, with a manual backup and the screen on (preview 2 build). The backup updated WhatsApp's ongoing notification 64 times in 33 seconds. Each was ignored as ongoing, and nothing flashed. |
| Google Messages' notification while it receives | Tested on Redmi 15C 5G, Android 16. Its ongoing notification is ignored. |
| The camera app opened over a running alert | Tested on Redmi 15C 5G, Android 16. Opening another app counts as leaving the alert, so it ends and the light goes off within 1 ms, as designed. So the case where the camera is taken while the alert stays in front wasn't reached. |

### Reconnecting

| Check | Result |
|---|---|
| After an update | Tested on Redmi 15C 5G, Android 16. HyperOS refused to reconnect the listener (P5). Opening Beacon reconnected it each time: within 4 s after the diagnostic update, and again after each preview. |
| After a restart | Tested on Redmi 15C 5G, Android 16: no alerts until Beacon is opened (P5). |

### The widget

| Check | Result |
|---|---|
| Finding it | Tested on Redmi 15C 5G, Android 16. It's under "ווידג'טים נוספים", sized 4x2 (PO4). |
| Tap to pause | Tested on Redmi 15C 5G, Android 16. It showed PAUSED. Its resume alarm was set one hour later, for 12:54:20, and its spoken label gave the time in 24-hour format, as the phone is set. The visible time is defect P4. |
| Tap to turn ON | Tested on Redmi 15C 5G, Android 16. The saved pause was cleared. |
| After a restart | Tested on Redmi 15C 5G, Android 16. HyperOS let the launcher's update start Beacon, and the widget drew ON. No pause was running, as the owner had turned it ON before the restart. |
| The preview look | Tested on Redmi 15C 5G, Android 16. PAUSED with its icon, and the resume time on two lines (P4, decision 2). |

### TalkBack

| Check | Result |
|---|---|
| Home, the test alert and the widget | Tested on Redmi 15C 5G, Android 16, with TalkBack 17.1. The owner went through all three and reports it "worked well". The words weren't recorded. |

## Not tested

- **Alerts on battery:** no alert ran on battery, so Doze, and the resume
  alarm in Doze, weren't reached. The owner's cable-out check later that
  day ran WhatsApp and SMS alerts on battery in Doze; the resume alarm in
  Doze is still not tested
  ([`2026-11-alerts-on-battery.md`](2026-11-alerts-on-battery.md)).
- **The resume alarm firing on its own:** the pause was ended by a tap. In
  the owner's second cable-out check that evening, a 1-hour pause ended on
  battery, but the screen-on catch-up cleaned it up and cancelled the alarm
  before it fired
  ([`2026-11-pause-on-battery.md`](2026-11-pause-on-battery.md)).
- **A restart during a timed pause:** there was no pause when the phone
  restarted.
- **The largest text size** on the phone (decision 4). The screens were
  checked at 2.0 on the emulator, and here at 1.45.
- **Details of what TalkBack says,** such as PR 1's D6 (the order of the
  announcements when the overlay opens). Only the owner's overall report
  exists.
- **The camera taken while an alert stays in front,** the overlay's "Off
  while another app uses the camera" line, the heat stop and the real heat
  listener.
- **"Until I stop it," Custom lengths, other speeds,** and the hourly
  wake-lock renewal.
- **TalkBack on Alert settings:** the owner went through Home, the test
  alert and the widget.
- **WhatsApp's automatic nightly backup with the screen off.** Only a
  manual backup with the screen on was run.
- **An SMS app that posts with `when == 0` and no messaging layout.**
  Google Messages uses the messaging layout with real times.
- **Google Messages' own replies** from its notification.
- **HyperOS's 12/24-hour switch** sending `TIME_SET`, and a clock change
  while the widget updates (PR 3's O1).
- **The widget in landscape, on a single row, and with Bold text.**
- **HyperOS in English** (P2), **a Hebrew translation**, **Autostart switched
  on**.
- **Telegram and WhatsApp Business,** **other phones and brands**, and
  **Android 10 to 15**.

## Phone settings changed and restored

| What | Before | During | After |
|---|---|---|---|
| Beacon | Not installed | `main`, then the diagnostic build, then previews 1 and 2 | Preview 2 left installed, until a build with the fixes replaces it |
| Beacon's permissions | None | Notification access, "Display over other apps" and "No restrictions", all switched on by the owner | Left on |
| Beacon's widget | None | Added by the owner, at the top of the launcher's first page | Left in place, ON |
| TalkBack | Off | Switched on and off by the owner | Off |
| Text size, display size | 1.45, 336 | Unchanged | 1.45, 336 |
| The phone | Running | Restarted by the owner | Running |
| WhatsApp | | One manual backup, made by the owner | |
| `/sdcard/ui.xml` | Not checked | Written by each screen dump | Deleted at 12:41, and checked gone |

No setting was changed over adb: the owner granted every permission and
changed every setting by hand. Adb installed the builds, opened, scrolled
and tapped Beacon's own screens and test alerts, opened settings pages and
the widget picker to read them, opened the camera app once during an alert,
took screenshots and read logs. One stray adb tap opened a menu on an old
settings page; it was closed with Back.

**Privacy:** some screenshots and logs from the session show the owner's
contacts, groups and messages. None of them is in this repository.
