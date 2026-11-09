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
  Compose, 120 unit tests). It has:
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
  right-to-left and with TalkBack. The checks found six defects. Five are
  fixed and re-checked on the emulator. For example, TalkBack read the
  Custom value's new length at every step, and read "2 of 4" in the
  two-column duration grid; the one-column grid is covered by unit tests
  only. The sixth, a switch's new state after a double tap with TalkBack,
  needs the phone. The file also lists what stays not tested until the
  phone is connected.
- `docs/plans/2026-11-09-step2-pr2-alert-engine.md`: the implementation
  plan for Step 2's second PR, the alert engine. It has 8 test-first tasks:
  - The message facts and the classifier's rules.
  - The alert screen, with the system bars hidden while a flash shows
    (replaced while building: the bars stay on black strips, D1).
  - The alert window over other apps and the lock screen.
  - The full alert controller: a real alert replaces a test, plus the wake
    lock, the heat check and opening the alert window.
  - Listening for messages.
  - Ending alerts from Beacon's main screen.
  - The "Finish setup" card.
  - Emulator checks.

  It also records 12 rulings where the spec left a choice, and the items
  PR 1's reviews left for PR 2.
- `app/`: Step 2's PR 2, the alert engine (222 unit tests). It adds:
  - The message facts and the classifier's rules: Beacon reads only a
    notification's structure, never its text, and flashes only for a new
    message from an app that is ON while alerts aren't paused. Ongoing
    notifications (WhatsApp's backup), group summaries, repeated updates
    and the person's own replies never flash.
  - The notification listener and the message watcher, which waits for
    the saved settings before deciding. Opening Beacon re-binds a listener
    the system dropped.
  - The full alert controller: a real message replaces a running test; a
    wake lock with a 10-second margin, renewed hourly for "Until I stop
    it" and released only after the light is off; the heat check, with a
    30-second poll when the system refuses the heat listener; and the
    alert window.
  - The alert window (`AlertActivity`): "New message" and the app's name
    over the blink, over other apps and the lock screen, with Stop first
    for TalkBack. It opens as a non-user action and treats Home as an
    ending only while the screen is on, so an alert that arrives with the
    screen off stays up. It closes itself whenever no real alert runs, and
    ends the alert on Stop, Back, Home or the screen turning off.
  - The flash keeps the system bars visible: the blink paints black strips
    behind them, so their white icons stay 21:1, and the rest of the
    window blinks.
  - Endings from Beacon's main screen: opening Beacon ends a real alert;
    leaving it ends a test, except for a rotation. The test overlay's Stop
    and Back end only a test, and keyboard focus moves to its Stop when it
    opens.
  - The test overlay's new camera line, "Off while the phone is hot", and
    a countdown TalkBack reads every second.
  - The "Finish setup" card on Home, with notification access, "Display
    over other apps" and the battery exemption, each ON or OFF with an
    "Open settings" button. App icons now load off the main thread.
  - The setup buttons never crash on a refused settings page: a page that
    throws (as some HyperOS pages can) falls back to App info, and if that
    fails too, nothing happens.
  - A listener reconnect forgets the "already handled" record before
    priming from what is showing, so a message re-posted after a
    disconnect can alert again.
  - The alert screen's window title no longer assumes a source, so a
    request without one cannot crash it.
- `docs/checks/2026-11-step2-pr2.md`, with screenshots in
  `docs/checks/step2-pr2/`: PR 2's emulator checks (AVD `beacon-api36`,
  Android 16) of the setup card, real SMS alerts from the emulator's own
  modem, every ending, light-only alerts, the false-alert cases, the
  largest font, TalkBack and the keyboard. They found two defects, both
  fixed and re-checked on the emulator: an alert that started while the
  screen was off ended within a third of a second (D2), and hiding the
  system bars made Android's one-time "Viewing full screen" panel cover the
  first alert and the first test (D1). The largest font was re-checked on
  the fixed build, on the alert screen and the test overlay in both
  orientations. The file also lists what stays not tested until the phone
  is connected.

### Changed

- `CLAUDE.md`:
  - Git rules: authenticate only through the gh login
    (`gh auth setup-git`); never read, use or create credentials, tokens or
    keychain entries; stop and ask if access fails. This replaced the rule
    that pointed at a repo-scoped keychain token.
  - Git rules again: GitHub access now uses only the repo-scoped keychain
    token for `yanivil/beacon.git`. git picks it up through its credential
    helper, and gh receives it for one command at a time. The token is
    never printed or saved elsewhere, and keychain entries are never
    changed. This replaces the gh-login-only rule.
  - Accessibility rules: every control works with TalkBack (correct roles,
    state read aloud, a spoken label for every icon), and all user-facing
    text lives in `strings.xml` with start/end layouts, so a right-to-left
    (Hebrew) version stays possible.
  - Plan gate, now its own section: Step 1 results are reported as "Tested
    on <phone model>, Android <version>" or "Not tested", and Step 1 adds a
    60-minute camera-light run at Fast that records overheating warnings and
    the battery drop.
  - Scope and defaults, by the owner's decision: phone calls and WhatsApp
    video calls are dropped (the design still shows them); Beacon flashes
    only when a message is received, never for backups or other app
    activity; "How long to flash" defaults to 1 minute; headings follow the
    28sp bold rule; Telegram stays, for messages only, and is built without
    a Step 1 test.
