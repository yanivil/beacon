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
- `docs/plans/2026-11-09-step2-pr3-widget.md`: the implementation plan for
  Step 2's third PR, the widget and the timed pause. It has 6 test-first
  tasks:
  - The timed pause rules and an atomic pause update.
  - The widget's look, which lines fit, and the resume time in the phone's
    12- or 24-hour format.
  - The widget itself, the resume alarm, and one coordinator for every
    pause change.
  - Home's "Back ON by itself at HH:MM" line, and catching up when Beacon
    opens and when the screen turns on.
  - The items PR 2's reviews left for PR 3.
  - Emulator checks.

  It also records 11 rulings, including removing the permissions Glance's
  WorkManager would add (the app keeps only `SYSTEM_ALERT_WINDOW` and
  `WAKE_LOCK`, plus the AndroidX signature permission).
- `app/`: Step 2's PR 3, the widget and the timed pause (305 unit tests,
  Jetpack Glance 1.2.0). It adds:
  - The timed pause rules: a widget tap pauses for 1 hour while ON and
    turns alerts ON while paused; a timed pause past its time counts as
    ON. The settings repository changes the pause in one atomic
    read-and-write, so two quick taps never make a 2-hour pause.
  - The home-screen widget, "Beacon" in the widget picker, 4 × 2 cells by
    default: ON on yellow or PAUSED on black, in the system font, one tap
    target with one spoken label. Each line is measured with
    `StaticLayout` as the launcher will draw it, and lines that don't fit
    are left out in a fixed order: the tap hint, the title, the icon, then
    the resume time, which the spoken label still carries. The state word
    is never left out. A font-size, density or language change fits the
    widget again.
  - One `PauseCoordinator` for every pause change, from Home, the widget,
    the resume alarm, Beacon coming to the front and the screen turning
    on. It saves the pause, sets or cancels an inexact resume alarm with a
    10-minute window, and redraws the widget when the pause changed. A
    change that has started finishes even if its screen closes.
  - Home's "Back ON by itself at HH:MM." line on the PAUSED card, in the
    phone's 12- or 24-hour format. Home turns ON at the resume time by
    itself, re-reading the clock every minute and when Beacon comes back
    to the front.
  - The permissions and the foreground service that Glance's WorkManager
    would add are removed from the merged manifest, which still asks only
    for `SYSTEM_ALERT_WINDOW`, `WAKE_LOCK` and AndroidX's signature
    permission.
  - PR 2's carry-overs: rotating keeps a light-only alert while Beacon is
    in front; a real alert replaces a test without an Idle state in
    between; the blink regions are worked out only when the size or the
    bars change; the camera check for the test overlay is read when the
    overlay needs it; and tests for PR 2's gaps.
- `docs/checks/2026-11-step2-pr3.md`, with screenshots in
  `docs/checks/step2-pr3/`: PR 3's emulator checks (AVD `beacon-api36`,
  Android 16) of the widget picker, the tap and pause path between the
  widget and Home, two quick taps, the 12- or 24-hour setting,
  auto-resume with the emulator's clock moved forward (by the screen
  turning on and by the resume alarm), the widget at font 1.0 and 2.0 at
  two sizes, TalkBack, and rotation with a light-only alert. They found
  three defects, all fixed in PR 3 and re-checked on the emulator, with
  ten more screenshots (42 to 51): after only a font-size change the
  widget stayed clipped until Glance drew it afresh (D1); the launcher let
  it shrink to 3 × 2 (D2); and at 3 × 2 "PAUSED" broke and the resume time
  was cut off (D3). The re-checks also cover the alarm's 10-minute window,
  no drawing session when nothing changed, and none for a rotation. The
  file also lists what stays not tested until the phone is connected.
