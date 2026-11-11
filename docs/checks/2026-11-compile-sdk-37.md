# compileSdk 37: checks

This page records the compileSdk 37 PR on `fix/compile-sdk-37`:
- its rulings, each with what it costs if wrong;
- the checks on the build machine;
- an emulator comparison of the new build with `main`'s, screen by screen.

Every emulator result is labelled **"Tested on emulator beacon-api36,
Android 16"** or **"Not tested"**. Nothing on this page was run on the real
phone.

## Why

The dependency-version PR (yanivil/beacon#12) bumped Gradle and Kotlin, but
held Compose 1.12, Lifecycle 2.11 and Core KTX 1.19, because they need
compileSdk 37 and `CLAUDE.md` fixed compileSdk at 36. On 11 November 2026
the owner raised compileSdk to 37 in `CLAUDE.md`, so they could be bumped.
targetSdk stays at 36.

compileSdk only decides which Android APIs the code may be compiled
against. targetSdk decides how Android treats the app at run time, so with
it at 36, Android 10 to 16 run Beacon as before.

## Rulings

- **C1. compileSdk is 37, from SDK platform `android-37.0`.**
  - The SDK also offers 37.1 and 37.2, minor releases with a few more APIs.
    `CLAUDE.md` says 37, the libraries need 37, and Beacon uses nothing
    newer, so plain 37 is the smallest change. Lint doesn't ask for more.
  - Cost if wrong: Beacon can't use APIs added in 37.1 or 37.2 until
    compileSdk is raised again.
- **C2. targetSdk stays at 36.**
  - `CLAUDE.md` keeps it. Lint's reminder that targetSdk 37 exists stays
    silenced with `//noinspection OldTargetApi` on that line only.
  - The `//noinspection GradleDependency` on the compileSdk line is gone:
    at 37, lint has nothing to remind about there.
  - A comment in `app/build.gradle.kts` says why the two levels differ.
  - Cost if wrong: none at run time. Android treats Beacon as before.
- **C3. Compose, Lifecycle and Core KTX go to the releases lint named.**
  - Compose BOM 2026.06.01 to 2026.09.00, which takes Compose 1.11.4 to
    1.12.1. The Compose UI test libraries 1.11.4 to 1.12.1. Lifecycle
    2.10.0 to 2.11.0, and Core KTX 1.18.0 to 1.19.1.
  - Afterwards lint finds no newer version of anything.
  - The catalog's comment now says why these four need compileSdk 37, and
    that a later bump asking for a higher compileSdk needs the owner again.
  - Cost if wrong: a behaviour change in a library that no test or screen
    comparison caught. The tests and the comparison below found none.
- **C4. Beacon's code doesn't change.**
  - It compiles against the new libraries with no errors, no warnings and
    no deprecations.
  - `BeaconTheme` builds its ripple with `createRippleModifierNode`, because
    material-ripple has no public `ripple()`. `javap` shows 1.12.1 is the
    same: no `ripple()`, and `createRippleModifierNode` with the same
    parameters. Only the comment changes, to say 1.12 has none either.
  - Cost if wrong: none at run time.

### Where the rules and lint disagree

- Nowhere now. Lint's one remaining reminder is that targetSdk 37 exists,
  and `CLAUDE.md` keeps targetSdk at 36, so it stays silenced on that line
  (ruling C2).
- The design isn't involved.

## Build machine

| Check | Result |
|---|---|
| SDK platform | `platforms;android-37.0` revision 2 (Android 17, API 37.0, 152 MB), installed with `sdkmanager` from Google's SDK repository. |
| `./gradlew :app:checkDebugAarMetadata` | Passes with the four bumps, where it failed on 13 libraries at compileSdk 36. |
| `./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --rerun-tasks --warning-mode all` | Passes. 327 tests, 0 failures, the same tests as `main`. No Kotlin compiler warnings (`Problem found` lines) and no Gradle deprecation warnings. |
| `./gradlew :app:lintDebug` | "No issues found". It was 0 errors and 6 warnings on `main`. |
| Libraries the app ships | `./gradlew :app:dependencies --configuration debugRuntimeClasspath`, `main` against this branch. Every Compose module goes from 1.11.4 to 1.12.1, every Lifecycle module from 2.10.0 to 2.11.0, and Core from 1.18.0 to 1.19.1. Seven libraries leave: `androidx.transition` 1.6.0, which only Compose UI 1.11 needed, and what it brought in: `dynamicanimation`, `legacy-support-core-utils`, `documentfile`, `loader`, `localbroadcastmanager` and `print`, all 1.0.0. None is added. |
| Compiled manifest | `aapt2 dump xmltree` against a debug APK built from `main` at `29b66c8`. The only differences are `compileSdkVersion` (36 to 37), `compileSdkVersionCodename` (16 to 17), and `platformBuildVersionCode` and `platformBuildVersionName` to match. `aapt2 dump badging`: minSdk 29 and targetSdk 36, as before. |
| Compiled resources | `aapt2 dump resources` with resource ids masked: the only difference is 13 `id` resources from `androidx.transition` that are gone, such as `ghost_view` and `transition_layout_save` (734 ids before, 721 after). Every string, colour, dimension and layout is the same. |
| APK files | The 7 `META-INF/*.version` files of the libraries that left are gone; nothing is added. The debug APK is 30,531,250 bytes, 374,800 more than `main`'s (1.2%). |

## Emulator

| | |
|---|---|
| Emulator | AVD `beacon-api36`, Pixel 7 profile (1080 × 2400 px, 420 dpi), run as `emulator-5554` |
| System image | Android 16 (API 36), build `BE2A.250530.026.F3` |
| Builds | Debug APKs of `main` at `29b66c8` and of `fix/compile-sdk-37`, each installed with `adb install -r` in turn; app data was kept |
| Start state | Font size 1.0, dark mode off. Notification access and "Display over other apps" off, so Home shows the "Finish setup" card. Medium speed, Custom length 10 minutes, Yellow chosen. |
| Date | 11 November 2026 |

**How the screens were compared.** A script installs one build, opens
Beacon, and takes the same screenshots with the same taps and swipes. A
second script compares each pair of screenshots pixel by pixel, below the
status bar, whose clock changes. Swipes injected with `adb` don't always
stop in the same place. So a screen whose position a swipe decides is
compared again after shifting it to line up, and two runs of the new build
are compared with each other to show how much the swipes alone vary.

| Check | Result | Screenshot |
|---|---|---|
| Font size 1.0, screens in a fixed position | Tested on emulator beacon-api36, Android 16. The top of Home, the top of Alert settings, the end of Alert settings (colours) and Home again: pixel-identical to `main`'s build. | |
| Font size 1.0, screens a swipe leaves part-way | Tested on emulator beacon-api36, Android 16. Five screens stopped up to 280 px apart on the two builds. Lined up, the pixels match within 3 of 255 in any colour channel, which no eye can see. Two runs of the new build against each other show the same: up to 121 px apart, and within 3 of 255 once lined up. So the differences come from the swipes, not the libraries. | |
| Font size 2.0 | Tested on emulator beacon-api36, Android 16. Font size set to 2.0, then back to 1.0 afterwards. The top of Home, the end of Home, the top of Alert settings and the end of Alert settings: pixel-identical to `main`'s build. (In the first run, `main`'s swipes stopped short of the end of Home; with 30 swipes on each build the end matches.) | [02](compile-sdk-37/02-settings-end-font-2.png) |
| The test alert | Tested on emulator beacon-api36, Android 16. Tapping "Test alert" on Home opens the yellow overlay, counting down "2 seconds left", with Color "Yellow", Speed "Medium, 60 a minute", Camera light "Flashing too", Real alerts last "1 minute", and Stop. Five seconds after the tap, Home is back. The crash log has nothing from `dev.beacon`. | [01](compile-sdk-37/01-test-alert.png) |

The new build is left installed. The only setting changed was the font
size, which is back at 1.0. The emulator was left on Home, with
`/sdcard/ui.xml` removed.

## Not tested

The phone checks on 11 November 2026 ran many of these on the test phone:
see [`2026-11-phone-checks.md`](2026-11-phone-checks.md).

- **TalkBack on a device.** The unit tests read the node info TalkBack
  gets, and they pass on the new libraries.
- **The widget on the emulator.** None is placed. Glance 1.2.0 itself
  doesn't change, but it now runs on Compose's 1.12.1 runtime; its unit
  tests pass.
- **A real message alert on the emulator.** Notification access is off.
  The alert engine's unit tests pass.
- **Everything on the test phone** (Redmi 15C 5G), and Android 10 to 15.
