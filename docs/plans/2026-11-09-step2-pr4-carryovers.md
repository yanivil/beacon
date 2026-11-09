# Step 2, PR 4: PR 2's last two carry-overs

**Goal:** finish the two items that PR 2's reviews left and PR 3 didn't
take:
1. Tell the alert controller when the camera light refuses mid-alert.
2. Make the battery steps for phones other than Xiaomi match what Android
   shows.

Also record that PR 3 (yanivil/beacon#8) and the app icon
(yanivil/beacon#9) are merged.

**Approval:** the owner said on 9 November 2026 to go on to the other open
tasks without asking, and that work may be merged to `main`. That standing
instruction stands in for a separate plan review. The controller ruled the
design below; each ruling is listed with what it costs if wrong.

**Spec:** `docs/specs/2026-11-08-step2-design.md`. This plan touches its
sections on the alert engine (the light and the heat watch), the setup
card (the battery row) and section 16 (delivery).

**Branch:** `feature/step2-carryovers`, from `main` at `167d396`.

## Global constraints

`CLAUDE.md` applies in full. The parts that matter most here:
- KDoc on every class and function, block comments for the why, and inline
  comments for edge cases.
- All user-facing text in `strings.xml`, with start/end, never left/right.
- Body text at least 20 sp. Every text colour pair at least 7:1.
- Results are reported as "Tested on emulator beacon-api36, Android 16"
  or "Not tested". "Should work" counts as Not tested.
- Use only `emulator-5554`. Never change a setting on the phone.
- Never commit to `main`. Each commit ends with the `Co-Authored-By:` line
  of the model that wrote it.
- README, CHANGELOG `[Unreleased]` and the affected spec sections are
  updated with the change.

## Rulings

1. **A refusal is reported back, and the screen blink carries on.**
   `TorchBlinker` takes an `onRefused: () -> Unit` callback. It runs once,
   on the torch looper, after the blinker's one attempt to leave the light
   off. `AlertController` posts it to the main looper and treats it like
   the heat stop for that blinker: the light is marked off
   (`lightFlashing = false`) and the 30-second heat poll is cancelled. A
   late callback from an alert that has already ended or been replaced
   changes nothing (the same `blinker === forBlinker` guard as
   `onTooHot`).
   - Cost if wrong: none for alerts. The screen blink and the wake lock are
     untouched.
2. **The heat listener isn't unregistered on a refusal,** the same as on
   the heat stop. Its callback is already guarded, and `endCurrent`
   unregisters it.
   - Cost if wrong: one idle listener until the alert ends.
3. **The battery steps follow the page Beacon opens** (App info,
   `ACTION_APPLICATION_DETAILS_SETTINGS`).
   - The wording for stock Android is taken from the emulator's own
     screens (Android 16), read with `uiautomator dump`.
   - Where older Android versions name the path differently, the text is
     chosen by `Build.VERSION.SDK_INT`, and those versions are reported as
     Not tested.
   - Xiaomi's text doesn't change.
   - Cost if wrong: on an untested Android version, a step label that
     doesn't match the screen.
4. **No emulator check for the refusal.** The emulator has no camera light
   to take away from Beacon (PR 2's checks), so the refusal is covered by
   unit tests and stays Not tested on devices.

## Task 1: the refusal reaches the controller

**Files:**
- `app/src/main/kotlin/dev/beacon/alert/TorchBlinker.kt`
- `app/src/main/kotlin/dev/beacon/alert/AlertController.kt`
- their tests in `app/src/test/kotlin/dev/beacon/alert/`

**Interfaces:**
- `TorchBlinker(torch, clock, looper, onRefused: () -> Unit = {})`.
- The controller passes a callback that posts
  `onLightRefused(newBlinker)` to the main looper.
- The heat stop and the refusal share one private function, so the "mark
  off, cancel the poll" logic is written once.

**Tests (write each one failing first, then make it pass):**
- `TorchBlinkerTest`:
  - `aRefusalCallsOnRefusedOnce`: the switch refuses at a later flip. The
    callback runs exactly once, after the final `set(false)`.
  - `aNormalEndNeverCallsOnRefused`
  - `aStopNeverCallsOnRefused`
- `AlertControllerTest`:
  - `aRefusedLightStopsTheHeatPoll`: the heat listener is refused, so the
    poll is posted. Then the light refuses. Advancing past several
    `HEAT_POLL_MILLIS` gives no further `isTooHot` call. The state is
    still Running with `lightFlashing = false`, and the wake lock is still
    held.
  - `aRefusalAfterTheAlertEndedChangesNothing`
  - `aRefusalOfAReplacedTestChangesTheNewAlertNothing`: a real alert
    replaces a test, then the test's blinker reports a refusal. The real
    alert's state and poll are unchanged.
- Check what reads `lightFlashing`, for example the test overlay's camera
  line. A unit test pins that it follows the refusal.

## Task 2: battery steps for other phones

**Files:**
- `app/src/main/res/values/strings.xml`
- `app/src/main/kotlin/dev/beacon/ui/home/SetupCard.kt`, or
  `setup/SetupIntents.kt` if the choice of text belongs there
- their tests

**Steps:**
1. **Read the real wording on the emulator.**
   - On `emulator-5554`, open Beacon's setup card and tap the Battery row.
   - Dump each screen on the way to the setting that makes
     `PowerManager.isIgnoringBatteryOptimizations` true. On stock
     Android 16 that is probably "App battery usage", "Allow background
     usage", then "Unrestricted", but use what the screen says.
   - Check that the Battery row then shows ON. Restore the setting
     afterwards.
2. **Write the text.** It names the labels in the order the person taps
   them, in the setup card's existing style ("Tap …, then …").
   - If Android 12 and 13 (API 31 to 33) or Android 10 and 11 (API 29 and
     30) use other labels, add one string per wording and choose by
     `SDK_INT`. Those wordings are Not tested.
   - Keep Xiaomi's string.
3. **Test it.** Robolectric tests with `@Config(sdk = …)` for each branch
   check that a non-Xiaomi phone shows the right string.
   - At font 2.0 the row must still wrap without clipping. Use the
     existing largest-font test pattern, if there is one.

## Task 3: docs

- **README:**
  - Status: PR 3 merged as yanivil/beacon#8 and the icon as #9. PR 4 built
    on `feature/step2-carryovers`.
  - Add a short "What PR 4 delivers" section.
  - Remove the two items from PR 3's "Not in PR 3" line.
- **CHANGELOG `[Unreleased]`:** the two changes, and this plan.
- **Spec:**
  - The alert-engine section: the refusal now stops the heat poll.
  - The setup card section: the battery wording, and its versions with
    their labels.
  - §16: PR 3 and the icon merged, and a PR 4 row.
  - §17: unchanged unless something moves.
- **`docs/checks/2026-11-step2-pr4.md`:** the emulator check of the
  battery path, with screenshots in `docs/checks/step2-pr4/` (cropped,
  small). Include a Not-tested list: the refusal on a device, the older
  Android wordings, and every phone check.
- **This plan:** under each ruling, note how it was amended while building,
  if it was.
