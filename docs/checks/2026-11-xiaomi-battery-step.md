# The Xiaomi battery step: checks

This page records the fix for the phone checks' defect P2, on
`fix/xiaomi-battery-step`: the evidence, its ruling, with what it costs if
wrong, and its checks.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**.

## The defect

On Xiaomi phones, Beacon's "Battery: No restrictions" row said "Tap Battery
saver, then No restrictions." On the test phone, in Hebrew, the App info
row to tap is called "חשמל". "Battery saver" is only the heading on the
next page ([`2026-11-phone-checks.md`](2026-11-phone-checks.md), P2). The
phone checks didn't know whether English HyperOS used "Battery saver".

## Evidence: HyperOS's own labels

The labels come from HyperOS's Security app (`com.miui.securitycenter`
12.8.6-260915.1.1), which draws App info and the battery page. Its package
was copied off the test phone with `adb pull` and read with `aapt2 dump
resources`. Nothing on the phone was changed (Tested on Redmi 15C 5G,
Android 16, 11 November 2026).

| Where | Resource | English | Hebrew |
|---|---|---|---|
| App info: the row Beacon's step must name | `app_manager_details_electric_title_new` | **Power** | חשמל |
| The same row in older versions | `app_manager_details_electric_title` | Battery | |
| The value under the row | `app_manager_details_electric_title_value` | Battery usage %s | ניצול סוללה %s |
| The next page's title | `pc_battery_detail` | Battery details | פרטי הסוללה |
| Its heading | `app_manager_permission_power_saving_title` | Battery saver | חסכון בסוללה |
| The option to choose | `app_manager_powerkeeper_no_restrict` | **No restrictions** | אין הגבלות |

So the row was misnamed in English as well: "Battery saver" is the next
page's heading.

## Ruling

- **R1. The Xiaomi step says "Tap Power (or Battery), then No
  restrictions."**
  - "Power" is the row's name on HyperOS 3, from HyperOS's own strings.
    "Battery" is the older name of the same row, which the same app still
    carries, so phones on older versions find it too.
  - "No restrictions" is unchanged: it is the option's name.
  - A comment by the string records where the names come from, with the
    Hebrew labels for a future translation.
  - Cost if wrong: a HyperOS or MIUI version that names the row something
    else again. The person still lands on Beacon's App info page and looks
    for one row.

## Build machine

| Check | Result |
|---|---|
| New test, `HomeScreenTest.xiaomiStepsNameHyperOsRows` | It pins the Xiaomi steps to "Tap Power (or Battery), then No restrictions.". It failed against the old string and passes with the new one. `batteryStepsOnThisPhone` now checks that the new Xiaomi text isn't shown on a phone that isn't a Xiaomi. |
| `./gradlew :app:testDebugUnitTest` | 342 tests, 0 failures: the 341 before, and the new one. |
| `./gradlew :app:lintDebug --rerun-tasks --warning-mode all` | "No issues found", and no warnings. |

## Not tested

- **The new text on the test phone.** It shows only on a Xiaomi; the
  emulator isn't one. The next build installed there will show it.
- **HyperOS in English**, seen on the phone rather than read from the
  Security app.
- **Older Xiaomi phones** (MIUI, HyperOS 1 and 2), where the row may be
  "Battery".
- **Other Xiaomi brands' skins** (Redmi and POCO run the same HyperOS).
