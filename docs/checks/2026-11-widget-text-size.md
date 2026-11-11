# The widget's text size: checks

This page records the owner's widget decision from the phone checks, which
fixes defect P4, on `feature/widget-larger-text-fit`: its rulings, with
what they cost if wrong, and its checks.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"**, **"Tested on
emulator beacon-api36, Android 16"** or **"Not tested"**.

## The defect and the decision

On the test phone, at the owner's text size (1.45) and display size, the
4 × 2 widget is 301.83 × 154.69 dp. Paused, it showed only "PAUSED": the
line "Back ON by itself at 12:54" wraps onto two rows there and didn't fit
under a 52 sp "PAUSED" inside 16 dp of padding
([`2026-11-phone-checks.md`](2026-11-phone-checks.md), P4). The spoken
label still named the time.

The owner asked to see smaller text. Two preview builds ran on the phone,
and the owner chose the second on 11 November 2026: "PAUSED" at 40 sp, "ON"
at 48 sp, and 12 dp of padding.

## Rulings

- **W1. The state word is 40 sp (PAUSED) and 48 sp (ON), and the widget's
  padding is 12 dp on each side.**
  - These are the owner's values from preview 2, not the design's 52 and
    64 sp and 16 dp. The spec's section 3 now lists this as a difference
    from the design, decided by the owner.
  - Both sizes stay well above the rules' 28 sp floor for headers. The
    other lines (28 and 22 sp) don't change, nor do the drop order and the
    measure.
  - The sizes are named constants (`STATE_ON_SP`, `STATE_PAUSED_SP`), and
    the fit and the drawing share `WIDGET_PADDING_DP` as before.
  - Cost if wrong: a smaller word than the design's on large widgets,
    where 52 and 64 sp would also have fitted.
- **W2. The fit is tested with the test phone's own line heights.**
  - A diagnostic build logged what the widget's measure returned on the
    phone: "PAUSED" at 40 sp 55.3 dp, "ON" at 48 sp 66.3 dp, the two-row
    resume line 69.1 dp, the title 41.0 dp, the tap hint 37.7 dp.
    "PAUSED" at 52 sp was drawn 69.5 dp tall, so 71.5 dp with the measure's
    margin.
  - `WidgetFitOnPhoneTest` runs the fit at the phone's 301.83 × 154.69 dp
    with those heights. With the old sizes and padding, the PAUSED test
    failed on the fit itself (only the word was kept), and the ON test
    failed because the phone measured no height for a 64 sp "ON". Both
    pass with the new values.
  - Cost if wrong: none at run time; the heights belong to this phone and
    this text size only.
- **W3. Four tests that pinned the old sizes are updated.**
  - `StaticLayoutMeasureTest.heightsMatchTheEmulator` checks the measure
    against the heights the emulator's launcher drew in PR 3, when
    "PAUSED" was 52 sp. It now measures an explicit 52 sp "PAUSED", so it
    still compares like with like.
  - At 3 × 2 and font 1.0 the pause now keeps every line and the icon
    (`paused3x2KeepsEverything`, before: the word and the resume time
    only). At 3 × 2 and the largest font it keeps the word, the icon and
    the resume time (`paused3x2KeepsTheResumeTime`, before: the word
    alone). Both still pass the file's checks that the kept lines fit
    inside and the word takes one row.
  - `WidgetContentTest.iconGoesWhenTheFitDropsIt` needed a narrower widget
    to still drop the icon: 190 × 160 dp instead of 232 × 160 dp, worked
    out from the estimate it uses.

## Build machine

| Check | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest` | 344 tests, 0 failures: the 342 before, and the 2 new ones. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:lintDebug --rerun-tasks --warning-mode all` | No Kotlin compiler warnings, and lint reports "No issues found". |

## The test phone

| Check | Result | Screenshot |
|---|---|---|
| Preview 2 paused at 4 × 2, text size 1.45 | Tested on Redmi 15C 5G, Android 16, with preview 2. It has the same three values as this PR, plus a diagnostic log line. The widget shows "PAUSED" with its icon, and "Back ON by itself at 12:54" on two rows. The owner chose this look. | [07](phone-checks/07-widget-preview.png) |
| Preview 2 ON at 4 × 2, text size 1.45 | Tested on Redmi 15C 5G, Android 16, with preview 2 after the restart. It shows "Visual alerts" and "ON" with the bolt; the tap hint is dropped. | |

## Not tested

- **This branch's own build on the phone.** Preview 2 has the same values,
  but this build hasn't been installed there.
- **The widget on the emulator.** The Robolectric tests measure with
  Android's real text layout at the emulator's density, at font 1.0 and
  2.0, but no widget was placed on the emulator for this PR.
- **Other launchers and cell sizes,** and the widget in landscape.
- **HyperOS's launcher scale.** It draws other apps' widgets at 0.9 times
  the density (the phone checks' PO5), so on HyperOS the fit leaves a
  little room to spare.
