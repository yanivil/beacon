# Changelog

All notable changes to Beacon are recorded here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `CLAUDE.md` with the rules every session follows: WCAG AAA accessibility,
  blink-safety limits (30/60/90 blinks a minute, never over 3 flashes a
  second), the branch and PR workflow, code-comment and docs requirements,
  and the response order.
- `README.md` describing Beacon, its planned features, the stack
  (Kotlin, Compose, Glance; minSdk 29) and the project status.
- `CHANGELOG.md` (this file).
- `docs/feasibility.md`: Step 1 results on a Redmi 15C 5G (HyperOS,
  Android 16). SMS and WhatsApp detection, the camera light at 30/60/90
  including a 60-minute run at Fast, the full-screen blink over other apps
  and the lock screen, and alerts with the screen off on battery. Also the
  permissions each part needs, Android and Google Play limits with sources,
  HyperOS's app freezer and the "No restrictions" fix, requirements for
  Step 2, and what was not tested.
- `spikes/feasibility/`: the throwaway Step 1 test app (package
  `dev.beacon.spike`, Views, no libraries) and the adb scripts that drive
  each test. It is not part of Beacon.
- `.gitignore` for Gradle, Android build output and local files.
- `docs/specs/2026-11-08-step2-design.md`: the Step 2 design spec. It
  covers:
  - Where the project rules and the design disagree, and how each conflict
    is resolved.
  - The architecture: one module, package `dev.beacon`, Compose, DataStore
    and Glance.
  - Settings and their defaults, and pause and resume.
  - The message rules, which skip ongoing, summary, repeated and own
    messages.
  - The alert engine, the screens, the "Finish setup" card and the widget.
  - Every text colour pair checked at 7:1 or more, plus type, touch targets,
    TalkBack roles and right-to-left support.
  - Error handling, privacy (no internet), testing and the PR order.
- `docs/plans/2026-11-08-step2-pr1-screens.md`: the implementation plan for
  Step 2's first PR. It has 12 test-first tasks:
  - The project setup, with exact library versions.
  - Settings and their storage.
  - Theme, contrast and the bundled font.
  - The blink clock and the camera light.
  - The alert controller for test alerts.
  - Shared components.
  - The app shell and its two tabs.
  - The Home and Alert settings screens.
  - The 3-second test alert.
  - Emulator checks at the default and largest font sizes, with TalkBack.
- `app/`: the Beacon app, Step 2's PR 1 (package `dev.beacon`, Kotlin and
  Compose, 114 unit tests). It has:
  - The project setup: one module, exact library versions, minSdk 29,
    compileSdk and targetSdk 36, no internet permission, backup off.
  - Settings and their storage: the settings model and defaults, saved with
    DataStore; unknown or missing values fall back to the defaults.
  - The theme: Beacon's colours and type, the bundled Atkinson Hyperlegible
    Next font, and a unit test that checks every text colour pair at 7:1 or
    more.
  - The blink clock and the camera-light blinker, which only take the 30,
    60 or 90 presets and never replay missed flashes.
  - The alert controller's test path: it runs one alert at a time, owns its
    timing and ends it on time, so a rebuilt screen can't restart or
    lengthen it.
  - The shared components: buttons, headings, app switch rows, ON/OFF
    pills, option buttons and swatch grids, with platform roles for
    TalkBack.
  - The app shell: the two tabs and the Test alert button, which moves into
    the scrolling content when the fixed area would take more than a third
    of the window.
  - Home: the Visual alerts card (ON or PAUSED), the app switches and the
    "How alerts look" summary.
  - Alert settings: how long to flash (with Custom), blink speed and the
    screen flash colour, one for all apps or one per app.
  - The 3-second test alert overlay, with its countdown and Stop button.
- `docs/checks/2026-11-step2-pr1.md`, with screenshots in
  `docs/checks/step2-pr1/`: PR 1's emulator checks (AVD `beacon-api36`,
  Android 16) at the default and largest font sizes, in landscape,
  right-to-left and with TalkBack. It lists six defects found, how five of
  them were fixed and re-checked, and what stays not tested until the phone
  is connected.

### Changed

- `CLAUDE.md` Git rules: authenticate only through the gh login
  (`gh auth setup-git`); never read, use or create credentials, tokens or
  keychain entries; stop and ask if access fails. This replaces the rule
  that pointed at a repo-scoped keychain token.
- `CLAUDE.md` Accessibility rules: every control works with TalkBack
  (correct roles, state read aloud, a spoken label for every icon), and all
  user-facing text lives in `strings.xml` with start/end layouts, so a
  right-to-left (Hebrew) version stays possible.
- `CLAUDE.md` plan gate, now its own section: Step 1 results are reported as
  "Tested on <phone model>, Android <version>" or "Not tested", and Step 1
  adds a 60-minute camera-light run at Fast that records overheating
  warnings and the battery drop.
