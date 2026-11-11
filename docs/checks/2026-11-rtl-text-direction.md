# Right-to-left text direction: checks

This page records the fix for the phone checks' defect P1, on
`fix/rtl-text-direction`:
- its ruling, with what it costs if wrong;
- the tests and the checks on the build machine;
- an emulator comparison of `main`'s build with the fixed one, in Hebrew
  and in English.

Every emulator result is labelled **"Tested on emulator beacon-api36,
Android 16"** or **"Not tested"**.

## The defect

On the test phone, set to Hebrew, Beacon mirrors its layout as it should,
but it also laid every English line out right to left
([`2026-11-phone-checks.md`](2026-11-phone-checks.md), P1). Full stops moved
to the start of the line, and a line starting with a number had its parts
swapped: "1 minute" read "minute 1", "60 blinks a minute" read "blinks a
minute 60".

The cause: Beacon's text styles leave the text direction unset, and Compose
1.12.1 then lays text out in the layout's direction. Its
`resolveTextDirection` maps an unset direction to `Ltr` or `Rtl` from the
layout, as its bytecode shows; only `TextDirection.Content` looks at the
words.

## Ruling

- **R1. All of Beacon's text is drawn by one function, `BeaconText`.**
  - It sets each text's direction from its own words
    (`TextDirection.Content`). English reads left to right, and a future
    Hebrew translation right to left.
  - It keeps each text at the layout's start edge. Compose's
    `TextAlign.Start` follows the text's direction, which would move
    English lines to the left of a mirrored layout. Compose has no
    alignment that follows the layout, so `BeaconText` turns Start and End
    into Left or Right from the layout direction. Centred text stays
    centred.
  - This is the one place in the app that uses left and right. It derives
    them from the layout direction, so a right-to-left layout keeps working,
    which is what the start/end rule in `CLAUDE.md` is for.
  - All 16 files that drew text with `BasicText` now use `BeaconText`. A
    test fails if any other file calls `BasicText`.
  - The Test alert button's "(3 sec)" no longer sets its own direction:
    `BeaconText` does it for every text.
  - Cost if wrong: on a Hebrew phone, English lines sit on the right, where
    they sat before this fix, rather than on the left, where Android's own
    views put them. Moving them is a change in `BeaconText` alone.

## Where the rules and the design disagree

Nowhere. The design is left to right; `CLAUDE.md` asks for a right-to-left
version to stay possible, and this keeps it so.

## Build machine

| Check | Result |
|---|---|
| New tests, `BeaconTextTest` | 6 tests. Against a `BeaconText` that drew text as before, 4 failed: the style handed to the text has `TextDirection.Content` and right alignment in a right-to-left layout; an English line sits at the right edge in a right-to-left layout (it was at the left, 75.8 of 300 px); the style mapping; and the source check, which listed the 16 files. The 2 that passed are guards: left-to-right text stays at the left edge, and centred text stays centred. All 6 pass with the fix. |
| Robolectric's limit | Robolectric's text engine works out a paragraph's direction from its words whatever direction it is asked for: with the old code, "1 minute" in a right-to-left layout came out left to right there, so the tests can't show "minute 1" itself. They check the style and the line's position; the reading order is checked on the emulator below. |
| `./gradlew :app:testDebugUnitTest` | 333 tests, 0 failures: the 327 before, and the 6 new ones. The screen, TalkBack and largest-font tests pass unchanged. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:lintDebug --rerun-tasks --warning-mode all` | No Kotlin compiler warnings (`Problem found` lines), and lint reports "No issues found". |

## Emulator

| | |
|---|---|
| Emulator | AVD `beacon-api36`, Pixel 7 profile (1080 × 2400 px, 420 dpi), run as `emulator-5554` |
| System image | Android 16 (API 36), build `BE2A.250530.026.F3` |
| Builds | Debug APKs of `main` at `2eb7577` (the merge of #14 since then changed only docs) and of `fix/rtl-text-direction`, each installed with `adb install -r` in turn; app data was kept |
| Right-to-left | Beacon alone was given Hebrew as its language (`cmd locale set-app-locales dev.beacon --locales he-IL`, Android 13 and later). Beacon has no Hebrew strings, so it shows its English text in a right-to-left layout, as on the test phone. Forcing right-to-left with `debug.force_rtl` didn't change the layout, so it was set back. |
| Start state | Font size 1.0, dark mode off. Notification access and "Display over other apps" off, so Home shows the "Finish setup" card. Medium speed, Custom length 10 minutes, Yellow chosen. |
| Date | 11 November 2026 |

| Check | Result | Screenshots |
|---|---|---|
| `main` in Hebrew | Tested on emulator beacon-api36, Android 16. The emulator shows the phone's defect: "minute 1" in Home's summary, and "blinks a minute 30" and "blinks a minute 60" under the speeds. | [01](rtl-text-direction/01-main-summary.png), [03](rtl-text-direction/03-main-speeds.png) |
| The fix in Hebrew | Tested on emulator beacon-api36, Android 16. "1 minute", "60 blinks a minute" and "90 blinks a minute" read correctly, still on the right of their rows. The setup card's sentences end with their full stops ("…for your messages."). The layout is still mirrored: "Beacon" on the right, Home the right tab, and "(3 sec)" to the left of "Test alert". | [02](rtl-text-direction/02-fixed-summary.png), [04](rtl-text-direction/04-fixed-speeds.png), [05](rtl-text-direction/05-fixed-setup-card.png) |
| The test alert in Hebrew | Tested on emulator beacon-api36, Android 16. "Medium, 60 a minute" and "1 minute" read correctly. | [06](rtl-text-direction/06-fixed-test-alert.png) |
| English, left to right | Tested on emulator beacon-api36, Android 16. Beacon's language set back to the system's (English). The top and the end of Home and of Alert settings are pixel-identical to `main`'s build below the status bar (0 rows differ). The first capture of `main` caught its splash screen, so Home's top was captured again on both builds after a longer wait. | |

Afterwards Beacon's language is the system's again, `debug.force_rtl` is 0
(setting and property), the font size is 1.0, and the fixed build is left
installed. `/sdcard/ui.xml` was deleted.

## Not tested

- **The test phone with this build.** It still runs the phone checks'
  widget preview; the next build installed there will carry this fix.
- **A real Hebrew translation:** there is none yet.
- **Arabic and other right-to-left languages,** and **mixed lines**, such
  as an English app name inside a Hebrew sentence.
- **The widget:** Glance draws it with the launcher's TextViews, whose
  default direction already follows the words; on the test phone its
  English lines read correctly.
