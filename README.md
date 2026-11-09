# Beacon

Beacon makes incoming messages visible. When a text or a chat message
arrives, the whole screen blinks a colour and the camera light flashes, so
people who can't hear the notification sound still notice it.

It is designed for deaf and hard-of-hearing seniors: large bold text, WCAG AAA
contrast, big touch targets and plain wording.

## Status

- [x] **Step 0:** project rules (`CLAUDE.md`), README, CHANGELOG
- [x] **Step 1:** feasibility tests on a real phone: notification detection,
      camera light blinking, full-screen blink over other apps and the lock
      screen, alerts with the screen off and battery optimisation on, and a
      60-minute camera-light run at Fast (overheating warnings, battery
      drop). Each result is reported as tested on a named phone and Android
      version, or as not tested. Done on a Redmi 15C 5G with Android 16,
      and approved on 8 November 2026. Results:
      [`docs/feasibility.md`](docs/feasibility.md).
- [ ] **Step 2 (in progress):** Home, Alert settings, Test alert overlay and
      home-screen widget, built to match the design, plus the alert engine
      behind them. The spec is approved:
      [`docs/specs/2026-11-08-step2-design.md`](docs/specs/2026-11-08-step2-design.md).
      It is built in three PRs: screens, then the alert engine, then the
      widget.
      - [ ] **PR 0:** the spec and the plan for PR 1
        ([`docs/plans/2026-11-08-step2-pr1-screens.md`](docs/plans/2026-11-08-step2-pr1-screens.md)),
        open as yanivil/beacon#5.
      - [ ] **PR 1:** the screens and the test alert, built on
        `feature/step2-screens`, open as yanivil/beacon#6. Emulator checks:
        [`docs/checks/2026-11-step2-pr1.md`](docs/checks/2026-11-step2-pr1.md).
        Five of the six defects they found are fixed and re-checked on the
        emulator; the sixth needs TalkBack on the phone. Nothing in it has
        run on the test phone yet.
      - [ ] **PR 2:** the alert engine. Its plan is written and waiting
        for review:
        [`docs/plans/2026-11-09-step2-pr2-alert-engine.md`](docs/plans/2026-11-09-step2-pr2-alert-engine.md).
      - [ ] **PR 3:** the widget.

The app lives in [`app/`](app): one Gradle module, package `dev.beacon`,
Kotlin and Jetpack Compose. [`spikes/feasibility/`](spikes/feasibility)
holds the throwaway test app used in Step 1. It stays a separate project and
is not part of Beacon.

## What PR 1 delivers

- **Project setup:** the `app` module with exact library versions, minSdk
  29, compileSdk and targetSdk 36, no internet permission and Android backup
  off.
- **Settings:** the settings model and its defaults, saved on the phone with
  DataStore.
- **Theme:** Beacon's colours, type sizes and the bundled Atkinson
  Hyperlegible Next font, with a unit test that checks every text colour
  pair at 7:1 or more.
- **Blink clock and camera light:** one clock for the screen and the light,
  only at 30, 60 or 90 blinks a minute.
- **Home:** the Visual alerts card (ON or PAUSED), the app switches and the
  "How alerts look" summary.
- **Alert settings:** how long to flash (with Custom), blink speed and the
  screen flash colour, one for all apps or one per app.
- **Test alert:** the 3-second overlay that blinks the chosen colour, with a
  countdown and a Stop button.
- **Checks:** 120 unit tests, and emulator checks at the default and largest
  font sizes, in landscape, right-to-left and with TalkBack. Five of the six
  defects those checks found are fixed: system bar contrast, a squeezed
  icon, a right-to-left wrap and two TalkBack readings.

Not in PR 1: noticing messages, the real alert screen and the "Finish setup"
card (PR 2), and the widget (PR 3).

## What Step 1 found

On the test phone, Beacon works if the person grants three things:

- **Notification access**, to notice SMS and WhatsApp messages (heard about
  0.3 s after they arrive).
- **Display over other apps**, so the blink screen can open over other apps
  and the lock screen. Without it, only the camera light flashes.
- **Battery saver: "No restrictions"** for Beacon (HyperOS). Without it,
  HyperOS freezes Beacon while the phone sleeps, and alerts wait until the
  phone is charged.

