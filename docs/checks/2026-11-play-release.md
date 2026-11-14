# Play release: checks

This page records the checks for Beacon 1.0.0 and 1.0.1 on Google Play
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
the message under the button when no browser is installed (and no toast), and the section at font scale 2.0
with no clipped text (378 tests in all).

## Play Console setup and internal testing

Done in the owner's Play Console account ("Bernhard Software") in the
browser pane, with every answer from
[`play-console-answers.md`](../store/play-console-answers.md). Questions
that weren't on that sheet were asked in chat first.

| Step | When | What Play Console showed |
|---|---|---|
| App created | 11 November 2026, about 22:27 | "Beacon: Visual Message Alerts", package `dev.beacon`, app id 4975241735920109220. The create form also asked for three declarations (the owner said yes to all three) and offered "automatic protection" (installer check); the owner kept it on. |
| Store listing and settings | 11 November, evening | The texts from [`listing.md`](../store/listing.md), the icon, the feature graphic and five 1080 × 1920 screenshots; category Communication; the contact email. The AI-generated assets label: "Don't label" (the owner). |
| App content | 11 November, evening | All ten declarations saved: privacy policy URL, no ads, no sign-in, the content rating (ESRB Everyone, PEGI 3, IARC 3+), target audience 18 and over, Data safety "no data collected or shared", no advertising ID, not a government app, no financial or health features. |
| Testers | 12 November, early morning | An email list, "Beacon testers", with the owner's addresses (they stay in Play Console only). |
| Release 1.0.0 (1) | 12 November, 09:49 | "Available to internal testers", "Not reviewed". Play App Signing with a Google-generated key. Bundle: API 29+, target SDK 36. Two warnings, both expected: no deobfuscation file (shrinking is off) and no native debug symbols. |
| Release 2 (1.0.1) | 12 November, 10:22 | The same two warnings; the supported devices didn't change (8,722 phones, 5,732 tablets). Release notes: "Adds a Privacy section at the end of Alert settings, with a button that opens the privacy policy." Published with the owner's yes. |
| Sent for review | 12 November, about 10:27 | The owner said yes to "Submit 8 changes for review": the store listing, content rating, target audience, privacy policy, ads, Data safety, health apps and category. Play's quick checks passed, and by about 10:42 Publishing overview said "Your changes are now in review". Managed publishing is off, so the changes go live when Google approves them. |
| Google's review | 12 November | Approved. Play Console's notification "App update published" is dated 12 November. Seen on 14 November: the app's name is "Beacon: Visual Message Alerts" and Publishing overview had nothing waiting. |
| Release 3 (1.0.2) | 14 November, 09:23 | Built from `main` at `04772d9`: versionCode 3, signed with the upload key, the same permissions as 1.0.1's build and no `INTERNET`. The same two warnings; the supported devices didn't change from 1.0.1 (8,731 phones and 5,733 tablets in Play's device catalog that day). Release notes, as typed: The "Read the privacy policy" button now always opens your web browser, and TalkBack says where it leads. If no browser can open the page, Beacon now says so clearly. Published with the owner's yes: "Available to internal testers". |
| Store listing update sent for review | 14 November, about 09:25 | The owner uploaded the new feature graphic ("See when messages arrive") and the widget screenshot on black; the old ones were removed, so the listing has 1 feature graphic and 5 screenshots in their old order. The full description now matches [`listing.md`](../store/listing.md) ("color"). Play asked the AI asset question again; the answer is "Don't label assets", the owner's answer of 11 November for images made the same way. With the owner's yes, "Submit 3 changes for review": the full description, the phone screenshots and the feature graphic. Play's quick checks then ran ("Up to 14 minutes remaining"). Until Google approves them, the store keeps the old graphic, screenshot and spelling. |

Until Google's first review was done, testers saw the temporary name
"dev.beacon (unreviewed)"; since then they see "Beacon: Visual Message
Alerts".

## Beacon from Play on the test phone

| | |
|---|---|
| Phone | Redmi 15C 5G (`2508CRN2BE`), Android 16, HyperOS 3, Hebrew, font scale 1.45 |
| Install | The owner uninstalled the debug build, opened the opt-in link, installed Beacon from Play and granted the permissions again |
| Date | 12 November 2026, 10:44 to 10:54 |

| Check | Result |
|---|---|
| 8. It comes from Play, at 1.0.1 | Tested on Redmi 15C 5G, Android 16. `dumpsys package dev.beacon`: `versionName=1.0.1`, `versionCode=2`, `installerPackageName=com.android.vending`, installed 10:44:06. |
| 9. No internet permission in the copy Play delivers | Tested on Redmi 15C 5G, Android 16. Requested permissions: `SYSTEM_ALERT_WINDOW`, `WAKE_LOCK`, `dev.beacon.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` and `com.android.vending.CHECK_LICENSE`. The last one isn't in the release build from the Mac: Play's automatic protection adds it for its licence check, which asks the Play Store app on the phone. There is no `INTERNET` permission, so `PRIVACY.md` and the Data safety answer still hold. |
| 10. Setup is finished | Tested on Redmi 15C 5G, Android 16. Display over other apps allowed (`appops`), Beacon's notification listener enabled, Beacon on the battery exemption list (`deviceidle whitelist`). Home showed "Visual alerts ON" with no Finish setup card. |
| 11. The Privacy section and its button | Tested on Redmi 15C 5G, Android 16. At the end of Alert settings, at font scale 1.45 in the right-to-left layout of the phone's Hebrew locale, nothing cut off. The button opened Chrome on `github.com/yanivil/beacon/blob/main/PRIVACY.md`, showing the version merged 34 minutes before. Back returned to Beacon. |
| 12. With the phone locked, a message flashes | Tested on Redmi 15C 5G, Android 16. The owner locked the phone with the power button at 10:53:20 and had an SMS sent to it. At 10:53:38 Beacon's alert screen started while the display was off and turned the screen on (`WAKE_REASON_APPLICATION`, `TURN_ON`), over the lock screen. The notification came from Google Messages. The camera light came on 7 times, once a second (Medium, 60 a minute), for Beacon's process (`CameraService`); `batterystats` shows `+flashlight` for Beacon's new uid 10300 from 10:53:39. The owner touched Stop at 10:53:44.15; the alert screen closed and the light went off at 10:53:44.23. The owner saw it flash. |

Seen on the way: `dumpsys` reports `minSdk=32` for the copy Play delivered to
this Android 16 phone, while the bundle's is 29 (Play Console shows "API
29+"). Play builds its APKs for ranges of devices, so this is probably its
build for newer Android versions; a phone with Android 10 or 11 wasn't
tried.

## Version 1.0.2: the privacy link's fixes, on the emulator

| | |
|---|---|
| Emulator | `beacon-api36` (`emulator-5554`), Android 16, font scale 1.0, Chrome installed (its first-run screen not accepted) |
| Build | `./gradlew :app:assembleRelease` on `fix/deferred-minors`: versionName 1.0.2, versionCode 3, signed with the upload key, no `INTERNET` permission; the merged manifest has the new VIEW/https `<queries>` entry |
| TalkBack | Android Accessibility Suite 16.0.0.738667889, read from its verbose log and driven through the emulator console, as in [`2026-11-step2-pr1.md`](2026-11-step2-pr1.md); the log level was set back to ERROR and TalkBack switched off afterwards |
| Date | 12 November 2026, about 11:09 to 11:35 |

| Check | Result |
|---|---|
| 13. The button names the browser | Tested on emulator beacon-api36, Android 16. A tap logged `START u0 {act=android.intent.action.VIEW cat=[android.intent.category.BROWSABLE] dat=https://github.com/... pkg=com.android.chrome ...}`; 1.0.1 started the same intent with no package. The label is 24 sp. |
| 14. TalkBack on the Privacy section | Tested on emulator beacon-api36, Android 16. Touching the summary line, then swiping right: "Beacon keeps your settings on this phone and sends nothing anywhere.", then "Read the privacy policy. Button. Double-tap to open in your web browser", then "Opens in your web browser.". A TalkBack double tap on the button opened Chrome (`pkg=com.android.chrome`). |
| 15. A traversal index for the note | Tested on emulator beacon-api36, Android 16, and dropped. A first version gave the note `traversalIndex = -1` in a traversal group; Robolectric showed the order changed, but TalkBack still read the button first. So the button's click label carries "open in your web browser" instead. |

## Version 1.0.2 from Play on the test phone

| | |
|---|---|
| Phone | Redmi 15C 5G (`2508CRN2BE`), Android 16, HyperOS 3, Hebrew (`he-IL`), font scale 1.45 |
| Install | The owner updated Beacon from Play over 1.0.1, so the permissions stayed granted |
| Date | 14 November 2026, 09:30 to 09:34 |

| Check | Result |
|---|---|
| 16. It comes from Play, at 1.0.2 | Tested on Redmi 15C 5G, Android 16. `dumpsys package dev.beacon`: `versionName=1.0.2`, `versionCode=3`, `installerPackageName=com.android.vending`, updated 09:30:19; first installed 12 November 10:44:06, and the uid is still 10300. |
| 17. No internet permission, and setup kept | Tested on Redmi 15C 5G, Android 16. The same four requested permissions as 1.0.1 from Play, with no `INTERNET`. After the update, Display over other apps was still allowed (`appops`), Beacon's notification listener still enabled, and Beacon still on the battery exemption list (`deviceidle whitelist`). |
| 18. The Privacy button opens the browser by name | Tested on Redmi 15C 5G, Android 16. At the end of Alert settings, in the right-to-left layout at font scale 1.45, a tap on "Read the privacy policy" logged `START u0 {act=android.intent.action.VIEW cat=[android.intent.category.BROWSABLE] dat=https://github.com/... pkg=com.android.chrome ...} ... callingPackage dev.beacon`. Chrome showed "Beacon privacy policy", "Last updated: 12 November 2026". Back returned to Beacon. 1.0.1 started the same intent with no package. |
| 19. With the phone locked, a message flashes | Tested on Redmi 15C 5G, Android 16. The owner locked the phone with the power button at 09:33:17.86 and had a message sent to Google Messages; it arrived as an RCS chat message (Google Messages' receiving pipeline started at 09:33:31.6). At 09:33:34.37 Beacon's alert screen turned the screen on (`WAKE_REASON_APPLICATION`, `TURN_ON`) over the lock screen; `AlertActivity` was displayed 323 ms later. The camera light came on 5 times, once a second (Medium, 60 a minute), for Beacon's process (`CameraService`). The owner touched Stop at 09:33:38.97 and the light went off at 09:33:38.98. The owner saw it flash. |

## Not tested

- **Telegram,** as in every check so far.
- **WhatsApp with the copy from Play:** the locked check used an SMS.
  WhatsApp alerts were checked on the debug build on 11 November.
- **Google's review of the 14 November listing changes:** sent; the
  result isn't in yet.
- **Beacon from Play on Android 10 to 12:** only the Android 16 test phone
  and the Android 16 emulator were used.
- **1.0.2 with the GitHub app installed:** neither the test phone nor the
  emulator has it; `PrivacyPolicyIntentTest` covers it with a fake app.
- **A plain SMS from the phone network with 1.0.2:** the 14 November
  message reached Google Messages as an RCS chat message. Beacon reads the
  notification either way, as the earlier SMS checks did.
