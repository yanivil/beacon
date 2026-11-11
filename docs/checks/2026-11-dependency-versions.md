# Dependency versions: checks

This page records the dependency-version PR on `fix/dependency-versions`:
- its rulings, each with what it costs if wrong;
- the checks on the build machine;
- one emulator run through the screens, since every line of Kotlin was
  compiled again.

Every emulator result is labelled **"Tested on emulator beacon-api36,
Android 16"** or **"Not tested"**. Nothing on this page was run on the real
phone.

## What lint reported

On `main` at `441e2be` (the lint clean-up merged), `./gradlew
:app:lintDebug` reported 0 errors and 8 warnings, all about newer versions.
The owner asked for them to be bumped on 11 November 2026.

| Warning | What | In use | Newer |
|---|---|---|---|
| `AndroidGradlePluginVersion` | Gradle (the wrapper) | 9.8.2 | 9.9.0 |
| `NewerVersionAvailable` | The Kotlin Compose compiler plugin | 2.2.10 | 2.4.21 |
| `GradleDependency` | Compose BOM | 2026.06.01 | 2026.09.00 |
| `GradleDependency` × 2 | Compose `ui-test-junit4` and `ui-test-manifest` | 1.11.4 | 1.12.1 |
| `GradleDependency` × 2 | Lifecycle `runtime-compose` and `viewmodel-compose` | 2.10.0 | 2.11.0 |
| `GradleDependency` | Core KTX | 1.18.0 | 1.19.1 |

## Rulings

- **V1. Gradle goes from 9.8.2 to 9.9.1.**
  - Lint asked for 9.9.0. Once the build ran on 9.9.0, lint found the patch
    release 9.9.1, so the wrapper took that.
  - It was updated with `./gradlew wrapper --gradle-version 9.9.1`, run
    twice so that the second run, already on 9.9.1, also writes its own
    wrapper jar. The jar's SHA-256 matches the one Gradle publishes.
  - The Step 1 test app in `spikes/feasibility/` keeps its own wrapper at
    9.8.2. It is a separate, throwaway project kept as Step 1's record, and
    lint doesn't check it.
  - Cost if wrong: the build machine needs Gradle 9.9.1, which the wrapper
    downloads by itself. Nothing in the app changes.
- **V2. Kotlin goes from 2.2.10 to 2.4.21.**
  - The catalog's `kotlin` version is the Compose compiler plugin's. AGP
    9.5.1's built-in Kotlin is 2.2.10. With the plugin at 2.4.21, Gradle
    takes the higher version for both, so the compiler and the plugin
    still match: `./gradlew buildEnvironment` shows
    `kotlin-gradle-plugin:2.2.10 -> 2.4.21`.
  - The app's `kotlin-stdlib` goes from 2.2.20 to 2.4.21. No other library
    the app ships changes.
  - A comment in the catalog now says all this.
  - Cost if wrong: Beacon's code is compiled by a newer compiler. Every
    test passes with no compiler warnings, and the screens run on the
    emulator, but nothing has run on the phone.
- **V3. Compose, Lifecycle and Core KTX stay where they are.**
  - Compose 1.12 (BOM 2026.09.00 and the UI test libraries 1.12.1),
    Lifecycle 2.11.0 and Core KTX 1.19.1 need compileSdk 37: their AAR
    metadata says `minCompileSdk=37`. `CLAUDE.md` fixes compileSdk at 36,
    and the rules win. The spec said this in PR 1; this PR checked it
    again.
  - With the four bumps, `./gradlew :app:checkDebugAarMetadata` fails and
    names 13 libraries:
    - Compose 1.12.1: `animation`, `animation-core`, `foundation`,
      `foundation-layout`, `material-ripple`, `runtime-saveable`, `ui`,
      `ui-graphics` and `ui-text`;
    - Core 1.19.1: `core` and `core-ktx`;
    - Lifecycle 2.11.0: `lifecycle-runtime-compose` and
      `lifecycle-viewmodel-compose`.
  - Only compileSdk needs to change for them. targetSdk can stay at 36,
    since compileSdk only decides which APIs the code may use, while
    targetSdk decides how Android treats the app.
  - Lint's 6 warnings about them are not silenced. They stay as the
    owner's reminder until the owner decides about compileSdk 37. A comment
    in the catalog says why.
  - Cost if wrong: Beacon misses those libraries' newer fixes until then.

