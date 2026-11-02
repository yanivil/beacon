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