The camera light needs no permission, and Beacon needs no SMS, call log or
phone permissions. Telegram was not tested, and calls are out of scope.
Details, sources and the owner's decisions are in
[`docs/feasibility.md`](docs/feasibility.md).

## Planned features

From the design:

- **Flash for these apps:** Texts (SMS), WhatsApp messages and Telegram
  messages, each switched ON or OFF. Beacon flashes only when a message is
  received, never for backups or other app activity. Telegram wasn't tested
  in Step 1. The design also shows Phone Calls and WhatsApp video calls;
  those are out of scope.
- **How long to flash:** 1 min, 5 min, Custom (1 to 60 minutes in fixed
  steps) or Until I stop it. The default is 1 min.
- **Blink speed:** Slow (30), Medium (60) or Fast (90) blinks a minute.
- **Screen flash colour:** Red, Yellow, Blue, Green or White, either one
  colour for all apps or a different colour for each app.
- **Test alert:** a 3-second preview of the alert.
- **Home-screen widget:** shows ON or PAUSED; one tap pauses alerts for 1 hour.

Added in the Step 2 spec:

- **Finish setup:** a card on Home, shown only while notification access,
  "Display over other apps" or the battery setting "No restrictions" is
  missing. Each shows ON or OFF and opens the right settings page.
- **Alert screen:** "New message" and the app's name over the blinking
  colour, with a big Stop button. It never shows who sent the message or
  what it says.
- **No internet:** Beacon has no internet permission, and settings never
  leave the phone.

## Tech stack

- Kotlin, Jetpack Compose (app UI), Jetpack Glance (home-screen widget)
- **minSdk 29 (Android 10)**; compileSdk and targetSdk 36

Per Google's October 2026 distribution data, minSdk 29 covers about 94% of
active Android phones, and every supported phone follows the same rules for
showing a full-screen alert from the background (restricted since
Android 10). The trade-off is that Android 8 and 9 phones (about 5%) can't
install Beacon.

## Building

You need JDK 21 and the Android SDK with platform 36 and build-tools 36.
[Android Studio](https://developer.android.com/studio) includes both, but
isn't required: the command-line tools are enough. Set `ANDROID_HOME` to the
SDK folder. Gradle comes with each project's wrapper.

From the repo root, build the app:

```bash
./gradlew :app:assembleDebug
```

Run the unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Install it on one device or emulator, named by its serial (see
`adb devices`):

```bash
ANDROID_SERIAL=<serial> ./gradlew :app:installDebug
```

Build the Step 1 test app:

```bash
cd spikes/feasibility && ./gradlew assembleDebug
```

Install it on a phone with USB debugging on:

```bash
adb install -r spikes/feasibility/build/outputs/apk/debug/beacon-feasibility-spike-debug.apk
```

How to run each test is in [`docs/feasibility.md`](docs/feasibility.md).

## Safety and accessibility rules

The full rules are in [`CLAUDE.md`](CLAUDE.md). In short:

- Every text colour pair is at least 7:1 contrast; body text at least 20sp;
  headers at least 28sp and bold; touch targets at least 64dp.
- Every switch says ON or OFF in words; no sliders, hamburger menus or
  icon-only buttons; a flat 2-tab bottom bar; usable at the largest font size.
- Every control works with TalkBack: correct roles, state read aloud and a
  spoken label for every icon.
- All text lives in `strings.xml` and layouts use start/end, so a Hebrew
  (right-to-left) version stays possible.
- Blink speed is limited to the presets 30, 60 and 90 blinks a minute, never
  more than 3 flashes a second (WCAG 2.3.1).

## Contributing

- Work on a `feature/<desc>` or `fix/<desc>` branch, never on `main`.
- GitHub access uses only the repo-scoped token in the macOS keychain
  (`github.com`, path `yanivil/beacon.git`); see the Git rules in
  [`CLAUDE.md`](CLAUDE.md).
- Every change comes with a PR draft (Title, Context/Why, Summary of Changes,
  Test Steps, Doc Impact), an updated README, and an entry under
  `[Unreleased]` in [`CHANGELOG.md`](CHANGELOG.md).

## Design

The design reference is an HTML prototype:
https://claude.ai/code/artifact/57db235d-b0b1-4b3d-af5c-bdee1677e1f2
It is titled "Visual Alerts"; the app's name is Beacon.