### Where the rules and lint disagree

- Lint asks for Compose 1.12, Lifecycle 2.11 and Core 1.19, which need
  compileSdk 37. `CLAUDE.md` says compileSdk 36.
- The rules win (ruling V3).
- The design isn't involved.

## Build machine

| Check | Result |
|---|---|
| `./gradlew clean :app:testDebugUnitTest` | Passes: 327 tests, 0 failures, the same tests as `main`. A version bump adds no behaviour of its own to test, so the whole suite is the test. |
| `./gradlew :app:lintDebug` | Passes: 0 errors, 6 warnings, all of them ruling V3's. |
| Warnings | A clean build with `--warning-mode all` shows no Kotlin compiler warnings and no Gradle deprecation warnings. |
| Kotlin version | `./gradlew buildEnvironment`: `compose-compiler-gradle-plugin:2.4.21`, and `kotlin-gradle-plugin:2.2.10 -> 2.4.21`. |
| Libraries the app ships | `./gradlew :app:dependencies --configuration debugRuntimeClasspath` before and after: the only difference is `kotlin-stdlib`, 2.2.20 before and 2.4.21 after. |
| Wrapper jar | SHA-256 `621008af…3cdaf8`, the same as Gradle's published `gradle-9.9.1-wrapper.jar.sha256`. |
| Compiled APK | Against a debug APK built from `main` at `441e2be`: `aapt2 dump xmltree` of `AndroidManifest.xml` and `aapt2 dump resources` match exactly. Only the `.dex` files and Kotlin's own `kotlin_builtins` files differ. The debug APK is 30,156,450 bytes, 229,748 more than `main`'s (0.8%). |
| The other four bumps | With Compose BOM 2026.09.00, the Compose UI test libraries 1.12.1, Lifecycle 2.11.0 and Core KTX 1.19.1, `./gradlew :app:checkDebugAarMetadata` fails: 13 libraries need compileSdk 37 (ruling V3). The catalog was then put back. |

## Emulator

| | |
|---|---|
| Emulator | AVD `beacon-api36`, Pixel 7 profile (1080 × 2400 px, 420 dpi), run as `emulator-5554` |
| System image | Android 16 (API 36), build `BE2A.250530.026.F3` |
| Build | Debug APK of `fix/dependency-versions`, installed over the lint clean-up's build with `adb install -r`; app data was kept |
| Start state | Font size 1.0. Notification access and "Display over other apps" off, as earlier checks left them, so Home shows the "Finish setup" card. Medium speed chosen. |
| Date | 11 November 2026 |

| Check | Result | Screenshot |
|---|---|---|
| Beacon opens on Home | Tested on emulator beacon-api36, Android 16. `am start` reports `Status: ok`. `uiautomator dump` lists "Finish setup" with the notification-access and "Display over other apps" rows, each with "Open settings", then "Test alert" with "(3 sec)" and both tabs. Both rows show OFF. | |
| Alert settings | Tested on emulator beacon-api36, Android 16. The duration options read "1 min", "5 min", "Custom" with "10 min", and "Until I stop it". The speed lines read "30 blinks a minute", "60 blinks a minute" and "90 blinks a minute", with Medium chosen. | |
| The test alert | Tested on emulator beacon-api36, Android 16. Tapping "Test alert" opens the yellow overlay, counting down "2 seconds left", with Color "Yellow", Speed "Medium, 60 a minute", Camera light "Flashing too", Real alerts last "1 minute", and Stop. Five seconds after the tap, Alert settings is back. The crash log has nothing from `dev.beacon`. | [01](dependency-versions/01-test-alert.png) |

Nothing was changed on the emulator but the installed build. Afterwards it
was left on Home, with `/sdcard/ui.xml` removed.

## Not tested

- **The widget on the emulator:** none is placed on the emulator's home
  screen. Its unit tests pass through Glance's test library.
- **A real message alert on the emulator:** notification access is off.
  The alert engine's unit tests pass.
- **Everything on the test phone** (Redmi 15C 5G), and Android 10 to 15.
