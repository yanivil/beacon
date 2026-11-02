# Beacon

Beacon makes incoming calls and messages visible. When a phone call, a text or
a chat message arrives, the whole screen blinks a colour and the camera light
flashes, so people who can't hear the ringtone still notice it.

It is designed for deaf and hard-of-hearing seniors: large bold text, WCAG AAA
contrast, big touch targets and plain wording.

## Status

- [x] **Step 0:** project rules (`CLAUDE.md`), README, CHANGELOG
- [ ] **Step 1:** feasibility tests on a real phone: notification detection,
      camera light blinking, full-screen blink over other apps and the lock
      screen, alerts with the screen off and battery optimisation on, and a
      60-minute camera-light run at Fast (overheating warnings, battery
      drop). Each result is reported as tested on a named phone and Android
      version, or as not tested.
- [ ] **Step 2:** Home, Alert settings, Test alert overlay and home-screen
      widget, built to match the design

There is no app code yet.

## Planned features

From the design:

- **Flash for these apps:** Phone Calls, Texts (SMS), WhatsApp messages,
  WhatsApp video calls and Telegram, each switched ON or OFF.
- **How long to flash:** 1 min, 5 min, Custom (1 to 60 minutes in fixed
  steps) or Until I stop it.
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

You need [Android Studio](https://developer.android.com/studio) (latest
stable). It includes its own JDK and Android SDK. Build and run steps will be
added with the first code in Step 1.

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
- Git access goes only through the gh login (`gh auth setup-git`).
- Every change comes with a PR draft (Title, Context/Why, Summary of Changes,
  Test Steps, Doc Impact), an updated README, and an entry under
  `[Unreleased]` in [`CHANGELOG.md`](CHANGELOG.md).

## Design

The design reference is an HTML prototype:
https://claude.ai/code/artifact/57db235d-b0b1-4b3d-af5c-bdee1677e1f2
It is titled "Visual Alerts"; the app's name is Beacon.
