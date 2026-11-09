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
      - [x] **PR 0:** the spec and the plan for PR 1
        ([`docs/plans/2026-11-08-step2-pr1-screens.md`](docs/plans/2026-11-08-step2-pr1-screens.md)),
        merged as yanivil/beacon#5 on 9 November 2026.
      - [x] **PR 1:** the screens and the test alert, merged as
        yanivil/beacon#6 on 9 November 2026. Emulator checks:
        [`docs/checks/2026-11-step2-pr1.md`](docs/checks/2026-11-step2-pr1.md).
        Five of the six defects they found are fixed and re-checked on the
        emulator; the sixth needs TalkBack on the phone. Nothing in it has
        run on the test phone yet.
      - [x] **PR 2:** the alert engine, merged as yanivil/beacon#7 on
        9 November 2026. Plan:
        [`docs/plans/2026-11-09-step2-pr2-alert-engine.md`](docs/plans/2026-11-09-step2-pr2-alert-engine.md).
        Emulator checks:
        [`docs/checks/2026-11-step2-pr2.md`](docs/checks/2026-11-step2-pr2.md).
        They found two defects, both fixed and re-checked on the emulator:
        an alert that started while the screen was off ended at once, and
        Android's one-time "Viewing full screen" panel covered the first
        alert. Nothing in it has run on the test phone yet.
      - [ ] **PR 3:** the widget and the timed pause, built on
        `feature/step2-widget` and not merged yet. Plan:
        [`docs/plans/2026-11-09-step2-pr3-widget.md`](docs/plans/2026-11-09-step2-pr3-widget.md).
        Emulator checks:
        [`docs/checks/2026-11-step2-pr3.md`](docs/checks/2026-11-step2-pr3.md).
        They found three defects, all fixed and re-checked on the
        emulator: after only a font-size change the widget stayed clipped
        until Glance drew it afresh, the launcher let it shrink to 3 × 2,
        and at 3 × 2 "PAUSED" broke and the resume time was cut off.
        Nothing in it has run on the test phone yet.
      - [ ] **App icon:** Beacon's own launcher icon, a black bolt on the
        design's yellow, built on `feature/app-icon`, open as
        yanivil/beacon#9. Emulator checks:
        [`docs/checks/2026-11-app-icon.md`](docs/checks/2026-11-app-icon.md).

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

## What PR 2 delivers

- **Noticing messages:** a notification listener that reads only a
  notification's structure (its app, key, kind and times), never a title,
  a name or message text.
- **The message rules:** Beacon flashes for a new SMS, WhatsApp or Telegram
  message, and not for ongoing notifications such as WhatsApp's backup,
  group summaries, repeated updates of the same message, the person's own
  replies, paused alerts or an app switched OFF. A message waits for the
  saved settings, so it is never judged with the defaults.
- **Real alerts:** the alert controller now runs them. A real message
  replaces a running test, and a message during an alert never starts a
  second one. It keeps the phone's processor awake with a wake lock that
  always has a timeout, stops the camera light when the phone gets too hot,
  and releases the wake lock only after the light is off.
- **The alert screen:** "New message" and the app's name over the blinking
  colour, with a big Stop button that TalkBack focuses first. It opens over
  other apps and the lock screen, also when the screen is off. The system
  bars stay visible on black strips while it flashes, so their icons stay
  readable. It never shows who sent the message or what it says.
- **Endings:** Stop, Back, Home, the power button, the timer, or opening
  Beacon (the way to end a light-only alert).
- **The "Finish setup" card:** on Home while notification access, "Display
  over other apps" or the battery exemption is missing. Each row says ON or
  OFF and has an "Open settings" button for its page.
- **Checks:** 222 unit tests, and emulator checks of the setup card, real
  SMS alerts, every ending, the false-alert cases, the largest font,
  TalkBack and the keyboard. They found two defects, both fixed and
  re-checked: an alert that started with the screen off ended at once, and
  hiding the system bars brought up Android's "Viewing full screen" panel.

Not in PR 2: the widget, the timed pause and its Home line (PR 3).

## What PR 3 delivers

- **The home-screen widget:** "Beacon" in the widget picker, 4 × 2 cells
  by default. It shows ON on yellow or PAUSED on black, in the system font,
  and the whole widget is one tap target with one spoken label. One tap
  pauses alerts for 1 hour; a tap while paused turns them ON. Each line is
  measured as the launcher will draw it. When the widget is small or the
  font is large, the tap hint goes first, then the title, then the icon,
  then the resume time, which TalkBack still reads; the state word always
  stays. After a font-size change the widget is fitted again.
- **Following the phone's settings:** the widget follows a change of the
  12- or 24-hour setting, the time zone or the language by itself.
- **The timed pause:** Home's PAUSED card says "Back ON by itself at HH:MM."
  in the phone's 12- or 24-hour format, and Home turns ON at that time by
  itself.
- **Keeping it current:** every pause change goes through one coordinator,
  which saves it, sets or cancels a resume alarm and, when the pause
  changed, redraws the widget. A change that has started finishes even if
  Beacon is closed at once. The alarm is inexact with a 10-minute window,
  so Beacon needs no exact-alarm permission. A pause that has ended is
  also cleaned up when Beacon comes to the front, when the screen turns
  on, and after a time, time-zone or language change. Alerts never wait
  for any of this: they check the clock.
- **No new permissions:** the ones Glance's background library would add
  are removed, so Beacon still asks only for "Display over other apps" and
  the wake lock, and still has no internet permission.
- **PR 2's carry-overs:** rotating no longer ends a light-only alert while
  Beacon is in front, a real alert replaces a test without a moment of
  Idle in between, and the blink works out its regions only when the size
  or the bars change.
- **Checks:** 305 unit tests, lint with no errors, and emulator checks of
  the widget picker, the tap and pause path, auto-resume with the
  emulator's clock moved forward, the widget at font 1.0 and 2.0 at three
  sizes, TalkBack and the rotation carry-over. They found three defects, all fixed and re-checked:
  the widget stayed clipped after only a font-size change, it could be
  shrunk to 3 × 2, and at 3 × 2 its PAUSED state didn't fit.

Not in PR 3: telling the alert when the camera refuses the light, and new
battery steps for phones other than Xiaomi (both left from PR 2's reviews),
and every check on the test phone.

## The launcher icon

Beacon's icon is the design's bolt in near-black `#0B0B0D` on the design's
yellow `#FFD60A`, 13.93:1: the widget's ON card, as an icon. The design has
no icon of its own; the owner chose this one from three ideas on
9 November 2026.

- **Adaptive:** `ic_launcher` and `ic_launcher_round` in
  `app/src/main/res/mipmap-anydpi/`, with vector background, foreground and
  monochrome layers. Each launcher cuts it to its own shape, and Android 13
  and later can show it as a themed icon.
- **Size:** the bolt is 44 dp tall and stays inside the 66 dp safe zone,
  which no launcher mask clips.
- **Widget picker:** the widget has no preview image of its own, so the
  picker shows this icon too.
- **Checks:** `LauncherIconTest` (8 unit tests, 313 in all) ties the
  colours to the theme tokens, the path to the design's bolt and the bolt
  to the safe zone. Lint loses its `MissingApplicationIcon` warning. On the
  emulator the icon shows on the launcher, on App info and in the widget
  picker, and as a themed icon:
  [`docs/checks/2026-11-app-icon.md`](docs/checks/2026-11-app-icon.md).
  It hasn't run on the test phone yet.

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
