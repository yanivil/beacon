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
| Emulator | AVD `beacon-api36`, Pixel 7 profile (1080 × 2400 px, 420 dpi), `sdk_gphone64_arm64`, run as `emulator-5554` (the same AVD as PRs 2 and 3) |
| System image | Android 16 (API 36), build `BE2A.250530.026.F3` |
| Launcher | Pixel Launcher (`com.google.android.apps.nexuslauncher`) 16, with Wallpaper and style (`com.google.android.apps.wallpaper`) 16 |
| Build | Debug APK of `feature/app-icon` after merging `main` at `25d65c2` (PR 3, the widget), installed with `adb -s emulator-5554 install -r`. All 313 unit tests pass on it. It replaced PR 3's debug build; app data was kept. |
| Screenshots | Captured at 1080 × 2400 px, then scaled down to at most 1200 px on the longer side (`sips -Z 1200`, PR 1's ruling 19), in [`app-icon/`](app-icon/) |
| Start state | Font size 1.0, light system theme, gesture navigation, Themed icons OFF, no Beacon widget placed |
| Date | 9 November 2026 |

## Build machine

| Check | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | Passes: 313 tests, 0 failures (305 on `main` plus the 8 of `LauncherIconTest`). |
| `./gradlew :app:lintDebug` | Passes: 0 errors, 20 warnings. `main` at `25d65c2` has 0 errors and 21 warnings; the one gone is `MissingApplicationIcon`, and there is no new warning. |
| `LauncherIconTest` can fail | Scaling the bolt to 3 (farthest corner 33.15 dp from the centre, past the 33 dp safe-zone radius) failed `boltStaysInsideTheSafeZone` and `monochromeIsTheSameBolt`. The file was then restored. |

## Emulator

| # | Check | Result | Label |
|---|---|---|---|
| 1 | The icon on the launcher | The app list shows "Beacon" with a yellow circle and the black bolt, centred, with a clear margin to the circle's edge. The home screen's dock shows it too, as Pixel Launcher's "Predicted app: Beacon". Screenshots 01 and 02. | Tested on emulator beacon-api36, Android 16 |
| 2 | The icon on Beacon's App info screen | The same icon above "Beacon". Screenshot 03. | Tested on emulator beacon-api36, Android 16 |
| 3 | The icon on the widget picker | The list shows "Beacon, 1 widget" with the icon. Opened, the entry's preview box shows the icon too, because the widget sets no preview image, above "Beacon", "4 × 2" and the description. The nearby "Battery" entry also has a bolt, but in blue on white, so the two are told apart by colour and shape. Screenshots 04 and 05. | Tested on emulator beacon-api36, Android 16 |
| 4 | Themed icons on | With Themed icons ON in Wallpaper and style, the home screen shows Beacon's bolt in the theme's dark blue on pale lavender, the same size and weight as the other themed glyphs (Phone, Messages, Chrome). This Pixel Launcher themes only the home screen: the app list keeps full-colour icons for every app, Beacon included. Screenshots 06 and 07. Themed icons were then switched OFF again, and the home screen showed full-colour icons. | Tested on emulator beacon-api36, Android 16 |

The pale ring around Beacon's dock icon in screenshots 01 and 06 is Pixel
Launcher's mark for a predicted app, not part of Beacon's icon.

## Screenshots

| # | File | What it shows |
|---|---|---|
| 01 | [`01-home-predicted-app.png`](app-icon/01-home-predicted-app.png) | Home screen, Beacon in the dock as a predicted app |
| 02 | [`02-app-list.png`](app-icon/02-app-list.png) | The app list, Beacon first |
| 03 | [`03-app-info.png`](app-icon/03-app-info.png) | Beacon's App info screen |
| 04 | [`04-widget-picker-list.png`](app-icon/04-widget-picker-list.png) | The widget picker's list |
| 05 | [`05-widget-picker-entry.png`](app-icon/05-widget-picker-entry.png) | Beacon's picker entry, opened |
| 06 | [`06-themed-home.png`](app-icon/06-themed-home.png) | Home screen with Themed icons ON |
| 07 | [`07-themed-app-list.png`](app-icon/07-themed-app-list.png) | The app list with Themed icons ON (full colour) |

## Emulator state afterwards

- Themed icons: back to OFF, as before the checks.
- Wallpaper, font size (1.0) and theme: not changed.
- No widget was placed.
- `dev.beacon` on the emulator is now this branch's debug build (PR 3's
  code plus the icon), not PR 3's own build.

## Not tested

The phone checks on 11 November 2026 ran many of these on the test phone:
see [`2026-11-phone-checks.md`](2026-11-phone-checks.md).

- Any launcher other than the emulator's Pixel Launcher, including
  HyperOS's on the Redmi 15C 5G.
- Themed icons on HyperOS.
- Android 10 to 12L. They ignore the monochrome layer (it needs Android
  13), so they show only the full-colour icon.
- The icon at the largest font size and in the dark system theme. Neither
  changes the icon itself, only the label under it.
