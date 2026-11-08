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
      version, or as not tested. Done on a Redmi 15C 5G with Android 16;
      waiting for approval. Results: [`docs/feasibility.md`](docs/feasibility.md).
- [ ] **Step 2:** Home, Alert settings, Test alert overlay and home-screen
      widget, built to match the design

There is no app code yet. [`spikes/feasibility/`](spikes/feasibility) holds
the throwaway test app used in Step 1. It is not part of Beacon.

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
