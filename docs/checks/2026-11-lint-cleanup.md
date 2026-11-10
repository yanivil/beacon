# Lint clean-up: checks

This page records the lint clean-up on `fix/lint-warnings`:
- its rulings, each with what it costs if wrong;
- the checks on the build machine;
- one emulator check of the only text that changed form, the speed lines.

Every emulator result is labelled **"Tested on emulator beacon-api36,
Android 16"** or **"Not tested"**. Nothing on this page was run on the real
phone.

## What lint reported

On `main` at `a327dc5` (PR 4 merged), `./gradlew :app:lintDebug` reported
0 errors and 20 warnings. Ten were about Beacon's own code and resources,
and this clean-up deals with all of them:

| Warning | Where | Count |
|---|---|---|
| `ModifierParameter` | `AdaptiveGrid`, `FilledButton`, `OutlineButton`, `OptionButton` | 4 |
| `PrivateResource` | `state_on`, `state_off` in `strings.xml` | 2 |
| `UnusedAttribute` | `targetCellWidth`, `targetCellHeight` in `beacon_widget_info.xml` | 2 |
| `PluralsCandidate` | `speed_rate` in `strings.xml` | 1 |
| `DataExtractionRules` | `<application>` in the manifest | 1 |

The other ten are about newer versions:
- Two are of `compileSdk` and `targetSdk`, which `CLAUDE.md` fixes at 36
  (ruling L6).
- Eight are of Gradle and the libraries (ruling L7).

## Rulings

- **L1. `speed_rate` becomes a plural.** It has "one" ("1 blink a
  minute") and "other" ("30 blinks a minute"), read with
  `pluralStringResource`, the same way as the minutes and seconds.
  - Cost if wrong: none in English, since the presets are all "other". A
    translator gets the two forms to fill.
- **L2. The setup row's "On" and "Off" get names of their own,**
  `setup_state_on` and `setup_state_off`.
  - The old names were the same as Compose's private `state_on` and
    `state_off`, which TalkBack reads for every switch. So Beacon's strings
    replaced Compose's English text, with the same words. Compose's
    translations still applied: `main`'s APK holds 86 language values
    under that name besides Beacon's English one.
  - Nothing sounded different, but a change to the setup row's words would
    have changed every switch's.
  - Cost if wrong: none. The English text is the same, and the app rows'
    "On" and "Off" now come from Compose itself; a new test reads them
    through TalkBack's node info.
- **L3. `modifier` is the first optional parameter** of the four shared
  composables, as Compose's guidelines ask. The KDoc follows the new order.
  - Every swapped parameter has a different type from `Modifier`, so a call
    that passed one by position would no longer compile. Every call still
    compiles and every test passes.
  - Cost if wrong: none at run time.
- **L4. `DataExtractionRules` is ignored on `<application>`, with the
  reason in a comment.**
  - Lint asks for `fullBackupContent` rules for Android 10 and 11. There,
    `allowBackup="false"` already stops every backup and transfer, and
    Android doesn't read those rules at all.
  - Cost if wrong: none for data, since backup stays off. The rest is Not
    tested, since no Android 10 or 11 device was used.
- **L5. `UnusedAttribute` is ignored in `beacon_widget_info.xml`, with the
  reason in a comment.**
  - `targetCellWidth` and `targetCellHeight` are only read from Android 12.
    Android 10 and 11 use `minWidth` and `minHeight`, as the file already
    said (PR 3).
  - Cost if wrong: none. The attributes don't change, and the compiled file
    is the same as `main`'s.
- **L6. `compileSdk` and `targetSdk` stay at 36.**
  - `CLAUDE.md` fixes both, so lint's two reminders that 37 exists are
    silenced with `//noinspection` on those two lines only, under a comment
    saying why.
  - Cost if wrong: lint stops reminding about API 37 until the owner
    raises the rule.
- **L7. The eight version warnings are left as they are.** They are for:
  - Gradle 9.9.0;
  - Compose BOM 2026.09.00, and the Compose UI test libraries 1.12.1;
  - Lifecycle 2.11.0;
  - Core KTX 1.19.1;
  - the Kotlin Compose plugin 2.4.21.

  Bumping them is the owner's decision.
  - Cost if wrong: none. Nothing changes.

## Build machine

| Check | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | Passes: 327 tests, 0 failures. That is the 324 on `main`, plus 3 new ones: `everySpeedShowsItsRate` and `speedRateIsAPlural` in `AlertSettingsScreenTest`, and `appSwitchesSpeakOnAndOff` in `HomeScreenTest`. |
| The new tests can fail | `speedRateIsAPlural` didn't compile before `speed_rate` became a plural. In `appSwitchesSpeakOnAndOff`, a `state_on` string of "Yes" planted in `strings.xml` failed with `expected:<[On]> but was:<[Yes]>`, and the file was then restored. |
| `./gradlew :app:lintDebug` | Passes: 0 errors, 8 warnings, all of them the version warnings of ruling L7. |
| Merged manifest | The same as `main`'s apart from the new comment. `tools:ignore` doesn't reach it. |
| Compiled APK | `aapt2 dump xmltree` of `AndroidManifest.xml` and `res/xml/beacon_widget_info.xml` matches the APK built from `main` (PR 4's build, pulled from the emulator). The only differences are line numbers and resource ids, which moved with the two new strings. |
| Strings in the APK | `plurals/speed_rate` has one="%1$d blink a minute" and other="%1$d blinks a minute". `setup_state_on` and `setup_state_off` are "On" and "Off". `state_on` is "On", now Compose's own. |

## Emulator

| | |
|---|---|
| Emulator | AVD `beacon-api36`, Pixel 7 profile (1080 × 2400 px, 420 dpi), run as `emulator-5554` |
| System image | Android 16 (API 36), build `BE2A.250530.026.F3` |
| Build | Debug APK of `fix/lint-warnings`, installed over PR 4's build with `adb install -r`; app data was kept |
| Start state | Font size 1.0. Notification access off, as PR 4's checks left it, so Home shows the "Finish setup" card. Medium speed chosen. |
| Date | 10 November 2026 |

| Check | Result | Screenshot |
|---|---|---|
| The speed lines on Alert settings | Tested on emulator beacon-api36, Android 16. `uiautomator dump` reads "30 blinks a minute" under Slow, "60 blinks a minute" under Medium, and "90 blinks a minute" then "Highest safe speed" under Fast, as before. | [01](lint-cleanup/01-speed-lines.png) |
| The composables with moved parameters | Tested on emulator beacon-api36, Android 16. On Home, `uiautomator dump` lists the setup card's "Open settings" buttons (`OutlineButton`), "Test alert" with "(3 sec)" (`FilledButton`) and both tabs. On Alert settings, screenshot 01 shows the duration grid (`AdaptiveGrid`) and the speed options (`OptionButton`) drawn as before. | [01](lint-cleanup/01-speed-lines.png) |

Nothing was changed on the emulator but the installed build. Afterwards it
was left on Home, with `/sdcard/ui.xml` removed.

## Not tested

- **TalkBack reading "On" and "Off" on a device:** covered by the unit test
  through the node info TalkBack gets, but not heard on the emulator or the
  phone.
- **Backup on Android 10 and 11** (ruling L4): no such device was used.
- **Everything on the test phone** (Redmi 15C 5G).