- `README.md`:
  - minSdk 29 reach updated to about 94% of active phones (Android 8 and 9
    about 5%), per Google's October 2026 data; the rules summary, Step 1
    scope and contributing notes updated to match.
  - The contributing note matches the keychain-token Git rule.
  - Step 1 marked done and waiting for approval, with a short "What Step 1
    found" section. Building no longer requires Android Studio (JDK 21 and
    the Android SDK are enough) and shows how to build and install the test
    app.
  - The owner's scope and defaults, as in `CLAUDE.md`.
  - Step 1 marked approved (8 November 2026). Step 2 now links its approved
    spec and the PR 1 plan, and lists what the spec adds: the "Finish
    setup" card, the alert screen, and no internet use.
  - Step 2 marked in progress, with PR 0 open as yanivil/beacon#5 and PR 1
    open as yanivil/beacon#6. It now describes the `app/` module, how to
    build, test and install the app, and what PR 1 delivers.
  - PR 2 built on `feature/step2-alert-engine` and open as yanivil/beacon#7, with
    links to its plan and its emulator checks and the two defects they
    found and that are fixed, and a "What PR 2 delivers" section.
- `docs/feasibility.md`:
  - The owner's scope and defaults, as in `CLAUDE.md`.
- `docs/specs/2026-11-08-step2-design.md`, while building PR 1:
  - Status: PR 1 built, with the emulator checks' defects D1 to D5 fixed.
  - Section 3: four more rule-versus-design rows: dashed borders on
    disabled buttons, the ON/OFF pill wrapping under the text, the per-app
    Change/Done button stacking under the name, and the "Visual alerts"
    heading on the PAUSED card. Row 8: options are read as a list.
  - Section 4: the library versions and why, the ripple built with
    `createRippleModifierNode`, and the JVM flag Robolectric needs on
    JDK 21.
  - Section 7: the camera light never replays missed flips and a finite
    alert ends with the light off; the Home-button and screen-off endings
    come with PR 2's alert screen.
  - Section 8: the PAUSED heading, the pill and button wrapping, dashed
    disabled buttons, the tab bar's insets (never the keyboard's), and the
    test overlay's behaviour: taps and TalkBack stay on the overlay, Back
    ends the test, and the countdown shares the background's clock.
  - Section 11: the system bars are always dark, and their icons sit on the
    flash colour during a test (decided for real alerts with PR 2's alert
    screen); list positions in grids; how the live regions reach TalkBack.
  - Section 12: "(3 sec)" takes its own text direction.
  - Section 13: what happens when settings can't be saved, that the app
    stays on the defaults when settings can't be read, the glyph used when
    an app's icon can't be drawn, and no camera service counting as no
    light.
  - Section 15: what the Robolectric tests check.
  - Section 16: PR 1 built on `feature/step2-screens`, with a link to its
    checks.
- `docs/specs/2026-11-08-step2-design.md`, while building PR 2:
  - Status: PR 2 built, with its emulator checks' defects D1 and D2 fixed.
  - Section 6: the listener waits for the saved settings, and the
    reconnect runs at once when Beacon comes to the front.
  - Section 7: a real alert replaces a test without dropping the wake lock;
    the reason for the wake lock's 10-second margin; `lightFlashing` turns
    false after the heat stop; the 30-second heat poll when the system
    refuses the heat listener; the screen-off ending lives in the alert
    window, so it doesn't end a light-only alert; a test ends when Beacon's
    main screen stops, except for a rotation; the wake lock is released
    only after the light's off has run; the alert window closes itself
    whenever no real alert runs; Home ends an alert only while the screen
    is on, and the window opens as a non-user action (D2); the blink
    leaves black strips behind the system bars (D1).
  - Section 8: the camera line's third value, "Off while the phone is hot";
    keyboard focus moves to Stop when the test overlay opens; the overlay's
    Stop and Back end only a test; the alert window's title.
  - Section 9: on Android 11 and later, the "Display over other apps"
    button opens the list of apps; the battery steps sit above the button,
    and stock Android 16 names the row "App battery usage". The merged
    manifest also carries AndroidX core's signature-level
    `dev.beacon.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which grants
    nothing to other apps.
  - Section 11: the system bars are always visible and dark, never hidden,
    with black strips behind them during a flash, replacing "decided for
    real alerts with PR 2" (hiding them brought up Android's "Viewing full
    screen" panel, D1). The countdown is a new text node every second.
  - Section 13: rows for Beacon open in front without "Display over other
    apps", and for a refused heat listener; the hot-phone and
    real-during-test rows say what the overlay shows and that its Stop
    can't end the real alert.
  - Section 16: PR 2 built on `feature/step2-alert-engine`, with links to
    its plan and checks and the two defects they found and that are fixed.
- `docs/plans/2026-11-08-step2-pr1-screens.md`:
  - The library versions: Compose BOM 2026.06.01 (Compose 1.11.4), core-ktx
    1.18.0 and lifecycle 2.10.0. The newer Compose 1.12, core 1.19 and
    lifecycle 2.11 need compileSdk 37, and the project rules fix compileSdk
    at 36.
