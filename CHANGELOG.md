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
