# Beacon: rules for every session

Beacon is a native Android app that turns incoming messages into visual
alerts (a full-screen colour blink plus the camera light) for deaf and
hard-of-hearing seniors. Follow every rule below in every session.

## Project facts

- **Stack:** Kotlin + Jetpack Compose for the app, Jetpack Glance for the
  home-screen widget. Use the design's own colours and sizes; no Material
  dynamic colour.
- **SDK levels:** minSdk 29 (Android 10); compileSdk 37; targetSdk 36.
  The owner raised compileSdk from 36 on 11 November 2026, so Compose 1.12,
  Lifecycle 2.11 and Core 1.19 can be used. targetSdk stays at 36.
- **Design reference:**
  https://claude.ai/code/artifact/57db235d-b0b1-4b3d-af5c-bdee1677e1f2
  (an HTML prototype on a design canvas). It is the reference for layout,
  wording, colours, sizes and behaviour. Never port it as web code. The
  design is titled "Visual Alerts": ignore that, the app is called **Beacon**.
- **When the design and these rules disagree, the rules win.** Name the
  conflict and how you resolved it in the PR draft.
- **Scope (owner's decisions, 8 November 2026):** Beacon flashes only when a
  message is received: SMS, WhatsApp and Telegram messages. Never for
  calls, backups or other app activity. Telegram wasn't tested in Step 1:
  build it anyway, and report it as "Not tested". Phone calls and WhatsApp video calls
  are out of scope, although the design shows them. "How long to flash"
  defaults to 1 minute. Step 1 results and their reasons are in
  `docs/feasibility.md`.

## Plan gate

- Step 1 (feasibility tests on a real phone) is reported and approved
  before any Step 2 screens (Home, Alert settings, Test alert overlay,
  widget) are built.
- Report each Step 1 result as **"Tested on <phone model>, Android
  <version>"** or **"Not tested"**. "Should work" counts as not tested.
- Step 1 also runs the camera light at **Fast for 60 minutes** and records
  any overheating warning and the battery drop.

## Accessibility (non-negotiable)

- WCAG AAA: every text colour pair has at least **7:1** contrast. Check
  each new foreground/background pair before using it.
- Body text at least **20sp**. Headers at least **28sp** and **bold**.
- Touch targets at least **64dp** in both directions.
- Every switch shows **ON/OFF text**. State is never shown by colour alone.
- **No** sliders, **no** hamburger menus, **no** icon-only buttons: every
  button has a visible text label.
- Navigation is a **flat bottom bar with exactly 2 tabs**.
- Must stay usable at the **largest system font size**: text in sp, layouts
  that wrap and scroll, no fixed text heights, nothing clipped or truncated.
- Every control works with **TalkBack**: correct roles (Switch, RadioButton,
  Tab), state read aloud ("On"/"Off", "Selected"), and a spoken label for
  every icon. Check each screen with TalkBack on.
- All user-facing text lives in **`strings.xml`**, never hard-coded; layouts
  use **start/end, never left/right**, so a Hebrew (right-to-left) version
  stays possible.

## Blink safety

- Blink speed is one of three presets only: **Slow 30, Medium 60, Fast 90**
  blinks per minute (one blink = one on/off cycle).
- Never more than **3 flashes per second** (WCAG 2.3.1). Fast 90 is
  1.5 flashes per second.
- **No free number entry** for speed: code that sets a blink rate takes the
  preset type, never a raw number.

## Git

- **Never commit to `main`.** Work only on `feature/<desc>` or `fix/<desc>`
  branches.
- Every change comes with a full **PR draft**: Title, Context/Why, Summary
  of Changes, Test Steps, Doc Impact.
- Authenticate to GitHub **only with the repo-scoped token in the macOS
  keychain**: server `github.com`, path `yanivil/beacon.git`, account
  `yanivil`. Don't use gh's own login or any other credential, and don't run
  `gh auth login` or `gh auth setup-git`.
  - **git** (fetch, pull, push) uses it by itself through the `osxkeychain`
    helper, because this repo's config has `credential.usehttppath true` and
    `origin` is `https://yanivil@github.com/yanivil/beacon.git`. Leave both
    as they are.
  - **gh** (PRs, merges) gets it for one command at a time, run from the
    repo root:

    ```bash
    GH_TOKEN="$(printf 'protocol=https\nhost=github.com\npath=yanivil/beacon.git\nusername=yanivil\n\n' | GIT_TERMINAL_PROMPT=0 git credential fill | sed -n 's/^password=//p')" gh pr create ...
    ```

  - **Never print, log, commit or save the token anywhere else**, and never
    create, change or delete keychain entries.
  - If access fails, stop and ask me.

## Code comments

- **Block comments** explaining the "why" of each section.
- **KDoc on every class and function:** purpose, `@param`, `@return`,
  `@throws`.
- **Inline comments** for edge cases, fallbacks and non-obvious logic.

## Docs

With every change:

1. Update `README.md`.
2. Add an entry under `[Unreleased]` in `CHANGELOG.md`.
3. Update any affected specs (in `docs/`).

## Response order

When reporting a change, always in this order:

1. Branch name
2. Code
3. Updated docs
4. PR draft
5. Verification checklist
