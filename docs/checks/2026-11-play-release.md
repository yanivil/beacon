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
| Store listing update approved | 14 November, 10:34 | Submission activity: submission 2 (sent at 09:24 by Play's clock), "Store Listing", 3 changes (the full description, the phone screenshots and the feature graphic), "Published" on 14 November at 10:34. |
| Closed test sent for review | 14 November, 17:13 (Play's clock) | The owner said "Create it now" with 4 testers so far, so Google's review of the track runs while more testers are found. The track is the one Play already has, "Closed testing - Alpha" (track 4697746517901755908), not a new one. Testers: the "Beacon testers" email list, which holds the owner's 4 testers. Feedback: the public contact email. Countries: all 177 and "rest of world", the owner's choice. Release 3 (1.0.2), added from the app bundle library, with the release notes copied from the internal release; the same two warnings. With the owner's yes, "Submit 7 changes for review" (the release's full rollout, the countries, rest of world, the track's countries unsynced from production, the track resumed, the email list and the feedback email). Play's quick checks then ran ("Up to 14 minutes remaining"). Once Google approves it, testers join on Android at `https://play.google.com/store/apps/details?id=dev.beacon` or on the web at `https://play.google.com/apps/testing/dev.beacon`. |
| Closed test approved | 14 November, 17:23 | Submission activity: submission 3, "Closed testing - Alpha", 7 changes, "Published" on 14 November at 17:23, ten minutes after it was sent. The track shows 3 (1.0.2) "Available to selected testers" in 178 countries and regions. While it was in review the owner added 7 more addresses to "Beacon testers" (11 by about 18:00; email lists apply at once and need no review). At 18:07 the dashboard's production requirements said "0 testers currently opted-in": the 14 days haven't started. |
| Release 4 (1.0.3) | 14 November, 18:06 | Built from `main` at `6a4df59` (PR #40, the Visual alerts switch and the widget choice): versionCode 4, signed with the upload key, the same three permissions as 1.0.2's build and no `INTERNET`. The owner uploaded `beacon-1.0.3-4.aab` with the file picker. The same two warnings; the supported devices didn't change (8,733 phones and 5,739 tablets). Release notes, as typed: You can now turn alerts OFF until you turn them back ON. Use the new Visual alerts switch at the top of Alert settings, or choose in Alert settings that one tap on the widget turns alerts OFF until you tap it again. Published with the owner's yes ("Yes, release it"): "Available to internal testers". Not on the closed test yet. |
| 1.0.3 to the closed test, sent for review | 14 November, 18:10 | With the owner's yes ("Yes, move it"): a new release on "Closed testing - Alpha" with 4 (1.0.3) from the app bundle library and the release notes copied from the internal release. The same two warnings; the supported devices didn't change. Publishing overview listed one change, "4 (1.0.3), Start full rollout"; "Submit 1 change for review", then Play's quick checks ("Up to 14 minutes remaining"). Until Google approves it, the closed test keeps offering 1.0.2, and testers can still join. |
| Store listing: 1.0.3's full description, sent with the closed test's 1.0.3 | 14 November, 18:19 | With the owner's yes ("Yes, send it"): Play's full description was first checked against the text in `listing.md` before 1.0.3 (both 2,823 characters, the same words), then replaced with today's [`listing.md`](../store/listing.md) text (2,983 characters; the same SHA-256 as the file). Its only change is the pause paragraph: the Visual alerts switch, "stops message alerts", and the widget's no-time-limit choice. On the review step Play asked the AI asset question again; "Don't label assets" was already selected, the owner's answer of 11 November. Publishing overview listed one new change, "Change full description". Sending it asked "Do you want to restart your review?", because 1.0.3 for the closed test was still in its quick checks. That was new, so the dialog was cancelled and the owner asked; they chose "Restart with both". Now one review holds both changes ("Up to 14 minutes remaining" for the quick checks). |
| 1.0.3 and its description approved | 14 November, 18:26 | Checked on 15 November: Submission activity lists submission 5 ("Closed testing - Alpha, Store Listing", sent at 18:18 by Play's clock) as "Published" on 14 November at 18:26, with two changes, "4 (1.0.3), Start full rollout" and "Change full description". The closed track shows 4 (1.0.3), "Available to selected testers", in 178 countries and regions. Publishing overview has nothing waiting. |
| Testers, 15 November | 15 November | The "Beacon testers" list has 15 addresses (12 besides the owner's own two); the owner sent the join link that day. That morning Play's dashboard showed "2 testers currently opted-in". The 14 days start once 12 are opted in. |
| Release 5 (1.0.4) | 15 November, 11:48 | Built from `main` at `9574253` (PR #44, the version line at the end of Alert settings): versionCode 5, signed with the upload key, the same three permissions as 1.0.3's build and no `INTERNET`. The owner uploaded `beacon-1.0.4-5.aab` with the file picker. The same two warnings (no deobfuscation file; native code without debug symbols); the supported devices didn't change (8,733 phones and 5,739 tablets). Release notes, as typed: Alert settings now ends with Beacon's version, such as "Beacon version 1.0.4", so you can see which version you have. Published with the owner's yes ("Yes, release it"): "Available to internal testers". Not on the closed test yet. |
| 1.0.4 to the closed test, sent for review | 15 November, 11:51 | With the owner's yes ("Yes, send it"): a new release on "Closed testing - Alpha" with 5 (1.0.4) from the app bundle library and the same release notes as the internal release. The same two warnings; the supported devices didn't change; rollout 100%. Nothing else was in review, so no review was restarted. Publishing overview listed one change, "5 (1.0.4), Start full rollout"; "Submit 1 change for review", then Play's quick checks ("Up to 14 minutes remaining"). Until Google approves it, the closed test keeps offering 1.0.3, and testers can still join. |
| 1.0.4 approved for the closed test | 15 November, 12:05 | Checked at 12:26: Submission activity lists submission 6 ("Closed testing - Alpha", sent at 11:52 by Play's clock) as "Published" on 15 November at 12:05, thirteen minutes later, with one change, "5 (1.0.4), Start full rollout". At 12:26 Play's dashboard showed "4 testers currently opted-in". |
| Internal testing gets its own list | 15 November, about 15:45 | Play still showed "4 testers currently opted-in" at 14:16 and 15:31, though the owner counted more people who had joined. The internal and closed tracks both used "Beacon testers", and Google's help ("Set up an open, closed, or internal test", "Can I run multiple tests per app at the same time?") says a user who opts into the internal test is no longer eligible for a closed test until they leave the internal test. So, at the owner's request ("remove all (beside aba and michal) from the internal testers"), "Beacon testers" was left as it was, a new email list, "Beacon internal testers", was made with the two addresses the owner named, and the internal track now uses only that list. Checked after a reload: internal testing uses "Beacon internal testers" (2 addresses) and the closed test uses "Beacon testers" (15). Play applied it at once: Publishing overview had nothing waiting and nothing went to review. Before the change, the owner's main account was checked in the browser: the closed test's web link said "You are a tester." and the internal link still offered "Accept invite", so it is on the closed test. Play Console doesn't show who opted in; a tester can check on the web link, signed in with the address on the list. |
| More testers | 15 November, about 17:40 to 21:30 | At 17:34 Play's dashboard showed "5 testers currently opted-in", the first change since 12:26. At the owner's request, 5 more addresses went into "Beacon testers", one at a time (three Gmail addresses and two others that, the owner said, those testers use with Google Play). After each save a reload showed the list's new size, ending at 20, still on the closed test only. Email lists apply at once, with no review. The dashboard showed "10 testers currently opted-in" at 22:00 and "11" at 23:17. |
| 12 testers opted in | 16 November, between 08:35 and 10:00 | At 08:35 the dashboard still showed "11 testers currently opted-in". At 10:00 its production requirements showed "Have at least 12 testers opted-in to your closed test" as done (ticked; the number is no longer shown). "Run your closed test with at least 12 testers, for at least 14 days" is still open and shows no day count, and "Apply for production" is greyed out. If the 14 days started on 16 November, they end on 30 November at the earliest; if fewer than 12 testers stay opted in, they start again. |
| Day count shown, 23 on the list | 16 and 17 November | On 16 November, at the owner's request, 3 more addresses went into "Beacon testers", one at a time (all Gmail). After each save a reload showed the list's new size, ending at 23, still on the closed test only. At 22:56 on 17 November (the Mac's clock) the dashboard's "Run your closed test with at least 12 testers, for at least 14 days" step showed a count for the first time: "12 testers have currently been opted in for 1 day". "Have at least 12 testers opted-in to your closed test" was still ticked, and "Apply for production" was greyed out. Play doesn't say whether 12 is the real number or only the requirement, so how many testers are opted in above 12 isn't known. One day counted by that evening fits the 14 days starting on 16 November, so they end on 30 November at the earliest. |

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
- **The closed test's 14 days:** 12 testers had opted in by 10:00 on
  16 November, and at 22:56 on 17 November Play showed "12 testers
  have currently been opted in for 1 day". The 14 days end on
  30 November at the earliest.
- **1.0.3 from Play on the test phone, with a message:** the owner saw
  version 1.0.3 there on 15 November and turned alerts off with the
  Visual alerts switch, but no message was sent while alerts were off,
  and the widget's "Turn OFF until I tap again" choice wasn't tried.
- **1.0.4 from Play on the test phone:** on the closed test since 12:05
  on 15 November, not yet installed from Play. The phone's account is a
  closed tester, not an internal one.
- **Beacon from Play on Android 10 to 12:** only the Android 16 test phone
  and the Android 16 emulator were used.
- **1.0.2 with the GitHub app installed:** neither the test phone nor the
  emulator has it; `PrivacyPolicyIntentTest` covers it with a fake app.
- **A plain SMS from the phone network with 1.0.2:** the 14 November
  message reached Google Messages as an RCS chat message. Beacon reads the
  notification either way, as the earlier SMS checks did.
