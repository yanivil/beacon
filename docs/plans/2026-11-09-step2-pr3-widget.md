# Step 2, PR 3: the widget and the timed pause — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the home-screen widget that shows ON or PAUSED and pauses
alerts for 1 hour with one tap, and the timed pause behind it: Home's "Back
ON by itself at HH:MM" line, the resume alarm and keeping the widget
current.

**Architecture:**
- **Pause rules:** plain Kotlin in `settings`. The repository gains one
  atomic read-modify-write for the pause.
- **One coordinator:** every pause change goes through `PauseCoordinator`,
  from Home, the widget and the alarm. It saves the pause, sets or cancels
  the resume alarm, and refreshes the widget.
- **The widget:** built with Glance. Its look is a composable fed plain
  state, and a pure function decides which lines fit.
- **Keeping it current:** Home works out "paused" from the clock, so a timed
  pause ends on screen at its time. The listener catches up when the screen
  turns on, and so does `MainActivity` when Beacon comes to the front.

**Tech Stack:** as PR 1 and PR 2, plus Jetpack Glance
`glance-appwidget` 1.2.0 and, for tests, `glance-appwidget-testing` 1.2.0.

**Spec:** [`docs/specs/2026-11-08-step2-design.md`](../specs/2026-11-08-step2-design.md).
This plan covers spec section 16, PR 3:
- section 5 ("Pause")
- section 8 (Home's PAUSED card line)
- section 10 ("The widget")
- section 3 rows 1, 5, 11 and 12
- section 13 ("A timed pause ends while nothing runs")
- the items PR 2's reviews left for PR 3

Executors read the spec too.

## Global Constraints

- **Branch:** `feature/step2-widget`, created from `main` after PRs 0, 1 and
  2 merged (9 November 2026). Never commit to `main`. Each commit message
  ends with a `Co-Authored-By:` line naming the model that wrote it.
- **Running Gradle:** from the repo root, through `zsh -lc '…'`, so
  `JAVA_HOME` (JDK 21) and `ANDROID_HOME` load.
- **SDK and app:** unchanged: minSdk 29, compileSdk 36, targetSdk 36,
  `dev.beacon`, Java and Kotlin target 17.
- **New libraries, exact versions:**
  - `androidx.glance:glance-appwidget:1.2.0`
  - test only: `androidx.glance:glance-appwidget-testing:1.2.0`
  - Why: 1.2.0 (26 August 2026) is the newest stable release, and the
    compileSdk 37 note in Glance's release notes is for 1.3.0-alpha01.
  - If the build reports that a Glance artifact needs compileSdk 37,
    stop and report. Do not change compileSdk.
  - No Material or Glance-Material artifacts.
- **Manifest permissions:** the merged manifest's `<uses-permission>` set
  is exactly these three:
  - `android.permission.WAKE_LOCK`
  - `android.permission.SYSTEM_ALERT_WINDOW`
  - `dev.beacon.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (from AndroidX
    core; see spec section 9)

  Glance's WorkManager dependency adds `FOREGROUND_SERVICE`,
  `RECEIVE_BOOT_COMPLETED` and `ACCESS_NETWORK_STATE`, and declares
  `androidx.work.impl.foreground.SystemForegroundService`. Each is removed
  with `tools:node="remove"` (ruling 2). Never `INTERNET`, an exact-alarm
  permission, `POST_NOTIFICATIONS`, or a foreground service.
- **Privacy:** unchanged from PR 2. No message content anywhere. The only
  `Log` call stays the debug-only decision log.
- **Threads:** `PauseCoordinator`'s functions are `suspend` and safe from
  any thread. DataStore serialises the writes. The alarm and the widget
  refresh are thread-safe Android calls.
- **Widget look:**
  - Text is in sp, in the system font, never the bundled font (spec section
    3 row 5).
  - The only text pairs are `#0B0B0D` on `#FFD60A` (13.93:1) and `#FFFFFF` on
    `#0B0B0D` (19.66:1), from spec section 11.
  - The icons are decorative, and the whole widget is one tap target with
    one spoken label.
- **Text, layout and TalkBack in the app:** as in PR 1 and PR 2:
  - All user-facing text is in `strings.xml`, and layouts use start/end.
  - Body text is at least 20 sp. Headings are at least 28 sp, bold and
    marked `heading()`.
  - Touch targets are at least 64 dp.
  - The roles, states and labels are those of spec section 11.
- **Comments:** a block comment for the "why" of each file's main section.
  KDoc on every class and function, with `@param`, `@return` and `@throws`
  where they apply. Inline comments for edge cases and fallbacks.

## Rulings this plan makes

1. **Glance 1.2.0.** It is the newest stable release that builds at
   compileSdk 36.
   - Cost if wrong: an older API surface than 1.3.
2. **WorkManager's extras are removed from the merged manifest.** These are
   the three permissions above and its foreground service. Glance only
   uses WorkManager for short update sessions, never a foreground worker
   or a network constraint.
   - Cost if wrong: if a later Glance needs one, the widget could stop
     updating. The emulator check catches that.
3. **The resume alarm is an inexact wake-up alarm**
   (`AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, …)`), so the stored pause
   is cleaned up near its time.
   - Cost if wrong: one wake-up per timed pause.
   - **Amended while building (ruling F2):** the alarm is
     `setWindow(RTC_WAKEUP, at, 10 min, …)`. Android's own window for
     `setAndAllowWhileIdle` was 75% of the delay, 45 minutes for a 1-hour
     pause; 10 minutes is the smallest window without the exact-alarm
     permission. See spec section 10.
4. **The alarm isn't re-set after a restart,** because there's no
   `RECEIVE_BOOT_COMPLETED`. Alerts don't depend on it, because they check
   the clock. The stored pause is cleaned, and the widget redrawn, the next
   time Beacon comes to the front, the screen turns on, or the widget
   updates.
   - Cost if wrong: after a restart, a stale timed pause stays stored until
     then. Nothing shows it wrongly, since every screen checks the clock.
5. **Every pause change goes through one `PauseCoordinator`.** That covers
   Home, the widget, the alarm, the app coming to the front and the screen
   turning on.
   - Cost if wrong: none.
6. **The widget can't be resized below 4 × 2 cells**
   (`minResizeWidth` 250 dp, `minResizeHeight` 110 dp, the same as its
   minimum). It can only grow. At 4 × 2, the 52 sp "PAUSED", its icon and
   the resume line already need all the room.
   - Cost if wrong: less freedom to resize.
   - **Amended while building (PR 3's D2, ruling F4):** the minimum is in
     dp, so a launcher with wide columns allows 3 × 2, and one with tall
     rows would allow a single row. The minimum stays 250 × 110 dp, and the
     widget fits itself to the smaller sizes instead. See spec section 10.
7. **The fit estimate uses fixed constants** (Task 2): 0.55 em per
   character, 1.2 line height, 16 dp padding, 4 dp gaps, and an icon as
   wide as its text plus 8 dp.
   - **The order lines go in:** spec section 10's order (the tap hint, then
     the title), then the decorative state icon, before "PAUSED" would
     wrap.
   - **When it still doesn't fit:** if the state word and the resume time
     don't fit even alone, they're kept anyway, as spec section 10 says.
     The widget's spoken label always carries every line. That case comes
     up only at the largest font on the smallest sizes, and the emulator
     records whether it happens at the default size.
   - Cost if wrong: a line dropped that would have fitted, or kept when it
     clips.
   - **Amended while building (PR 3's D3, rulings F4 and F5):** the fit
     measures each line with `StaticLayout` instead of the 0.55 em
     estimate, which broke "PAUSED" at 3 × 2. The order becomes the tap
     hint, the title, the icon, then the resume time, which stays in the
     spoken label; the state word is never left out. See spec section 10.
8. **The widget picker shows the label "Beacon" and a one-line
   description.** There is no custom preview image.
   - Cost if wrong: a plain picker entry.
9. **A widget tap that can't read or save the settings does nothing**
   (`IOException` absorbed), and the widget redraws what's saved.
   - Cost if wrong: in a rare disk failure, a tap seems ignored.
10. **Not in PR 3, from PR 2's carry-overs:**
    - Telling the controller when the camera refuses the light. Nothing
      shows a camera line during a real alert, and a test runs with Beacon
      in front.
    - Rewording the non-Xiaomi battery steps. Other phones stay Not tested.
    - Cost if wrong: a heat check every 30 s while another app holds the
      camera, and possibly mismatched wording on stock Android.
11. **The emulator's auto-resume check moves the emulator's clock forward.**
    This is on the emulator only. If that is refused, auto-resume is Not
    tested there.

## Review Focus

1. **A timed pause ends while Beacon isn't running,** or after the phone
   restarted.
   - Expected: alerts resume on time. Home shows ON as soon as it opens,
     and the stored pause is cleaned.
   - Tests: Task 4 `homeShowsOnAfterExpiry`, Task 3
     `catchUpClearsExpiredPause`.
2. **Two quick taps on the widget.**
   - Expected: the first pauses for 1 hour and the second turns alerts ON.
     It never becomes a 2-hour pause.
   - Test: Task 3 `twoTapsPauseThenResume`.
3. **The phone's 12- or 24-hour setting.**
   - Expected: the resume time follows it.
   - Test: Task 2 `resumeTimeFollowsTheClockSetting`.
4. **Pausing from Home while a widget pause is running.**
   - Expected: "until I turn it back on" replaces the timed pause, the alarm
     is cancelled, and the widget shows PAUSED with no time.
   - Test: Task 3 `homePauseCancelsTheAlarm`.
5. **The largest font, or a small widget.**
   - Expected: the state word and the resume time stay. The tap hint goes
     first, then the title, then the state icon. At the default size and
     font scale 1.0, nothing clips.
   - Tests: Task 2 `fit…` and the widget content tests.

## Not in this plan

- Telegram and WhatsApp Business tests, other phones, Google Play:
  spec section 17.
- Everything Task 6 lists as Not tested on the phone.

---

## File structure

```text
gradle/libs.versions.toml, app/build.gradle.kts    + glance-appwidget, glance-appwidget-testing
app/src/main/AndroidManifest.xml                   + widget receiver, ResumeReceiver; WorkManager extras removed
app/src/main/res/xml/beacon_widget_info.xml        the appwidget-provider
app/src/main/res/drawable/widget_on_bg.xml, widget_paused_bg.xml, widget_bolt.xml, widget_pause.xml
app/src/main/res/values/strings.xml                + widget and Home strings
app/src/main/kotlin/dev/beacon/
  BeaconApp.kt                    + pauseCoordinator
  settings/  Pause.kt             + resume rules
             SettingsRepository.kt  + updatePause
  ui/        ResumeTime.kt        formatResumeTime (Home and widget)
             MainActivity.kt      catch-up on resume; rotation keeps a light-only alert
             MainViewModel.kt     pauses through the coordinator; hasLight read late
  ui/home/   PauseClock.kt        pausedNow(): paused by the clock, re-checked at the deadline
             HomeScreen.kt, AlertsCard.kt   the "Back ON by itself" line
  widget/    WidgetState.kt       WidgetState and widgetStateFor(...)
             WidgetFit.kt         fitLines(...): which lines and whether the state icon fit
             WidgetContent.kt     the Glance composable
             BeaconWidget.kt      GlanceAppWidget, its receiver, the tap action, WidgetRefresher
             ResumeAlarm.kt       ResumeAlarm, AndroidResumeAlarm, ResumeReceiver
             PauseCoordinator.kt  every pause change
  alert/     BeaconListener.kt    screen-on catch-up
             AlertController.kt   no Idle between a test and the real alert that replaces it
  ui/blink/  BlinkScreen.kt       regions cached with drawWithCache
app/src/test/kotlin/dev/beacon/…  mirrors the packages above
```

---

### Task 1: Pause rules and the atomic pause update

**Files:**
- Modify: `settings/Pause.kt`, `settings/SettingsRepository.kt`
- Test: `settings/PauseTest.kt` (plain JUnit), `settings/SettingsRepositoryTest.kt` (more tests)

**Interfaces:**
- **Produces, in `Pause.kt`:**
  - `const val WIDGET_PAUSE_MILLIS = 3_600_000L`
  - `fun Pause.resumeAtMillis(): Long?`: the deadline of an `Until`,
    otherwise null
  - `fun Pause.isExpiredAt(nowEpochMillis: Long): Boolean`: an `Until`
    with `now >= epochMillis`
  - `fun pauseAfterWidgetTap(current: Pause, nowEpochMillis: Long): Pause`:
    `None` when `current.isPausedAt(now)`, otherwise
    `Until(now + WIDGET_PAUSE_MILLIS)`. This is spec section 5's table: an
    expired `Until` counts as ON.
- **Produces, in `SettingsRepository`:**
  `suspend fun updatePause(transform: (Pause) -> Pause): Pause`.
  - It reads the stored pause and writes `transform(it)` in one
    `dataStore.edit`, then returns the new pause.
  - `setPause(p)` becomes `updatePause { p }`.

- [ ] **Step 1: Write the failing tests.**

```kotlin
// PauseTest
@Test fun widgetTapWhileOnPausesForAnHour() = assertEquals(Pause.Until(1_000 + 3_600_000), pauseAfterWidgetTap(Pause.None, 1_000))
@Test fun widgetTapWhilePausedTurnsOn() {
    assertEquals(Pause.None, pauseAfterWidgetTap(Pause.Indefinite, 1_000))
    assertEquals(Pause.None, pauseAfterWidgetTap(Pause.Until(5_000), 4_999))
}
@Test fun widgetTapAfterTheDeadlinePausesAgain() = assertEquals(Pause.Until(5_000 + 3_600_000), pauseAfterWidgetTap(Pause.Until(5_000), 5_000))
@Test fun expiryEdge() { assertFalse(Pause.Until(5_000).isExpiredAt(4_999)); assertTrue(Pause.Until(5_000).isExpiredAt(5_000)); assertFalse(Pause.Indefinite.isExpiredAt(Long.MAX_VALUE)) }
@Test fun resumeAt() { assertEquals(5_000L, Pause.Until(5_000).resumeAtMillis()); assertNull(Pause.Indefinite.resumeAtMillis()); assertNull(Pause.None.resumeAtMillis()) }
```

  `SettingsRepositoryTest` additions:
  - **`updatePauseWritesAndReturns`:** `updatePause { Pause.Until(9) }`
    returns `Until(9)`, and `settings.first().pause == Until(9)`.
  - **`updatePauseSeesTheStoredValue`:** after `setPause(Indefinite)`,
    `updatePause { if (it == Pause.Indefinite) Pause.None else it }`
    returns `None`.
  - **`updatePauseFailsOnUnreadableStore`:** with `UnreadableDataStore`, it
    throws `IOException`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.settings.*"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement the rules and `updatePause`.**
  - `updatePause` reads the pause with the same codec as `settings`
    (`SettingsCodec`) and writes it as `setPause` does today. Both keys go
    in the same edit.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add the timed pause rules and an atomic pause update".

---

### Task 2: The widget's look, its fit and the resume time

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `res/values/strings.xml`
- Create: `ui/ResumeTime.kt`, `widget/WidgetState.kt`, `widget/WidgetFit.kt`, `widget/WidgetContent.kt`, and `res/drawable/widget_on_bg.xml`, `widget_paused_bg.xml`, `widget_bolt.xml`, `widget_pause.xml`
- Test: `ui/ResumeTimeTest.kt` (Robolectric), `widget/WidgetFitTest.kt` (plain JUnit), `widget/WidgetStateTest.kt` (plain JUnit), `widget/WidgetContentTest.kt` (Robolectric, `runGlanceAppWidgetUnitTest` with `setContext(ApplicationProvider.getApplicationContext())` so the strings resolve)

**Interfaces:**
- **Produces the resume time:**
  `fun formatResumeTime(context: Context, epochMillis: Long): String`.
  It uses `android.text.format.DateFormat.getTimeFormat(context)`, which
  follows the phone's 12- or 24-hour setting.
- **Produces the widget's state:**
  - `sealed interface WidgetState`, with `data object On`,
    `data class Paused(val resumeAtMillis: Long?)` (null for an open-ended
    pause)
  - `fun widgetStateFor(pause: Pause, nowEpochMillis: Long): WidgetState`:
    `On` when not `isPausedAt(now)`, else `Paused(pause.resumeAtMillis())`
- **Produces the fit:**
  - `enum class WidgetLine { TITLE, STATE, RESUME_TIME, TAP_HINT }`
  - `data class LineSpec(val line: WidgetLine, val text: String, val textSp: Float, val hasIcon: Boolean)`
  - `data class WidgetFit(val lines: Set<WidgetLine>, val stateIcon: Boolean)`
  - `fun fitLines(lines: List<LineSpec>, widthDp: Float, heightDp: Float, spToDp: (Float) -> Float): WidgetFit`
- **Produces the look:**
  `@Composable fun WidgetContent(state: WidgetState, resumeTime: String?, spToDp: (Float) -> Float, modifier: GlanceModifier = GlanceModifier)`.
  It reads `LocalSize.current` for the size. The tap action comes in
  Task 3, which passes `GlanceModifier.clickable(…)` as `modifier`.
- **New strings:**

  | Name | Text |
  |---|---|
  | `widget_tap_pause` | Tap to pause for 1 hour |
  | `widget_tap_resume` | Tap to turn ON now |
  | `widget_back_on_at` | Back ON by itself at %1$s |
  | `widget_label_on` | Visual alerts are ON. Tap to pause for 1 hour. |
  | `widget_label_paused_until` | Visual alerts are PAUSED until %1$s. Tap to turn them back on now. |
  | `widget_label_paused` | Visual alerts are PAUSED. Tap to turn them back on now. |
  | `widget_description` | Shows whether visual alerts are ON. One tap pauses them for 1 hour. |

  The title, "ON" and "PAUSED" reuse `alerts_heading`, `alerts_on` and
  `alerts_paused`.

- **The look** (spec section 10):
  - **ON:** `widget_on_bg` is a solid `#FFD60A` with a 28 dp radius. The
    text is `#0B0B0D`.
  - **PAUSED:** `widget_paused_bg` is a solid `#0B0B0D` with a 4 dp
    `#FFFFFF` stroke and a 28 dp radius. The text is `#FFFFFF`.
  - **Lines, in order:**
    - title, 28 sp bold
    - state: the `widget_bolt` or `widget_pause` icon and "ON" at 64 sp or
      "PAUSED" at 52 sp, bold
    - resume line, 22 sp, timed pause only
    - tap hint, 22 sp
  - The bolt and pause vector drawables copy the path data of
    `BeaconIcons.Bolt` and `BeaconIcons.Pause`. They are tinted to the text
    colour and have no content description.
  - The root is a `Box` filling the widget, with 16 dp padding and the
    content centred.
  - The root's `semantics { contentDescription = … }` is the spoken label
    for the state.
- **The fit algorithm** (ruling 7), which the tests don't determine:

  ```text
  innerW = widthDp - 32;  innerH = heightDp - 32          // 16 dp padding each side
  height(line) = rows * spToDp(textSp) * 1.2
    rows = max(1, ceil(textW / (innerW - iconW)))
    textW = text.length * 0.55 * spToDp(textSp)
    iconW = if (hasIcon) spToDp(textSp) + 8 else 0
  total(set) = sum(height) + 4 * (set.size - 1)
  start with every line and the state icon; while total > innerH, drop in
  this order: TAP_HINT, then TITLE, then the state icon (iconW becomes 0);
  STATE and RESUME_TIME are never dropped, even if they still don't fit
  ```

- [ ] **Step 1: Add the dependencies.**
  - In `libs.versions.toml`, add `glance = "1.2.0"` with
    `glance-appwidget` and `glance-appwidget-testing`.
  - Add `implementation(libs.glance.appwidget)` and
    `testImplementation(libs.glance.appwidget.testing)`.
  - Run `zsh -lc './gradlew :app:assembleDebug'`. Expected: it builds.
    Stop and report if it says compileSdk 37 is needed.
  - Note what the merged manifest gained. Task 3 removes the extras.

- [ ] **Step 2: Write the failing tests.**

```kotlin
// WidgetFitTest — spToDp = { it } is font scale 1.0; { it * 2 } is a linear 2.0
private fun on() = listOf(LineSpec(TITLE, "Visual alerts", 28f, false), LineSpec(STATE, "ON", 64f, true), LineSpec(TAP_HINT, "Tap to pause for 1 hour", 22f, false))
private fun pausedTimed() = listOf(LineSpec(TITLE, "Visual alerts", 28f, false), LineSpec(STATE, "PAUSED", 52f, true),
    LineSpec(RESUME_TIME, "Back ON by itself at 10:30", 22f, false), LineSpec(TAP_HINT, "Tap to turn ON now", 22f, false))
@Test fun fitDefaultSizeShowsEverything() {
    assertEquals(WidgetFit(setOf(TITLE, STATE, TAP_HINT), true), fitLines(on(), 350f, 200f) { it })
    assertEquals(WidgetFit(setOf(TITLE, STATE, RESUME_TIME, TAP_HINT), true), fitLines(pausedTimed(), 350f, 200f) { it })
}
@Test fun fitDropsTheTapHintFirst() = assertEquals(WidgetFit(setOf(TITLE, STATE, RESUME_TIME), true), fitLines(pausedTimed(), 350f, 180f) { it })
@Test fun fitAtLargestFontKeepsOnlyTheState() = assertEquals(WidgetFit(setOf(STATE), true), fitLines(on(), 350f, 200f) { it * 2 })
@Test fun fitDropsTheIconBeforeTheStateWraps() = assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fitLines(pausedTimed(), 232f, 160f) { it })
@Test fun fitNeverDropsStateOrResumeTime() = assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fitLines(pausedTimed(), 250f, 110f) { it })

// WidgetStateTest
@Test fun stateFollowsTheClock() {
    assertEquals(WidgetState.On, widgetStateFor(Pause.None, 0))
    assertEquals(WidgetState.Paused(null), widgetStateFor(Pause.Indefinite, 0))
    assertEquals(WidgetState.Paused(5_000L), widgetStateFor(Pause.Until(5_000), 4_999))
    assertEquals(WidgetState.On, widgetStateFor(Pause.Until(5_000), 5_000))
}
```

  `ResumeTimeTest` uses Robolectric, with the default time zone set to UTC
  in the test:
  - **`resumeTimeFollowsTheClockSetting`:** at 14:30 UTC, with
    `Settings.System.TIME_12_24` = "24", the result is "14:30". With "12",
    it is "2:30 PM".

  `WidgetContentTest` uses `runGlanceAppWidgetUnitTest`, with
  `setAppWidgetSize(DpSize(350.dp, 200.dp))`:
  - **`onShowsItsThreeLines`:** "Visual alerts", "ON", "Tap to pause for
    1 hour". The root's content description is `widget_label_on`.
  - **`timedPauseShowsTheResumeLine`:** "PAUSED" and "Back ON by itself at
    10:30". The label is "Visual alerts are PAUSED until 10:30. Tap to turn
    them back on now."
  - **`openPauseHasNoResumeLine`:** "PAUSED", no "Back ON" text, and the
    label is `widget_label_paused`.
  - **`largestFontKeepsTheStateWord`:** with `spToDp = { it * 2 }`, "ON"
    is shown, and "Tap to pause for 1 hour" and "Visual alerts" are not.

- [ ] **Step 3: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.widget.*" --tests "dev.beacon.ui.ResumeTimeTest"'`
  - Expected: FAIL to compile.

- [ ] **Step 4: Implement the four files and the drawables.**
  - `WidgetContent` builds the `LineSpec`s from the strings, calls
    `fitLines` with `LocalSize.current`, and shows only the returned lines,
    in their order. It shows the state icon only when `stateIcon` is
    true.

- [ ] **Step 5: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS, with 0 warnings.

- [ ] **Step 6: Commit:** "Add the widget's look, its fit and the resume time".

---

### Task 3: The widget, the resume alarm and the pause coordinator

**Files:**
- Create: `widget/PauseCoordinator.kt`, `widget/ResumeAlarm.kt`, `widget/BeaconWidget.kt`, `res/xml/beacon_widget_info.xml`
- Modify: `BeaconApp.kt`, `AndroidManifest.xml`
- Test: `widget/PauseCoordinatorTest.kt` (Robolectric), `widget/ResumeAlarmTest.kt` (Robolectric), `widget/FakeResumeAlarm.kt`

**Interfaces:**
- Consumes: Task 1's `updatePause`, `pauseAfterWidgetTap`,
  `resumeAtMillis` and `isExpiredAt`, and Task 2's `WidgetContent`,
  `widgetStateFor` and `formatResumeTime`.
- **Produces the alarm:**
  - `interface ResumeAlarm { fun schedule(atEpochMillis: Long?) }`: null
    cancels it.
  - `class AndroidResumeAlarm(context: Context) : ResumeAlarm`: calls
    `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, at, pi)`. The pending
    intent is an explicit broadcast to `ResumeReceiver`, with
    `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`, so the alarm is always the
    latest one.
  - `class ResumeReceiver : BroadcastReceiver`: `goAsync()`, then on
    `appScope` it calls `pauseCoordinator.catchUp()` and absorbs
    `IOException`, then `finish()`.
- **Produces the coordinator:**

  ```kotlin
  class PauseCoordinator(
      repo: SettingsRepository,
      alarm: ResumeAlarm,
      refreshWidgets: suspend () -> Unit,
      now: () -> Long = System::currentTimeMillis,
  )
  ```

  - `suspend fun set(pause: Pause)`
  - `suspend fun tapFromWidget(): Pause`, which is
    `updatePause { pauseAfterWidgetTap(it, now()) }`
  - `suspend fun catchUp()`: an expired `Until` becomes `None`
  - All three end the same way: `alarm.schedule(newPause.resumeAtMillis())`,
    then `refreshWidgets()`. The coordinator lets `IOException` through.
- **Produces the widget, in `BeaconWidget.kt`:**
  - `class BeaconWidget : GlanceAppWidget`, with `SizeMode.Exact`.
    - `provideGlance` reads the first saved settings
      (`app.settingsRepository.settings.first()`) before `provideContent`.
      Inside it, `collectAsState(initial = thoseSettings)` follows later
      changes, so the widget never draws a default state first.
    - It shows `WidgetContent(widgetStateFor(pause, now), …)`, with
      `spToDp` from
      `TypedValue.applyDimension(COMPLEX_UNIT_SP, sp, metrics) / metrics.density`.
      That includes Android 14+'s non-linear font scaling.
    - The root modifier is `clickable(actionRunCallback<TogglePauseAction>())`.
  - `class BeaconWidgetReceiver : GlanceAppWidgetReceiver`, whose
    `glanceAppWidget` is `BeaconWidget()`.
  - `class TogglePauseAction : ActionCallback`: calls
    `app.pauseCoordinator.tapFromWidget()` and absorbs `IOException`
    (ruling 9).
  - `object WidgetRefresher { suspend fun refresh(context: Context) }`:
    calls `BeaconWidget().updateAll(context)`.
- **`BeaconApp` adds:**
  `val pauseCoordinator by lazy { PauseCoordinator(settingsRepository, AndroidResumeAlarm(this), refreshWidgets = { WidgetRefresher.refresh(this) }) }`.
- **`beacon_widget_info.xml`:**
  - `minWidth="250dp"`, `minHeight="110dp"`
  - `targetCellWidth="4"`, `targetCellHeight="2"`
  - `minResizeWidth="250dp"`, `minResizeHeight="110dp"` (ruling 6)
  - `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`
  - `updatePeriodMillis="0"`
  - `initialLayout="@layout/glance_default_loading_layout"`
  - `description="@string/widget_description"`
- **Manifest:**
  - `.widget.BeaconWidgetReceiver`: `exported="true"`,
    `label="@string/app_name"`, the `APPWIDGET_UPDATE` intent filter, and
    the `android.appwidget.provider` meta-data.
  - `.widget.ResumeReceiver`: `exported="false"`.
  - The removals in Global Constraints, with
    `xmlns:tools="http://schemas.android.com/tools"` on the root.

- [ ] **Step 1: Write the failing tests.**

  `PauseCoordinatorTest` uses the real repository on a temp file, a
  `FakeResumeAlarm` that records each `schedule` argument, a counter for
  the refreshes, and a settable `now`.

```kotlin
@Test fun widgetTapPausesForAnHourAndArmsTheAlarm() = runTest {
    now = 1_000; assertEquals(Pause.Until(3_601_000), c.tapFromWidget())
    assertEquals(listOf<Long?>(3_601_000), alarm.scheduled); assertEquals(1, refreshes)
}
@Test fun twoTapsPauseThenResume() = runTest {
    c.tapFromWidget(); assertEquals(Pause.None, c.tapFromWidget()); assertEquals(listOf<Long?>(3_601_000, null), alarm.scheduled)
}
@Test fun homePauseCancelsTheAlarm() = runTest {
    c.tapFromWidget(); c.set(Pause.Indefinite)
    assertEquals(Pause.Indefinite, repo.settings.first().pause); assertEquals(null, alarm.scheduled.last())
}
@Test fun catchUpClearsExpiredPause() = runTest {
    repo.setPause(Pause.Until(5_000)); now = 5_000; c.catchUp()
    assertEquals(Pause.None, repo.settings.first().pause); assertEquals(null, alarm.scheduled.last()); assertEquals(1, refreshes)
}
@Test fun catchUpKeepsARunningPauseAndRearms() = runTest {
    repo.setPause(Pause.Until(5_000)); now = 4_000; c.catchUp()
    assertEquals(Pause.Until(5_000), repo.settings.first().pause); assertEquals(5_000L, alarm.scheduled.last())
}
@Test fun saveFailureReachesTheCaller() { /* coordinator on UnreadableDataStore: set() throws IOException, the alarm is untouched */ }
```

  `ResumeAlarmTest` uses Robolectric with `ShadowAlarmManager`:
  - **`schedulesAnInexactWakeup`:** `schedule(5_000)` gives one scheduled
    alarm, of type `RTC_WAKEUP`, at 5_000, allowed while idle, targeting
    `ResumeReceiver`.
  - **`nullCancels`:** `schedule(null)` leaves no scheduled alarm.
  - **`receiverCatchesUp`:** store `Until(past)` in the app's repository,
    send the receiver's intent, and idle. Expected: the stored pause is
    `None`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.widget.*"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement the alarm, the coordinator, the widget, the
  provider XML, the manifest and the wiring.**

- [ ] **Step 4: Run the tests and check the merged manifest.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest :app:assembleDebug'`
  - Expected: PASS.
  - In `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`:
    - the `<uses-permission>` set is exactly the three in Global
      Constraints
    - there's no `SystemForegroundService` and no `INTERNET` permission
    - the widget receiver and `ResumeReceiver` are present

- [ ] **Step 5: Commit:** "Add the widget, the resume alarm and one place for pause changes".

---

### Task 4: Home's timed pause and keeping everything current

**Files:**
- Create: `ui/home/PauseClock.kt`
- Modify: `ui/home/HomeScreen.kt`, `ui/home/AlertsCard.kt`, `ui/MainViewModel.kt`, `ui/MainActivity.kt`, `alert/BeaconListener.kt`, `res/values/strings.xml`
- Test: `ui/home/HomeScreenTest.kt`, `ui/MainViewModelTest.kt`, `alert/BeaconListenerTest.kt` (more tests)

**Interfaces:**
- Consumes: Task 1's `isPausedAt` and `resumeAtMillis`; Task 2's
  `formatResumeTime`; Task 3's `PauseCoordinator` and
  `BeaconApp.pauseCoordinator`.
- **Produces:**
  `@Composable fun pausedNow(pause: Pause, nowEpochMillis: () -> Long = System::currentTimeMillis): Boolean`.
  It returns `pause.isPausedAt(now)`. While the pause is an `Until`, a
  `LaunchedEffect(pause)` waits until the deadline and then re-reads it,
  so the card turns ON at the resume time.
- **Changes `HomeScreen`:**
  - It becomes `HomeScreen(settings, setup, actions, nowEpochMillis: () -> Long = System::currentTimeMillis)`.
  - It uses `pausedNow(settings.pause, nowEpochMillis)` for both the alerts
    card and the apps banner, instead of `pause != None`.
- **Changes `AlertsCard`:** it becomes
  `AlertsCard(paused: Boolean, resumeTime: String?, onPause, onResume)`.
  The PAUSED card shows `alerts_back_on_at` ("Back ON by itself at %1$s.")
  under its body when `resumeTime` isn't null, in the body style. Home
  passes `formatResumeTime(...)` for a running `Until`, and null otherwise.
- **Changes `MainViewModel`:**
  - It becomes `MainViewModel(repo, alerts, checkSetup, pauses: PauseCoordinator)`.
  - `setPause(p)` runs `pauses.set(p)` inside the existing `save { }`,
    which absorbs `IOException`.
  - The Factory passes `app.pauseCoordinator`.
- **Changes `MainActivity.onResume`:** it also launches
  `app.pauseCoordinator.catchUp()` on `app.appScope`, absorbing
  `IOException`.
- **Changes `BeaconListener`:**
  - While connected, it holds a screen-on receiver: registered in
    `onListenerConnected` with
    `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`, and
    unregistered in `onListenerDisconnected` and `onDestroy`.
  - The receiver launches `pauseCoordinator.catchUp()` on `appScope`,
    absorbing `IOException`. This is spec section 6, "refreshes the widget
    when the screen turns on".
- **New string:** `alerts_back_on_at`: "Back ON by itself at %1$s."

- [ ] **Step 1: Write the failing tests.**

  `HomeScreenTest` additions:
  - **`timedPauseShowsTheResumeLine`:** settings with `Until(now + 1 h)`.
    Expected: "PAUSED", and "Back ON by itself at …" with the formatted
    time.
  - **`openPauseHasNoResumeLine`:** `Indefinite`. Expected: "PAUSED" and no
    "Back ON".
  - **`homeShowsOnAfterExpiry`:** `Until(past)`. Expected: the ON card and
    no paused banner.
  - **`cardTurnsOnAtTheDeadline`:**
    - Settings `Until(now + 2_000)`, with a fake clock the test controls.
    - Expected: PAUSED.
    - Then advance the fake clock and `composeRule.mainClock` by 2_000.
      Expected: ON.
  - **`resumeLineNoOverflowAtLargestFont`.**

  `MainViewModelTest`:
  - **`setPauseGoesThroughTheCoordinator`:** with a coordinator on a
    `FakeResumeAlarm`, `setPause(Indefinite)` leaves the alarm's last
    schedule null and counts one refresh.

  `BeaconListenerTest`:
  - **`screenOnCatchesUp`:** store `Until(past)`, connect the listener,
    send `ACTION_SCREEN_ON`, and idle. Expected: the stored pause is
    `None`.
  - **`noScreenOnReceiverAfterDisconnect`:** after
    `onListenerDisconnected()`, the receiver is no longer registered,
    checked with `shadowOf(app).registeredReceivers`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.home.HomeScreenTest" --tests "dev.beacon.ui.MainViewModelTest" --tests "dev.beacon.alert.BeaconListenerTest"'`
  - Expected: FAIL.

- [ ] **Step 3: Implement the clock, the line, the view model and activity
  changes, and the listener's receiver.**

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Show the timed pause on Home and keep the pause current".

---

### Task 5: PR 2's carry-overs

**Files:**
- Modify: `alert/AlertController.kt`, `ui/blink/BlinkScreen.kt`, `ui/MainActivity.kt`, `ui/MainViewModel.kt`
- Test: `alert/AlertControllerTest.kt`, `ui/MainActivityAlertTest.kt`, `alert/AlertActivityTest.kt`, `alert/BeaconListenerTest.kt`, `ui/home/HomeScreenTest.kt`, `ui/blink/OverlayLogicTest.kt`

Each item gets its test first, a RED run, then the fix.

- [ ] **Step 1: No Idle between a test and the real alert that replaces it.**
  - **Test (`AlertControllerTest`):** collect `state` on
    `Dispatchers.Unconfined` into a list of kinds (`Idle` as null). Start a
    test, start a real alert, and idle. Expected: the kinds are exactly
    `[null, TEST, REAL]`.
  - **Fix:** split `stop()` into a private `endCurrent()`, which does
    everything except setting `Idle`. Replacing calls `endCurrent()`, then
    the start sets `Running(REAL)` directly.
- [ ] **Step 2: Work out the blink regions only when the size or the bars
  change.**
  - **Fix:** `BlinkScreen` uses `Modifier.drawWithCache` and calls
    `blinkRegions(...)` in the cache block, read again only when the size
    or insets change. `onDrawBehind` paints the cached regions for the
    current phase.
  - **Test:** the existing BlinkScreen and overlay tests still pass.
- [ ] **Step 3: Rotating keeps a light-only real alert with Beacon in
  front.**
  - **Test (`MainActivityAlertTest`):** start a real alert ("Display over
    other apps" off), launch `MainActivity`, start another real alert,
    then recreate. Expected: still `Running` with the same clock. The
    existing `openingBeaconStopsRealAlert` still passes.
  - **Fix:** a private companion flag records in `onStop` whether
    `isChangingConfigurations` was true. `onStart` skips `stopIf(REAL)`
    once when it was, then clears the flag.
- [ ] **Step 4: Read `hasLight` when the overlay needs it.**
  - **Fix:** `MainViewModel.hasLight` becomes a getter
    (`get() = alerts.hasLight`), so the camera lookup has finished on its
    own thread by the time a test shows.
  - **Test:** the existing `TestAlertOverlayTest` passes.
- [ ] **Step 5: Close PR 2's test gaps.**
  - **`AlertActivityTest.screenOffReceiverGoesWithTheWindow`:** after the
    activity is destroyed, `shadowOf(app).registeredReceivers` has no
    `ACTION_SCREEN_OFF` receiver of its own.
  - **`MainActivityAlertTest.resumeReconnectsADroppedListener`:** with
    access granted and `ListenerConnection.connected = false`, resuming
    `MainActivity` leaves the listener component `ENABLED`.
  - **`BeaconListenerTest.postedNotificationStartsAnAlert`:**
    - Settings are loaded.
    - Build a `StatusBarNotification` for `com.whatsapp` with a
      MessagingStyle message from another person, using the public
      constructor.
    - Call `onNotificationPosted`.
    - Expected: the app's controller is `Running` a real WhatsApp alert.
  - **`HomeScreenTest.setupCardMirrorsInRightToLeft`:** in RTL, the pill's
    bounds are to the left of the row name's.
  - **`HomeScreenTest.setupRowDoesNotReadThePillTwice`:** the merged row
    node has no "ON" or "OFF" text.
  - **`OverlayLogicTest.leftBarStripIsBlack`:** `blinkRegions` with a 48 px
    left inset, which is what rotation 270 gives on 3-button navigation.
    Expected: a point at x = 10 is black in the colour phase.
- [ ] **Step 6: Run the full suite.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS, with 0 warnings.
- [ ] **Step 7: Commit:** "Finish PR 2's carry-overs".

---

### Task 6: Emulator checks, docs and the PR draft

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `docs/specs/2026-11-08-step2-design.md`
- Create: `docs/checks/2026-11-step2-pr3.md`, with screenshots under `docs/checks/step2-pr3/` (scaled to 1200 px)
- The PR draft goes to the session scratchpad as `pr3-body.md`. It is not
  committed and not pushed.

Use only the emulator `beacon-api36` (`-s emulator-5554`), and put back
every setting you change. If a check finds a defect, record it as D1, D2
and so on, with its screenshot, and report it. Don't fix it in this task.

- [ ] **Step 1: Install, and add the widget to the emulator's home screen.**
  Use long-press, then Widgets, then Beacon. Take a screenshot of the
  picker entry, which shows "Beacon", and of the widget in its ON state.
- [ ] **Step 2: Check the tap and pause path.**
  - Tap the widget. Expected: PAUSED with "Back ON by itself at HH:MM", and
    Home shows the same line.
  - Tap again. Expected: ON.
  - "Pause all alerts" on Home gives PAUSED with no time on the widget.
  - Take a screenshot of each.
- [ ] **Step 3: Check auto-resume.**
  - With a timed pause, move the emulator's clock past the resume time
    (`adb root`, then `date`, or the emulator's own settings; ruling 11).
  - Turn the screen off and on. Expected: the widget shows ON, Home shows
    ON, and the stored pause is cleared.
  - Put the clock back.
- [ ] **Step 4: Check the largest font.**
  - At font scale 1.0 and 2.0, take screenshots of the widget at its
    default size and at one larger size, in the ON state and in the timed
    PAUSED state.
  - Expected: at 1.0 nothing clips, and the dropped lines follow ruling 7.
  - At 2.0, record whether the kept lines clip at the default size, and
    record the widget's real size in dp from `dumpsys appwidget`.
- [ ] **Step 5: Check TalkBack.** Expected: the widget reads its spoken
  label from spec section 10, and Home's "Back ON by itself" line is read.
- [ ] **Step 6: Check the carry-overs on the emulator.** Rotating with a
  light-only real alert while Beacon is in front keeps the alert. Use
  overlay access off and an SMS, and check with `dumpsys power` that the
  wake lock is held.
- [ ] **Step 7: Write the checks file.** Its **Not tested on the Redmi 15C
  5G, Android 16** list includes:
  - the widget on HyperOS's launcher, with its real cell sizes
  - auto-resume with the phone asleep
  - an alarm firing late in Doze
  - a restart during a timed pause (ruling 4)
  - TalkBack on the widget
  - everything PR 1's and PR 2's checks files still list
- [ ] **Step 8: Update the docs.**
  - README: Step 2 status, with PR 3 built.
  - CHANGELOG: `[Unreleased]`.
  - Spec:
    - status
    - section 5: the coordinator and the atomic update
    - section 9: the manifest after the removals
    - section 10: the sizes, the fit constants and the picker entry
    - section 13: the restart row (ruling 4)
    - section 16: PR 3 built
- [ ] **Step 9: Commit:** "Record PR 3 checks and update docs".
- [ ] **Step 10: Write the PR draft** at `pr3-body.md`, with these parts:
  - Title, Context/Why, and Summary of Changes
  - this plan's rulings
  - a "### Rulings made while building" heading, which the controller
    fills
  - the emulator checks, Not in this PR, Test Steps, Doc Impact and the
    testing status

  It ends with `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
  Push and open the PR only after the owner's OK.
