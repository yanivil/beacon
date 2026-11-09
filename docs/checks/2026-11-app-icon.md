# App icon: checks

This page records the checks of Beacon's launcher icon: the unit tests and
lint on the build machine, and the emulator checks in four places (the
launcher, the app's info screen, the widget picker and themed icons).

Every emulator result is labelled **"Tested on emulator beacon-api36,
Android 16"** or **"Not tested"**. Nothing on this page was run on the real
phone.

## Test device

| | |
|---|---|
| Emulator | AVD `beacon-api36`, run as `emulator-5554` (only this emulator; never the owner's phone) |
| Builds | The debug APK of `feature/app-icon`, for the launcher, App info and themed icons. For the widget picker, a throwaway debug APK of PR 3's `feature/step2-widget` at `29b658a` with this branch's icon files and manifest change copied on top, because `main` has no widget yet. The throwaway build is never committed. |
| Date | 9 November 2026 |

## Build machine

| Check | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | Passes: 230 tests, 0 failures (222 on `main` plus the 8 of `LauncherIconTest`). |
| `./gradlew :app:lintDebug` | Passes: 19 warnings, against 20 on `main`. `MissingApplicationIcon` is gone; no new warning. |
| `LauncherIconTest` can fail | Scaling the bolt to 3 (farthest corner 33.15 dp from the centre, past the 33 dp safe-zone radius) failed `boltStaysInsideTheSafeZone` and `monochromeIsTheSameBolt`. The file was then restored. |

## Emulator

`emulator-5554` was in use for PR 3's checks, and the owner asked to wait,
so nothing was installed on it yet.

| # | Check | Result | Label |
|---|---|---|---|
| 1 | The icon on the launcher's home screen and in the app list | Waiting for the emulator. | Not tested |
| 2 | The icon on Beacon's App info screen | Waiting for the emulator. | Not tested |
| 3 | The icon on Beacon's widget-picker entry | Waiting for the emulator. | Not tested |
| 4 | The icon with themed icons on (Wallpaper and style) | Waiting for the emulator. | Not tested |

## Not tested

- All four emulator checks above, until `emulator-5554` is free.
- Any launcher other than the emulator's Pixel Launcher, including
  HyperOS's on the Redmi 15C 5G.
- Android 10 to 12L. They ignore the monochrome layer (it needs Android
  13), so they show only the full-colour icon.
