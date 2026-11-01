# Beacon: rules for every session

Beacon is a native Android app that turns incoming calls and messages into
visual alerts (a full-screen colour blink plus the camera light) for deaf and
hard-of-hearing seniors. Follow every rule below in every session.

## Project facts

- **Stack:** Kotlin + Jetpack Compose for the app, Jetpack Glance for the
  home-screen widget. Use the design's own colours and sizes; no Material
  dynamic colour.
- **SDK levels:** minSdk 29 (Android 10); compileSdk and targetSdk 36.
- **Design reference:**
  https://claude.ai/code/artifact/57db235d-b0b1-4b3d-af5c-bdee1677e1f2
  (an HTML prototype on a design canvas). It is the reference for layout,
  wording, colours, sizes and behaviour. Never port it as web code. The
  design is titled "Visual Alerts": ignore that, the app is called **Beacon**.
- **When the design and these rules disagree, the rules win.** Name the
  conflict and how you resolved it in the PR draft.
- **Plan gate:** Step 1 (feasibility tests on a real phone) is reported and
  approved before any Step 2 screens (Home, Alert settings, Test alert
  overlay, widget) are built.

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
- `origin` is `https://yanivil@github.com/yanivil/beacon.git`. Its token is a
  repo-scoped keychain entry (`credential.usehttppath true` in this repo's
  config): leave both as they are.

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