- `app/`: Beacon's launcher icon, a black bolt on the design's yellow
  (313 unit tests). The owner chose it from three ideas. It adds:
  - An adaptive icon, `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`
    in `res/mipmap-anydpi/`, with vector background, foreground and
    monochrome layers in `res/drawable/`. The manifest now sets
    `android:icon` and `android:roundIcon`, so Android no longer shows its
    default icon, and lint's `MissingApplicationIcon` warning is gone.
  - The monochrome layer, so Android 13 and later can show a themed icon.
  - `LauncherIconTest` (8 tests): both icons inflate as adaptive icons with
    a monochrome layer; the manifest names both; the colours are
    `BeaconColors.Accent` and `OnAccent` at 13.93:1; the path is the
    design's bolt; and every corner of the stroked bolt stays inside the
    66 dp safe zone.
- `docs/checks/2026-11-app-icon.md`, with screenshots in
  `docs/checks/app-icon/`: the icon's unit-test and lint results, and its
  emulator checks (AVD `beacon-api36`, Android 16). The icon shows on the
  launcher's app list and home screen, on App info, in the widget picker's
  list and in its opened entry, and, with Themed icons on, as a themed
  bolt on the home screen. Themed icons were switched off again
  afterwards.

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
  - PRs 0, 1 and 2 marked merged (yanivil/beacon#5, #6 and #7, on
    9 November 2026), and PR 3's plan linked.
  - PR 3 built on `feature/step2-widget`, not merged yet, with links to
    its plan and its emulator checks and the three defects they found,
    all fixed, and a "What PR 3 delivers" section ending with what is not
    in PR 3.
  - The app icon built on `feature/app-icon`, with a link to its emulator
    checks, and a "The launcher icon" section.
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
- `docs/specs/2026-11-08-step2-design.md`, for the launcher icon:
  - Status: the launcher icon built on `feature/app-icon`.
  - Section 8: a "Launcher icon" part: the look, the three layers, the size
    and safe zone, the themed icon and the widget picker.
  - Section 11: the icon's `#0B0B0D` on `#FFD60A` pair (13.93:1), held to
    7:1 although WCAG asks only 3:1 of a graphic, and checked by
    `LauncherIconTest` rather than the text-pair table.
  - Section 15: what `LauncherIconTest` checks, and the icon's emulator
    checks.
  - Section 16: the app icon as a PR of its own, with a link to its checks.
- `docs/plans/2026-11-08-step2-pr1-screens.md`:
  - The library versions: Compose BOM 2026.06.01 (Compose 1.11.4), core-ktx
    1.18.0 and lifecycle 2.10.0. The newer Compose 1.12, core 1.19 and
    lifecycle 2.11 need compileSdk 37, and the project rules fix compileSdk
    at 36.
- `docs/specs/2026-11-08-step2-design.md`, before PR 3: a "Plan for PR 3"
  row in its header, and PRs 0 to 2 recorded as merged in section 16.
- `docs/specs/2026-11-08-step2-design.md`, while building PR 3:
  - Status: PR 3 built, with its emulator checks' defects D1 to D3 open.
  - Section 5: every pause change goes through `PauseCoordinator`, in one
    atomic read-and-write of the stored pause; when an ended timed pause
    is cleaned up.
  - Section 9: the merged manifest after removing what Glance's
    WorkManager adds, the widget's two receivers, and no boot or
    exact-alarm permission.
  - Section 10: the widget's sizes (its minimum, and the sizes seen on the
    emulator), the fit's fixed constants and the order lines are left out,
    the picker entry, the resume alarm's window, and that a font-size
    change alone doesn't redraw the widget.
  - Section 13: rows for a restart during a timed pause (ruling 4) and for
    the phone's clock set back by hand while the widget is updating (on
    the emulator the widget then showed ON while a pause was saved), and
    the alarm's window in the "timed pause ends while nothing runs" row.
  - Section 16: PR 3 built on `feature/step2-widget`, with links to its
    plan and checks and the three defects they found.
