# Xiaomi's Background autostart: checks

This page records the fix for the phone checks' defect P5, on
`feature/xiaomi-autostart-row`: the evidence, its rulings, with what they
cost if wrong, and its checks on the build machine and the test phone.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**.

## The defect and the decision

On HyperOS, with Beacon's Autostart off (its default), the phone refused to
connect Beacon's notification listener after an update or a restart
(`AutoStartManagerService: Reject service … BeaconListener`). Beacon then
heard nothing until someone opened it. An SMS at 12:10:37 on 11 November got
no decision at all ([`2026-11-phone-checks.md`](2026-11-phone-checks.md),
P5). The owner decided the same day that the Finish setup card gets a fourth
row for it on Xiaomi phones.

## Evidence

All of it was read from the test phone, without changing anything there.

- **How Xiaomi keeps Autostart.** `adb shell appops get dev.beacon 10008`
  answered `MIUIOP(10008): ignore`, while Autostart was off. Xiaomi keeps it
  in a private app-op, number 10008. Android has no public name for it:
  `appops get dev.beacon AUTO_START` answered `Unknown operation string`.
- **The page.** HyperOS's Security app (`com.miui.securitycenter`
  12.8.6-260915.1.1) was copied off the phone and read with `aapt2`:
  - its `AutoStartManagementActivity` is exported, and answers to the
    action `miui.intent.action.OP_AUTO_START`;
  - its title is "Background autostart" (Hebrew "הפעלה אוטומטית ברקע");
  - opened on the phone, it lists apps under "allowed" and "not allowed",
    each with a switch. Beacon was among those not allowed.

## Rulings

- **R1. Beacon reads the app-op with Android's hidden
  `AppOpsManager.checkOpNoThrow(int, int, String)`, by reflection.**
  - It's the only way to read an app-op by its number. On the test phone
    Android allowed it: the log says `Accessing hidden method …
    checkOpNoThrow … api=unsupported`, which is an informational note, and
    the read returned Autostart's real state.
  - Any failure, and any mode other than allowed or ignored, gives
    UNKNOWN. Phones that aren't Xiaomis aren't asked at all (NOT_XIAOMI).
  - Cost if wrong: a later Android that blocks the hidden method gives
    UNKNOWN, so the row loses its ON/OFF and works as R2 says.
- **R2. The row's behaviour follows what Beacon can read.**
  - ON or OFF: like the other three rows, with its pill and spoken state.
    OFF keeps the card open, and ON lets it close.
  - UNKNOWN: the row shows its reason, steps and button, but no pill and no
    spoken state, because showing OFF for a setting that may be on would
    mislead. It doesn't keep the card open, because Beacon couldn't tell
    when it is done.
  - Other phones have no row.
  - Cost if wrong: on a Xiaomi with an unreadable state, the row is seen
    only while another grant is missing.
- **R3. The words follow HyperOS's own page.**
  - The title is "Background autostart". The reason is "So Beacon works
    again by itself after the phone restarts." The steps are "Find Beacon
    in the list and turn its switch on."
  - "Open settings" opens the page by its action. A phone without that
    page gets Beacon's App info instead, as the other rows already do.
  - Cost if wrong: another HyperOS version with a different page or title.

## Build machine

| Check | Result |
|---|---|
| New tests | 12 tests. `AutostartTest` (4): other phones have no Autostart and aren't asked; on a Xiaomi allowed is ON and ignored is OFF; an unreadable or other mode is UNKNOWN; reading never throws. `SetupCheckerTest` (3): Autostart OFF keeps the card open, while ON, UNKNOWN and other phones don't; the row only on a Xiaomi; the checker on a phone that isn't a Xiaomi. `SetupIntentsTest` (1): the page's action. `HomeScreenTest` (4): the row with its state, steps and button on a Xiaomi; no pill or spoken state when unknown; no row on other phones; all four rows at the largest font with nothing cut off. Against placeholder code, 8 failed; the other 4 are guards. All pass. |
| `./gradlew :app:testDebugUnitTest` | 368 tests, 0 failures: the 356 before, and the 12 new ones. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:lintDebug --rerun-tasks --warning-mode all` | No Kotlin compiler warnings, and lint reports "No issues found". |

## The test phone

This branch's debug build was installed on 11 November 2026 at about 15:12,
and the owner accepted HyperOS's install prompt.

| Check | Result |
|---|---|
| Reading Autostart, off | Tested on Redmi 15C 5G, Android 16. With the other three grants on, the Finish setup card came back, with a fourth row: "Background autostart", its reason and steps, an OFF pill, and "Open settings". |
| The button | Tested on Redmi 15C 5G, Android 16. It opened HyperOS's "הפעלה אוטומטית ברקע" page (`AutoStartManagementActivity`). |
| Reading Autostart, on | Tested on Redmi 15C 5G, Android 16. The owner turned on Beacon's switch there. `appops` then answered `MIUIOP(10008): allow`, and Beacon's Home opened on "Visual alerts ON" with no setup card. |
| A restart with Autostart on | Tested on Redmi 15C 5G, Android 16. After the restart the listener couldn't start until the phone was first unlocked (15:25:57 and 15:26:11, "not found": Beacon isn't direct-boot aware). At 15:26:27 Android started Beacon for its listener by itself, with no refusal, and nobody opened Beacon. A WhatsApp message at 15:26:41 and the owner's SMS at 15:27:08.801 each flashed with the screen off. The SMS was decided `Alert(source=SMS)` 258 ms after it arrived, and the screen woke. |

Seen on the phone with this build, which carries every fix merged that day:
the Xiaomi battery row reads "Tap Power (or Battery), then No
restrictions." with its full stop at the end (P2, and P1's direction).

## Not tested

- **A phone where the hidden method is blocked:** the UNKNOWN row is seen
  only in the tests.
- **Other HyperOS and MIUI versions,** where the page or its title may
  differ.
- **TalkBack on the new row** and **the largest font on the phone.** Both
  are covered by the Robolectric tests only.
- **Process death without a restart,** such as HyperOS killing Beacon for
  memory, with Autostart on.
