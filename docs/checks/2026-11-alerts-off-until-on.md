# Checks: turning alerts OFF until turned back ON (1.0.3)

Testers asked for a way to turn alerts off with no time limit, from the
widget and from the settings. The owner chose the design on 14 November
2026: a "Visual alerts" switch at the top of Alert settings, and a "Widget"
choice there for what one widget tap does ([Step 2
spec](../specs/2026-11-08-step2-design.md), sections 5, 8, 10 and 11, and
conflict 20). This page records the checks of that build.

Each result says **"Tested on emulator beacon-api36, Android 16"** or
**"Not tested"**.

## Setup

| | |
|---|---|
| Emulator | `beacon-api36` (`emulator-5554`), Android 16, 1080 × 2400, font scale 1.0 unless a check says 2.0 |
| Build | `./gradlew :app:assembleRelease` on `feature/alerts-off-until-on`: versionName 1.0.3, versionCode 4, signed with the upload key |
| Install | `adb install -r` over 1.0.2 (versionCode 3) from the same key, so the saved settings came from 1.0.2 |
| TalkBack | Android Accessibility Suite 16.0.0.738667889, read from its verbose log and driven through the emulator console, as in [`2026-11-step2-pr1.md`](2026-11-step2-pr1.md); the log level was set back to ERROR and TalkBack switched off afterwards |
| Date | 14 November 2026, about 17:35 to 17:50 |
| Automated | 408 unit and Robolectric tests passed; `lintDebug` found no issues |

## Checks

| Check | Result |
|---|---|
| 1. An install from 1.0.2 keeps the 1-hour widget tap | Tested on emulator beacon-api36, Android 16. After `install -r` over 1.0.2, Alert settings showed "Pause for 1 hour" selected; 1.0.2 had stored no choice. |
| 2. The switch opens Alert settings | Tested on emulator beacon-api36, Android 16. Under the title: a card with "Visual alerts", "Your screen and camera light will flash when a message arrives and you aren't using the phone." and the yellow ON pill, read by `uiautomator` as one checked node, above "How long to flash". |
| 3. Switching OFF turns alerts off, and Home follows | Tested on emulator beacon-api36, Android 16. A tap on the card: the pill showed OFF and the line "You will NOT see any flashes until you turn alerts back on." Home then showed PAUSED, the same line and "Turn alerts back ON", which turned alerts back on. |
| 4. The Widget section | Tested on emulator beacon-api36, Android 16. After "Screen flash color" and before "Privacy": the heading "Widget", the line "What one tap on the Beacon widget does while alerts are ON. While they are paused, a tap always turns them back ON." and two options, "Pause for 1 hour" ("Alerts come back ON by themselves.") and "Turn OFF until I tap again" ("Alerts stay OFF until you tap the widget again, or turn them ON here or on Home."). |
| 5. Choosing "Turn OFF until I tap again" redraws the widget | Tested on emulator beacon-api36, Android 16. Right after the choice, the widget on the home screen said "Tap to turn OFF", with the label "Visual alerts are ON. Tap to turn them OFF until you tap again." |
| 6. A widget tap turns alerts OFF with no time | Tested on emulator beacon-api36, Android 16. One tap: PAUSED and "Tap to turn ON now", with no "Back ON by itself" line, and the label "Visual alerts are PAUSED. Tap to turn them back on now." `dumpsys alarm` listed no pending Beacon alarm, only an old cancelled one. Beacon's Alert settings showed the switch OFF with the same line as check 3. A second tap turned alerts ON; the state held for 8 seconds after a tap. |
| 7. The largest font | Tested on emulator beacon-api36, Android 16, font scale 2.0. The switch card grew to fit "Visual alerts" on two lines and its line on six, with the OFF pill beside them; nothing was cut off. The unit tests check both new sections for overflow at 2.0. |
| 8. TalkBack on the switch | Tested on emulator beacon-api36, Android 16. Touching the card: "On. Visual alerts. Your screen and camera light will flash when a message arrives and you aren't using the phone. Switch", hint "Double-tap to toggle". A TalkBack double tap turned alerts OFF; touching the card again: "Off. Visual alerts. You will NOT see any flashes until you turn alerts back on. Switch". As with the app rows (the PR 1 checks' D6), TalkBack didn't speak the new state right after the double tap; it needs checking with TalkBack on the phone. |
| 9. TalkBack on the Widget section | Tested on emulator beacon-api36, Android 16. "Widget. Heading"; "Not selected. Pause for 1 hour. Alerts come back ON by themselves. Radio button. 1 of 2"; "Selected. Turn OFF until I tap again. Alerts stay OFF until you tap the widget again, or turn them ON here or on Home. Radio button. 2 of 2". |
| 10. TalkBack on the widget | Tested on emulator beacon-api36, Android 16. Touching the widget: "Visual alerts are ON. Tap to turn them OFF until you tap again." After a TalkBack double tap: "Visual alerts are PAUSED. Tap to turn them back on now."; after another: ON again. |

## Seen once, not reproduced

During check 7, the first time the font scale was set to 2.0, Beacon
opened with the switch OFF although the widget had shown ON about five
seconds before; the widget then showed PAUSED too, so the saved pause had
really changed. That time the app was opened one second after the font
change and eight quick shell swipes were sent over the switch card to
scroll to the top. Replayed three times (a widget tap, the font change
1 s later, Beacon opened, the same eight swipes, and two single swipes on
the card), the state never changed again. Shell swipes reach the app as
plain touches, not a person's finger, so this may be the test input. The
app rows on Home are switches of the same kind and have no such report.
If it shows up on the phone, it is a defect to fix.

## Not tested

- **The test phone** (Redmi 15C 5G, Hebrew, right to left): the switch,
  the Widget section and the widget's new tap. The switch card uses the
  same layout as Home's app rows, which were checked right to left there.
- **TalkBack speaking the new state after a double tap**, on the phone.
- **A message while alerts are OFF from the widget:** an open-ended pause
  is the one Home's "Pause all alerts" sets, which ignored a WhatsApp
  message on the phone
  ([`2026-11-pause-on-battery.md`](2026-11-pause-on-battery.md)); not run
  again with 1.0.3.
- **Android 10 to 12.**
- **1.0.3 from Google Play:** on internal testing since 14 November 2026
  at 18:06 ([Play record](2026-11-play-release.md)), not yet installed
  from Play on a phone.
