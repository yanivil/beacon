# Flash only when the phone isn't in use: checks

This page records the owner's decision 1 from the phone checks, on
`feature/flash-only-when-not-in-use`: its rulings, with what they cost if
wrong, and its checks.

Every emulator result is labelled **"Tested on emulator beacon-api36,
Android 16"** or **"Not tested"**.

## The decision

At the test phone, while the owner was using it, a message turned the whole
screen into a flashing yellow, which was disruptive
([`2026-11-phone-checks.md`](2026-11-phone-checks.md), decision 1). On
11 November 2026 the owner decided:
- A real alert flashes only when the screen is off, or when it's on but
  showing the lock screen.
- While the phone is unlocked and in use, Beacon does nothing: no screen
  flash and no camera light. The message app's own notification is the
  cue then.
- The test alert is not affected.

## Rulings

- **R1. "In use" is a screen that is on and not showing the lock screen.**
  - `phoneInUse(context)` returns `PowerManager.isInteractive` and not
    `KeyguardManager.isKeyguardLocked`.
  - An always-on display counts as off, because Android reports it as not
    interactive. A phone with no lock screen at all is in use whenever its
    screen is on.
  - Cost if wrong: a person who left the phone on, unlocked, gets no flash
    until the screen times out. That follows from the owner's choice.
- **R2. It is a new message rule, 7, with the reason `IN_USE`.**
  - It comes after the owner's-own-message rule and before the pause and
    the app switches, so the log names "in use" for an incoming message
    whatever the pause and switches say.
  - Like the rules after it, it runs after the message is recorded as
    handled. A message that arrived while the phone was in use therefore
    never flashes later when the screen goes off: the person has seen its
    banner. A newer message in the same chat still flashes, once the phone
    isn't in use.
  - The check is asked only when a message's decision reaches rule 7,
    after any wait for the saved settings, so other apps' notifications
    cost nothing. If a system service can't be read, the phone counts as
    not in use, so the alert flashes rather than the listener crashing.
  - Cost if wrong: a person who unlocked the phone but didn't look at the
    banner misses that message's flash.
- **R3. `CLAUDE.md`'s scope records the decision**, next to the owner's
  8 November decisions, so every session follows it.

## Where the rules and the design disagree

The design doesn't say whether an alert flashes while the phone is in use.
This follows the owner's decision. Home's line "Your screen and camera
light will flash when a message arrives." was left to the owner, who then
asked for it to say so: it ends "…and you aren’t using the phone." since
the follow-up on `fix/home-in-use-wording` (yanivil/beacon#21). On the test
phone, with `main`'s build at `d3675d8`, Home's ON card shows that line in
full, with its full stop at the end in the phone's Hebrew (Tested on Redmi
15C 5G, Android 16, 11 November 2026).

## Build machine

| Check | Result |
|---|---|
| New tests | 12 tests. `MessageClassifierTest` (6): a message while in use is `IN_USE`; it doesn't flash later, but a newer one does; a repeat while still in use is already handled; the owner's own message is still named first; "in use" is named before a pause and an app switched OFF; the check is asked only for a message that reaches its rule. `MessageWatcherTest` (1): no alert while in use, then an alert once it isn't. `PhoneUseTest` (3): screen on and unlocked is in use; the lock screen and a screen that is off are not. `BeaconListenerTest` (2), through the real app: no alert with Robolectric's screen on and unlocked, and an alert with the lock screen showing. Against the code before the rule, the first 8 written included 5 that failed (`inUseIgnoresTheMessage`, `aMessageSeenInUseDoesNotFlashLater`, `inUseComesBeforePausedAndAppOff`, `noAlertWhileThePhoneIsInUse` in the watcher, `screenOnAndUnlockedIsInUse`); the rest are guards. With the app's check replaced by "never in use", `BeaconListenerTest.noAlertWhileThePhoneIsInUse` fails, so it guards the wiring. All pass with the rule. |
| An existing test | `BeaconListenerTest.postedNotificationStartsAnAlert` now turns Robolectric's screen off first: its phone reports the screen on and unlocked, which is now "in use". |
| `./gradlew :app:testDebugUnitTest` | 356 tests, 0 failures: the 344 before, and the 12 new ones. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:lintDebug --rerun-tasks --warning-mode all` | No Kotlin compiler warnings, and lint reports "No issues found". The new test first used Robolectric's deprecated `setIsInteractive`; it uses `turnScreenOn`. |

## Emulator

| | |
|---|---|
| Emulator | AVD `beacon-api36`, Pixel 7 profile, run as `emulator-5554` |
| System image | Android 16 (API 36), build `BE2A.250530.026.F3` |
| Build | Debug APK of `feature/flash-only-when-not-in-use`, installed with `adb install -r` |
| Setup | Notification access and "Display over other apps" granted over adb; SMS sent with `adb emu sms send` to Google Messages, the default SMS app |
| Date | 11 November 2026 |

| Check | Result |
|---|---|
| Screen on, unlocked, launcher in front | Tested on emulator beacon-api36, Android 16. Beacon decided `Ignore(reason=IN_USE)`; no alert opened. |
| Screen off | Tested on emulator beacon-api36, Android 16. Beacon decided `Alert(source=SMS)`; the screen woke and the alert opened. Back ended it. |
| Screen on, lock screen showing | Tested on emulator beacon-api36, Android 16. With the swipe lock switched on for this check, the screen woken and the lock screen showing, Beacon decided `Alert(source=SMS)` and the alert opened over the lock screen. Back ended it. |

Afterwards notification access was taken back (the two system listeners
only), "Display over other apps" reset to its default, and the lock screen
switched off again, as it was. Google Messages on the emulator holds three
more test SMS from 555-1234. The build is left installed.

## Not tested

- **The test phone** with this build, and an always-on display.
- **WhatsApp** on the emulator: it has none. The rule doesn't depend on
  the app.
- **A phone with no lock screen at all,** and a secure lock (PIN or
  fingerprint) showing.
- **The camera light while in use:** it stays off because no alert starts;
  not watched separately.
