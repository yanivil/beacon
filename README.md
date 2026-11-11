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
      widget, followed by PR 4 for what PR 2's reviews left over.
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
      - [x] **PR 3:** the widget and the timed pause, merged as
        yanivil/beacon#8 on 9 November 2026. Plan:
        [`docs/plans/2026-11-09-step2-pr3-widget.md`](docs/plans/2026-11-09-step2-pr3-widget.md).
        Emulator checks:
        [`docs/checks/2026-11-step2-pr3.md`](docs/checks/2026-11-step2-pr3.md).
        They found three defects, all fixed and re-checked on the
        emulator: after only a font-size change the widget stayed clipped
        until Glance drew it afresh, the launcher let it shrink to 3 × 2,
        and at 3 × 2 "PAUSED" broke and the resume time was cut off.
        Nothing in it has run on the test phone yet.
      - [x] **App icon:** Beacon's own launcher icon, a black bolt on the
        design's yellow, merged as yanivil/beacon#9 on 9 November 2026.
        Emulator checks:
        [`docs/checks/2026-11-app-icon.md`](docs/checks/2026-11-app-icon.md).
      - [x] **PR 4:** PR 2's last two carry-overs, merged as
        yanivil/beacon#10 on 9 November 2026. Plan:
        [`docs/plans/2026-11-09-step2-pr4-carryovers.md`](docs/plans/2026-11-09-step2-pr4-carryovers.md).
        Emulator checks:
        [`docs/checks/2026-11-step2-pr4.md`](docs/checks/2026-11-step2-pr4.md).
        Nothing in it has run on the test phone yet.
      - [x] **Lint clean-up:** lint's ten warnings about Beacon's own code
        and resources, merged as yanivil/beacon#11 on 10 November 2026.
        Checks:
        [`docs/checks/2026-11-lint-cleanup.md`](docs/checks/2026-11-lint-cleanup.md).
      - [ ] **Dependency versions:** Gradle 9.9.1 and Kotlin 2.4.21, built
        on `fix/dependency-versions` and not merged yet. Checks:
        [`docs/checks/2026-11-dependency-versions.md`](docs/checks/2026-11-dependency-versions.md).

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

Not in PR 3: every check on the test phone.

## What PR 4 delivers

- **A refused camera light reaches the alert:** when the light refuses
  mid-alert (for example another app takes the camera), the blinker leaves
  it off and tells the alert controller. The controller marks the light
  off and stops checking the heat for it, and the screen keeps blinking.
  A late report from an alert that has already ended or been replaced
  changes nothing. During a test, the overlay's camera line then says
  "Off while another app uses the camera"; before, it kept saying
  "Flashing too".
- **Battery steps for other phones:** the "Battery: No restrictions" row
  names the labels the phone's own Android version shows on the way from
  App info to "Unrestricted". On Android 14 and later: "Tap App battery
  usage. Tap the words Allow background usage, not its switch. If the next
  page’s switch is off, turn it on. Then tap Unrestricted." The row's
  switch would make Beacon "Restricted", so the steps steer away from it,
  and they also work when background use is already off. Android 14 and 15
  share that wording and Android 10 to 13 have their own, all taken from
  Android's Settings source and not tested. An Android 13 or 14 phone that
  never got its later update may show one label that differs. Xiaomi's
  steps don't change.
- **Checks:** 324 unit tests (11 new), lint with no errors, and emulator
  checks of the battery path on Android 16: the new steps followed word
  for word from "Optimized" and from "Restricted", each ending with the
  Battery row ON, and the row at font 2.0:
  [`docs/checks/2026-11-step2-pr4.md`](docs/checks/2026-11-step2-pr4.md).
  The choice of steps is a plain function tested in plain JUnit, so the
  tests need no extra Android versions. The refusal can't be made on the
  emulator, so it has unit tests only.

Not in PR 4: every check on the test phone, and the older Android
versions' battery steps on a screen.

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

## The lint clean-up

Lint had 0 errors and 20 warnings after PR 4. Ten were about Beacon's own
code and resources, and this clean-up deals with all ten. What a person sees
and hears in English doesn't change.

- **Speed line:** "30 blinks a minute" is now a plural, so a translation
  can change its words with the number.
- **Spoken On and Off:** the setup row's own "On" and "Off" strings had the
  same names as Compose's private ones. That replaced Compose's English
  words, which TalkBack reads for every switch, with the same words, so
  nothing sounded different. But any later change to the setup row's words
  would have changed every switch too. They now have names of their own
  (`setup_state_on`, `setup_state_off`). On a phone set to another
  language, the setup row now says "On" and "Off" in English, like the
  rest of Beacon, where it used to borrow Compose's translation. The app
  switches keep Compose's translations.
- **Code order:** four shared composables take `modifier` as their first
  optional parameter, as Compose's guidelines ask.
- **Kept on purpose, with the reason beside each:**
  - The widget's target cell size, which only Android 12 and later read.
  - Backup: there are no separate rules for Android 10 and 11, because
    `allowBackup="false"` already turns backup off there.
  - `compileSdk` and `targetSdk` stay at 36, as `CLAUDE.md` says, so
    lint's two reminders that 37 exists are silenced on those two lines.
- **Left for the owner at the time:** 8 warnings about newer versions of
  Gradle, the Compose BOM and test libraries, Lifecycle, Core KTX and the
  Kotlin Compose plugin. The owner asked for them to be bumped; see the
  next section.

## The dependency versions

The owner asked for lint's 8 newer-version warnings to be dealt with on
11 November 2026. Two are bumped, and six can't be yet:

- **Gradle:** the wrapper goes from 9.8.2 to 9.9.1. Lint asked for 9.9.0,
  then found 9.9.1 once the build was on 9.9.0.
- **Kotlin:** 2.2.10 to 2.4.21, through the Compose compiler plugin. AGP's
  built-in Kotlin moves up to the same version, so the compiler and the
  plugin still match. The only library the app ships that changes is
  `kotlin-stdlib`.
- **Not bumped:** Compose 1.12 (BOM 2026.09.00 and the UI test libraries),
  Lifecycle 2.11 and Core KTX 1.19 need compileSdk 37, and `CLAUDE.md`
  fixes compileSdk at 36. The build stops on 13 of their libraries if they
  are bumped. Only compileSdk would have to change for them; targetSdk can
  stay at 36. Lint's 6 warnings about them stay until the owner decides.
- **One test line:** Kotlin 2.4.21 warned about a bare `Unit` that did
  nothing at the end of a test, so it is removed.
- **Checks:** all 327 unit tests pass, with no compiler warnings. Lint
  reports 0 errors and 6 warnings. The APK's manifest and resources match
  `main`'s. On the emulator, Home, Alert settings and the test alert run
  as before:
  [`docs/checks/2026-11-dependency-versions.md`](docs/checks/2026-11-dependency-versions.md).
  Nothing has run on the test phone.

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
- Built with Gradle 9.9.1, AGP 9.5.1 and Kotlin 2.4.21. Compose, Lifecycle
  and Core KTX stay one release behind, because their newest releases need
  compileSdk 37.

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

Check the code with lint. The report goes to `app/build/reports/`. It
should show no errors and 6 warnings: the newer Compose, Lifecycle and Core
KTX versions, which need compileSdk 37 and wait for the owner's decision:

```bash
./gradlew :app:lintDebug
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