- `docs/specs/2026-11-08-step2-design.md`, in PR 3's final fixes:
  - Status: PR 3's defects D1 to D3 fixed and re-checked on the emulator.
  - Section 5: a started pause change always finishes; the widget is
    redrawn only when the saved pause changed, and the alarm is always set
    again.
  - Section 6: the screen-on catch-up redraws the widget only when it
    changed the pause.
  - Section 10: the minimum size is in dp, so 3 × 2 on launchers with wide
    columns and possibly a single row on launchers with tall rows; the
    default size on Android 10 and 11; the measured fit, which replaces the
    constants table; the new order lines are left out in, with the resume
    time last and the state word never; the emulator's results after the
    fix; fitting again after a font-size change; the alarm's 10-minute
    window.
  - Section 11: the widget has no role in Glance, and its label starts
    with the state and says what a tap does.
  - Section 13: the alarm's window and the two catch-ups seen clearing an
    ended pause; a clock set back by hand or by automatic time; a new row
    for a font-size change while Beacon isn't running.
  - Section 15: the widget's measure, its re-fit and the coordinator's
    tests.
  - Section 16: D1 to D3 fixed.
- `docs/plans/2026-11-09-step2-pr3-widget.md`: rulings 3 (the alarm), 6
  (the size) and 7 (the fit and its order) each say how they were amended
  while building.

### Fixed

- PR 3's final fixes, in `app/`:
  - A pause change started from Home finishes even if Beacon's screen is
    closed at once, so closing it can't leave the alarm or the widget
    behind the saved pause (ruling F1). This covers Beacon being closed,
    not its process being killed during the change. A widget refresh lost
    that way isn't repaired by a later catch-up while the pause stays the
    same, so the widget can show the old state until the next pause
    change, the resume alarm or a tap.
  - The resume alarm's window is 10 minutes instead of Android's 75% of
    the delay, which was 45 minutes for a 1-hour pause (ruling F2).
  - A screen-on or a return to Beacon with nothing to clean up no longer
    starts a Glance drawing session; the alarm is still set again
    (ruling F3).
  - The widget's lines are measured as the launcher draws them, so
    "PAUSED" no longer breaks at 3 × 2 (D3, ruling F5), and the resume
    time is left out after the icon when it can't fit beside the state
    word (ruling F4).
  - After only a font-size change the widget is fitted again, also inside
    a running Glance session (D1, ruling F6).
  - The widget's minimum stays 250 × 110 dp, and 3 × 2 is now drawn
    without clipping (D2, ruling F4).
  - KDoc for the listener's screen-on receiver and three test helpers, and
    corrected comments (ruling F8).
- PR 3's follow-up R1, in `app/`: the widget follows a 12/24-hour,
  time-zone or language change by itself. Since ruling F3 a timed pause
  kept its "Back ON" time in the old format or zone until the next pause
  change, the alarm or a tap. A new `TimeChangeReceiver`, in the manifest,
  not exported and with no new permission, gets TIME_SET (also sent for
  the 12/24-hour switch), TIMEZONE_CHANGED and LOCALE_CHANGED. The widget's
  key now also holds the 12/24-hour setting and the time zone, so a
  running Glance session redraws, and a new catch-up redraws the widget
  once while a timed pause runs, and in any state after a language
  change. 16 new unit tests (305). Re-checked on the emulator through the
  Settings app, with no screen off and on (checks 6 and 20 to 22 in
  `docs/checks/2026-11-step2-pr3.md`); the spec's sections 5, 9, 10, 13,
  15 and 16 describe it, and the plan's ruling 5 records it as an
  amendment of ruling F3.
- PR 3's two lint errors, in `app/`: `./gradlew :app:lintDebug` now passes
  (0 errors, 21 warnings). `StaticLayoutMeasure` sets the break strategy
  with `LineBreaker`'s constant, which has the same value, and the
  manifest's removal entry for WorkManager's foreground service tells lint
  to ignore `MissingClass`, because the class is declared only in
  WorkManager's own manifest.