- `README.md`: minSdk 29 reach updated to about 94% of active phones
  (Android 8 and 9 about 5%), per Google's October 2026 data; the rules
  summary, Step 1 scope and contributing notes updated to match.
- `CLAUDE.md` Git rules: GitHub access now uses only the repo-scoped
  keychain token for `yanivil/beacon.git`. git picks it up through its
  credential helper, and gh receives it for one command at a time. The token
  is never printed or saved elsewhere, and keychain entries are never
  changed. This replaces the gh-login-only rule. `README.md` contributing
  note updated to match.
- `README.md`: Step 1 marked done and waiting for approval, with a short
  "What Step 1 found" section. Building no longer requires Android Studio
  (JDK 21 and the Android SDK are enough) and shows how to build and install
  the test app.
- Scope and defaults, by the owner's decision: phone calls and WhatsApp
  video calls are dropped (the design still shows them); Beacon flashes only
  when a message is received, never for backups or other app activity;
  "How long to flash" defaults to 1 minute; headings follow the 28sp bold
  rule; Telegram stays, for messages only, and is built without a Step 1
  test. Recorded in `README.md`, `CLAUDE.md` and `docs/feasibility.md`.
- `README.md`: Step 1 marked approved (8 November 2026). Step 2 now links
  its approved spec and the PR 1 plan, and lists what the spec adds: the
  "Finish setup" card, the alert screen, and no internet use.
- The PR 1 plan's library versions: Compose BOM 2026.06.01 (Compose
  1.11.4), core-ktx 1.18.0 and lifecycle 2.10.0. The newer Compose 1.12,
  core 1.19 and lifecycle 2.11 need compileSdk 37, and the project rules fix
  compileSdk at 36.
- `docs/specs/2026-11-08-step2-design.md`, after building PR 1:
  - Status: PR 1 built.
  - Section 3: four more rule-versus-design rows: dashed borders on
    disabled buttons, the ON/OFF pill wrapping under the text, the per-app
    Change/Done button stacking under the name, and the "Visual alerts"
    heading on the PAUSED card.
  - Section 4: the library versions and why, the ripple built with
    `createRippleModifierNode`, and the JVM flag Robolectric needs on
    JDK 21.
  - Section 7: the camera light never replays missed flips and a finite
    alert ends with the light off; the Home-button and screen-off endings
    come with PR 2's alert screen.
  - Section 8: the PAUSED heading, the pill and button wrapping, dashed
    disabled buttons, and the test overlay's behaviour: taps and TalkBack
    stay on the overlay, Back ends the test, and the countdown shares the
    background's clock.
  - Section 13: what happens when settings can't be saved, that the app
    stays on the defaults when settings can't be read, and the glyph used
    when an app's icon can't be drawn.
  - Section 16: PR 1 built on `feature/step2-screens`, with a link to its
    checks.
- `README.md`: Step 2 marked in progress, with PR 0 open as
  yanivil/beacon#5 and PR 1 built. It now describes the `app/` module, how
  to build, test and install the app, and what PR 1 delivers.
- `docs/checks/step2-pr1/`: every screenshot scaled down to at most 1200 px
  on its longer side (about 10 MB down to under 9 MB with the new ones),
  same file names.
- `docs/checks/2026-11-step2-pr1.md`: a "Fixes and re-checks" section with
  each defect's status, fix, re-check and new `fix-*.png` screenshots, why
  the Custom value was silent, the re-check method, and how the emulator's
  adb connection was lost and repaired during the re-checks.
- `docs/specs/2026-11-08-step2-design.md`, after the fixes: status line;
  section 3 row 8 (options read as a list); section 11 (system bars always
  dark, list positions in grids, how the live regions reach TalkBack);
  section 12 ("(3 sec)" takes its own text direction); section 15 (what
  the Robolectric tests check).
- `README.md`: 114 unit tests, and the PR 1 line notes the fixed defects.

### Fixed

- System bars on a phone in its light theme: the status bar icons were dark
  on Beacon's dark page (about 1:1), and 3-button navigation showed white
  buttons on a light grey band (about 1.2:1). The bars are now always dark,
  with light icons and no contrast scrim (D1).
- The "Change settings" chevron shrank to about 4 dp at the largest font.
  A button's trailing icon now keeps its 28 dp, and the label wraps
  instead (D2).
- "(3 sec)" on the Test alert button broke onto two lines in a
  right-to-left layout. It now takes its direction from its own text and
  stays on one line (D3).
- TalkBack didn't read the Custom length value when Shorter or Longer
  changed it. The value is now a live region whose text is replaced at each
  step, which TalkBack always hears (D4).
- TalkBack read every option of the duration and swatch grids as "1 of 4"
  or "1 of 5". The grids now state their list size and each option's row,
  so TalkBack reads "2 of 4" and so on, in one column or two (D5).
