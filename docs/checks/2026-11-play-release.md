# Play release: checks

This page records the checks for Beacon 1.0.0 on Google Play
([spec](../specs/2026-11-11-play-release-design.md),
[plan](../plans/2026-11-11-play-release.md)). It grows as the release moves
on: first the release build on the emulator, then the Play Console setup and
the internal test, then Beacon installed from Play on the test phone.

Results are labelled **"Tested on emulator beacon-api36, Android 16"**,
**"Tested on Redmi 15C 5G, Android 16"** or **"Not tested"**.

## Release build on the emulator

| | |
|---|---|
| Emulator | `beacon-api36` (`emulator-5554`), Android 16, font scale 1.0 |
| Build | `./gradlew :app:assembleRelease` at `db05af2`: `dev.beacon`, versionName 1.0.0, versionCode 1, signed with the upload key (`CN=Beacon upload key`), shrinking off |
| Install | The debug build was uninstalled first (its key differs), then `adb install` of `app-release.apk` |
| Date | 11 November 2026, about 21:58 to 22:01 |

Before the checks, `grep -rn 'BuildConfig.DEBUG' app/src/main` found one
place only: `BeaconApp.kt:132`, the decision log. So the release build
differs from debug only in writing no decision log, apart from being signed
with another key.

| Check | Result |
|---|---|
| 1. It installs and opens on Home with the Finish setup card | Tested on emulator beacon-api36, Android 16. On this first start the splash (the bolt on white) showed for a few seconds; then Home showed "Finish setup" with Notification access and Display over other apps OFF. |
| 2. The test alert runs and ends | Tested on emulator beacon-api36, Android 16. "Test alert" on Home opened the yellow test screen with its countdown ("2 seconds left" a second in), and it went back to Home by itself after its 3 seconds. |
| 3. The widget can be placed, and a tap pauses alerts | Tested on emulator beacon-api36, Android 16. Placed from the launcher's widget list, it showed "Visual alerts", ON and "Tap to pause for 1 hour". One tap: PAUSED, "Back ON by itself at 11:00 PM" and "Tap to turn ON now", with the resume alarm set for 23:00:55 (`RTC_WAKEUP`, `window=+10m0s0ms`). A second tap: ON again. |
| 4. With setup finished and the screen off, an SMS flashes | Tested on emulator beacon-api36, Android 16. Setup was finished over adb (notification access, display over other apps, the battery exemption), and Home showed "Visual alerts ON" with no setup card. With the screen asleep, `adb emu sms send 5551234 "Release check"` woke it to the alert screen: "New message", "Texts (SMS)", the blinking yellow and Stop. Stop closed it. |
| 5. The release build writes no decision log | Tested on emulator beacon-api36, Android 16. During check 4, `logcat -s Beacon:D` had no lines. |

Seen on the way: the release build isn't debuggable, so `run-as dev.beacon`
can't read its stored settings. Checks on a release build read Android's own
records instead (alarms, battery history, activities), as the check from
Play on the test phone will.

## Version 1.0.1: the privacy link, on the emulator

| | |
|---|---|
| Emulator | `beacon-api36` (`emulator-5554`), Android 16, font scale 1.0 |
| Build | `./gradlew :app:assembleRelease` on `feature/in-app-privacy-link` (from `cb0f6a9`): versionName 1.0.1, versionCode 2, signed with the upload key (`CN=Beacon upload key`), no `INTERNET` permission in the merged manifest |
| Date | 12 November 2026, about 10:00 to 10:04 |

| Check | Result |
|---|---|
| 6. Alert settings ends with the Privacy section | Tested on emulator beacon-api36, Android 16. Below the colour swatches: "Privacy", "Beacon keeps your settings on this phone and sends nothing anywhere.", the full-width "Read the privacy policy" button and "Opens in your web browser.", above the Test alert button and the tab bar. |
| 7. The button opens the policy in the browser | Tested on emulator beacon-api36, Android 16. One tap: `ActivityTaskManager` logged `START u0 {act=android.intent.action.VIEW cat=[android.intent.category.BROWSABLE] dat=https://github.com/...}` to Chrome, from uid 10214, which is `dev.beacon`. This emulator's Chrome had never been opened, so it showed its own first-run screen; its terms weren't accepted, so the page itself wasn't seen in Chrome. Back returned to Beacon. The address, `https://github.com/yanivil/beacon/blob/main/PRIVACY.md`, answered HTTP 200 from the Mac. |

The unit tests check the rest: the intent's action, address and category,
the toast when no browser is installed, and the section at font scale 2.0
with no clipped text (378 tests in all).

## Not tested

- **The camera light** in the release build: the emulator has none.
- **The release build on a phone,** before Beacon is installed from Play.
- **Telegram,** as in every check so far.
- **The privacy page inside a browser on a phone:** on the emulator Chrome
  stopped at its first-run screen.
- **Whether Play's automatic protection changes the permissions** of the
  copy Play delivers. The check from Play on the test phone reads them.
