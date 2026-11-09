# Step 2, PR 2: the alert engine — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a received SMS, WhatsApp or Telegram message blink the whole
screen and flash the camera light, over other apps and the lock screen, and
add the "Finish setup" card that gets the person there.

**Architecture:**
- `BeaconListener` (a `NotificationListenerService`) turns each notification
  into `NotificationFacts`. The facts hold structure only, never text.
- `MessageWatcher` waits for the saved settings, asks the pure-Kotlin
  `MessageClassifier`, and hands an `Alert` to the `AlertController`.
- The controller now runs real alerts too: a real alert replaces a test, and
  the controller holds a wake lock, watches the phone's heat and opens
  `AlertActivity`. Its Android calls sit behind `AlertPlatform`, so tests use
  a fake.
- `AlertActivity` draws the new `AlertScreen` on the same `BlinkScreen` as
  the test overlay.
- `SetupChecker` feeds a "Finish setup" card at the top of Home.

**Tech Stack:** unchanged from PR 1: Kotlin 2.2.10, Compose BOM 2026.06.01
(Compose 1.11.4), core-ktx 1.18.0, lifecycle 2.10.0, DataStore 1.3.0, JUnit 4
and Robolectric 4.17. No new libraries.

**Spec:** [`docs/specs/2026-11-08-step2-design.md`](../specs/2026-11-08-step2-design.md).
This plan covers spec section 16, PR 2: sections 6, 7, 8 ("The real alert
screen") and 9, and the items PR 1's reviews left for PR 2. Executors read
the spec too.

## Global Constraints

- **Branch:** `feature/step2-alert-engine`, created from
  `feature/step2-screens` (PR 1, yanivil/beacon#6), so PR 2 opens on top of
  PR 1. Never commit to `main`. Each commit message ends with a
  `Co-Authored-By:` line naming the model that wrote it.
- **Running Gradle:** from the repo root, through `zsh -lc '…'`, so
  `JAVA_HOME` (JDK 21) and `ANDROID_HOME` load.
- **SDK and app:** unchanged: minSdk 29, compileSdk 36, targetSdk 36,
  `dev.beacon`, Java and Kotlin target 17.
- **No new libraries.** `NotificationCompat` comes with core 1.18.0, and
  `LifecycleResumeEffect` with lifecycle-runtime-compose 2.10.0.
- **Manifest permissions:** exactly `SYSTEM_ALERT_WINDOW` and `WAKE_LOCK`.
  Never `INTERNET`, `POST_NOTIFICATIONS`, any `FOREGROUND_SERVICE`,
  `USE_FULL_SCREEN_INTENT`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, or the
  SMS, call-log, phone or camera permissions. The listener service is
  protected by `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
- **Privacy:**
  - No code reads a notification's title or text (`EXTRA_TITLE`,
    `EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_SUB_TEXT`), a message's text or a
    person's name.
  - `NotificationFacts` holds only the fields listed in Task 1.
  - The only `Log` call in the app is `Log.d("Beacon", decision.toString())`,
    and it runs only when `BuildConfig.DEBUG` is true.
- **Threads:** `AlertController`, `MessageWatcher` and the listener's
  callbacks all run on the main thread, so the "already handled" record
  needs no locking. The camera light is only ever switched on the
  `beacon-torch` thread.
- **Blink speed:** any code that blinks takes a `BlinkSpeed`, never a
  number. There are never more than 3 flashes per second.
- **Text, layout and TalkBack:** as in PR 1.
  - All user-facing text is in `strings.xml`, and layouts use start and end.
  - Body text is at least 20 sp. Headings are at least 28 sp, bold, and
    marked `heading()`.
  - Touch targets are at least 64 × 64 dp.
  - No text is drawn directly on a flash colour.
  - Every control has its spec section 11 role, state and label.
- **Contrast:** this PR adds no new text colour pair. Every new text uses a
  pair from spec section 11's table.
- **Comments:** a block comment for the "why" of each file's main section.
  KDoc on every class and function, with `@param`, `@return` and `@throws`
  where they apply. Inline comments for edge cases and fallbacks.

## Rulings this plan makes

These are the plan's own decisions where the spec is silent or PR 1's
reviews left a choice. Each says what it costs if it's wrong.

1. **System bars are hidden while a flash fills the window**, in both the
   test overlay and the real alert screen. A swipe from the edge shows them
   for a moment. This settles PR 1's open item: white bar icons on Yellow
   were about 1.4:1.
   - Cost if wrong: the clock and battery icons are hidden during an alert.
   - Replaced while building (D1): the bars stay visible on black strips; see spec section 11 and `docs/checks/2026-11-step2-pr2.md`.
2. **A new camera-light line, "Off while the phone is hot".** The test
   overlay shows it when the phone has a light but the heat check stopped
   it. Before, the overlay would have said "Flashing too".
   - Cost if wrong: one extra string.
3. **The screen-off ending is wired in `AlertActivity`.** It's a screen-off
   receiver, because that screen keeps the display on, so the screen going
   off means the power button. A test ends through Beacon's main screen
   stopping, which also happens when the screen goes off. A light-only real
   alert is not ended by the screen going off, because a screen timeout
   would end it too.
   - Cost if wrong: a light-only alert can't be ended with the power button.
     It ends on its timer or when Beacon is opened.
4. **The wake lock is held 10 seconds longer than each period.** That is
   the alert's length plus 10 seconds, or 60 minutes plus 10 seconds,
   renewed every 60 minutes, for "Until I stop it". The margin makes sure
   the renewal runs before the old lock lapses.
   - Cost if wrong: up to 10 seconds of extra CPU awake time.
5. **Home's `isPausedAt(now)` and the clean-up of an expired timed pause
   move to PR 3.** That is where the timed pause and its Home line arrive.
   PR 2 has no way to create a timed pause. The classifier's pause rule
   does use `isPausedAt(now)` in this PR.
   - Cost if wrong: none in PR 2.
6. **The package `dev.beacon.ui.testalert` becomes `dev.beacon.ui.blink`**,
   because it now holds the real alert screen as well as the test.
   - Cost if wrong: a one-time rename in the history.
7. **A message counts as the owner's own** when the newest message has no
   sender person (the MessagingStyle convention for "the user"), or when
   its sender's key equals the style's user key. Names are never compared.
   - Cost if wrong: an app that leaves incoming senders empty would never
     flash. The phone check for own replies and incoming messages catches
     this.
8. **Beacon open in front with "Display over other apps" off:** only the
   light flashes, as everywhere else without that permission. It ends on
   its timer or when Beacon is opened again. This adds a spec section 13
   row.
   - Cost if wrong: a person looking at Beacon sees no blink screen for
     that message.
9. **The listener reconnect runs at once when Beacon comes to the front,**
   with no delay.
   - Cost if wrong: a needless off-and-on of the listener just after a cold
     start. The phone check covers this.
10. **`Buttons.kt` (226 lines) stays as it is.** No task edits it.
    - Cost if wrong: one file slightly over the 200-line soft limit.
11. **A quick second tap on Stop that lands on the shell is a phone check,
    not code.** The spec sets no rule for it, and the shell's buttons are
    harmless.
    - Cost if wrong: a fix in PR 3.
12. **The emulator checks use the emulator's own SMS** (`adb emu sms send`)
    and grant access with `adb` on the emulator only. The owner's phone is
    never changed over `adb`.

## Not in this plan

- **PR 3:**
  - the widget
  - the timed pause, with Home's "Back ON by itself" line and
    `isPausedAt(now)` (ruling 5)
  - the expired-pause clean-up
  - the listener's widget refresh when the screen turns on (spec
    section 6)
- **Phone checks:** everything Task 8 lists as Not tested.

## Review Focus

These are the inputs and conditions most likely to bite a person, which the
spec implies but no single feature test covers. Each one's test lives in the
task that owns the code.

1. **A message arrives before the saved settings have loaded.** For
   example, the listener started Beacon's process, and DataStore hasn't
   read the file yet.
   - Expected: the decision waits for the saved settings. It never uses
     the defaults, which would flash while the person has paused alerts.
   - Test: Task 5, `waitsForSavedSettings`.
2. **Rotating the phone during a test alert.** Beacon's main screen stops
   and starts again.
   - Expected: the test carries on and doesn't stop.
   - Tests: Task 6, `rotationKeepsTest`, plus PR 1's
     `recreateContinuesCountdown`.
3. **The alert ends before or while `AlertActivity` opens.**
   - Expected: the screen closes at once. A black screen is never left over
     the lock screen.
   - Tests: Task 3, `finishesWhenNothingRuns` and `finishesWhenAlertEnds`.
4. **Rotating the phone during a real alert.**
   - Expected: it keeps blinking on the same clock. It isn't stopped or
     restarted.
   - Test: Task 3, `rotationKeepsAlert`.
5. **A real message during a test.**
   - Expected: the real alert replaces the test, in the message app's own
     colour. Beacon's main screen going behind the alert screen doesn't
     stop the real alert.
   - Tests: Task 4, `realReplacesTest`; Task 5, `perAppColourIsUsed`;
     Task 6, `realAlertSurvivesMainScreenStopping`.

---

## File structure

```text
app/src/main/AndroidManifest.xml          + 2 permissions, BeaconListener, AlertActivity
app/build.gradle.kts                      buildConfig = true (for BuildConfig.DEBUG)
app/src/main/res/values/strings.xml       + alert, setup and camera-line strings
app/src/main/kotlin/dev/beacon/
  BeaconApp.kt                 + app scope, settings state, platform, watcher; light warm-up
  alert/  NotificationFacts.kt     the facts (pure Kotlin)
          NotificationReader.kt    factsOf(…): facts from an Android Notification
          MessageClassifier.kt     Decision, IgnoreReason, HandledRecord, the 8 rules
          MessageWatcher.kt        settings + classifier + controller, main thread
          BeaconListener.kt        the NotificationListenerService, ListenerConnection
          AlertPlatform.kt         interface + AndroidAlertPlatform
          AlertController.kt       real alerts, wake lock, heat, screen, stopIf
          AlertModel.kt            wake-lock constants
          TorchSwitch.kt           lazy camera lookup
          AlertActivity.kt         the real alert window
  setup/  SetupChecker.kt          SetupItem, SetupStatus, SetupChecker
          SetupIntents.kt          the settings pages, with fallbacks
  ui/     MainActivity.kt          stop rules on start/stop, listener reconnect
          MainViewModel.kt         setup state
          BeaconAppScreen.kt       setup refresh, no focus under the overlay
  ui/blink/  (renamed from ui/testalert/)
          BlinkScreen.kt           hides the bars; system-bar and cutout insets
          SystemBars.kt            HideSystemBars()
          OverlayParts.kt          overlayCard, TitleCard, StopButton (shared)
          TestAlertOverlay.kt      shared parts, countdown key(), camera line
          AlertScreen.kt           "New message", the app's name, Stop
          TestColour.kt            moved only
  ui/home/   SetupCard.kt          the "Finish setup" card
             HomeScreen.kt         shows the card
             AppIcon.kt            icon loaded off the main thread
app/src/test/kotlin/dev/beacon/…  mirrors the packages above
```

---

### Task 1: Message facts and rules

**Files:**
- Create: `alert/NotificationFacts.kt`, `alert/NotificationReader.kt`, `alert/MessageClassifier.kt`
- Test: `alert/MessageClassifierTest.kt` (plain JUnit), `alert/NotificationReaderTest.kt` (Robolectric)

**Interfaces:**
- Consumes: `MessageSource` (with `fixedPackages`), `BeaconSettings`
  (`pause`, `apps`), `Pause.isPausedAt(now)`.
- **Produces the facts:**

  ```kotlin
  data class NotificationFacts(
      val packageName: String,
      val key: String,
      val ongoing: Boolean,
      val groupSummary: Boolean,
      val category: String?,
      val hasMessagingStyle: Boolean,
      val newestMessageTime: Long?,   // null without MessagingStyle or messages
      val newestFromOwner: Boolean?,  // null without MessagingStyle or messages
      val whenMillis: Long,
  )
  ```

- **Produces the reader:**
  `fun factsOf(packageName: String, key: String, notification: Notification): NotificationFacts`
- **Produces the rules:**
  - `enum class IgnoreReason { NOT_A_SOURCE, ONGOING, GROUP_SUMMARY, NOT_A_MESSAGE, ALREADY_HANDLED, OWN_MESSAGE, PAUSED, APP_OFF }`
  - `sealed interface Decision`, with `data class Alert(val source: MessageSource)`
    and `data class Ignore(val reason: IgnoreReason)`
  - `class HandledRecord`, with:
    - `fun isNew(key: String, time: Long): Boolean`
    - `fun record(key: String, time: Long)`
    - `fun remove(key: String)`
  - `object MessageClassifier`, with:
    - `fun sourceOf(packageName: String, defaultSmsPackage: String?): MessageSource?`
    - `fun decide(facts: NotificationFacts, settings: BeaconSettings, nowEpochMillis: Long, defaultSmsPackage: String?, seen: HandledRecord): Decision`
    - `fun prime(facts: NotificationFacts, defaultSmsPackage: String?, seen: HandledRecord)`

- [ ] **Step 1: Write the failing classifier tests** in
  `MessageClassifierTest`. They are plain JUnit with no Android, using a
  `facts(…)` helper with these defaults:
  - package `com.whatsapp`, key `k1`
  - not ongoing, not a summary, category `msg`
  - MessagingStyle, newest message time 1_000, not from the owner
  - `when` 1_000

  `SMS_APP` is `"com.google.android.apps.messaging"`, passed as the
  default SMS package.

```kotlin
@Test fun whatsAppMessageAlerts() = assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts()))
@Test fun whatsAppBusinessAlerts() = assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(pkg = "com.whatsapp.w4b")))
@Test fun smsWithCategoryMsgAndNoStyleAlerts() {
    val f = facts(pkg = SMS_APP, style = false, newest = null, own = null, whenMillis = 5_000)
    assertEquals(Decision.Alert(MessageSource.SMS), decide(f))
}
@Test fun telegramIsOffByDefault() = assertEquals(Decision.Ignore(IgnoreReason.APP_OFF), decide(facts(pkg = "org.telegram.messenger.web")))
@Test fun shellAndUnknownAppsAreNotSources() {
    assertEquals(Decision.Ignore(IgnoreReason.NOT_A_SOURCE), decide(facts(pkg = "com.android.shell")))
    assertNull(MessageClassifier.sourceOf(SMS_APP, defaultSmsPackage = null))
}
@Test fun whatsAppBackupIsOngoing() = // the Step 1 pattern: ongoing, no style, no category
    assertEquals(Decision.Ignore(IgnoreReason.ONGOING), decide(facts(ongoing = true, style = false, category = null, newest = null, own = null)))
@Test fun groupSummaryIsSkipped() = assertEquals(Decision.Ignore(IgnoreReason.GROUP_SUMMARY), decide(facts(summary = true)))
@Test fun firstFailingRuleWins() = assertEquals(Decision.Ignore(IgnoreReason.ONGOING), decide(facts(ongoing = true, summary = true)))
@Test fun noStyleAndNoMsgCategoryIsNotAMessage() =
    assertEquals(Decision.Ignore(IgnoreReason.NOT_A_MESSAGE), decide(facts(style = false, category = "progress", newest = null, own = null)))
@Test fun silentChannelMessageAlerts() = // MessagingStyle with no category, like WhatsApp's silent_notifications_6
    assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(category = null)))
@Test fun repeatedUpdateIsHandledOnce() {
    val seen = HandledRecord()
    assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(), seen = seen))
    assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
    assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(newest = 2_000, whenMillis = 2_000), seen = seen))
}
@Test fun noStyleUsesWhenForTheRecord() {
    val seen = HandledRecord(); val f = facts(pkg = SMS_APP, style = false, newest = null, own = null, whenMillis = 7_000)
    decide(f, seen = seen); assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(f, seen = seen))
}
@Test fun ownReplyIsSkippedAndRecorded() {
    val seen = HandledRecord()
    assertEquals(Decision.Ignore(IgnoreReason.OWN_MESSAGE), decide(facts(own = true), seen = seen))
    assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(own = false), seen = seen))
}
@Test fun pauseEdges() {
    assertEquals(Decision.Ignore(IgnoreReason.PAUSED), decide(facts(), settings = paused(Pause.Indefinite)))
    assertEquals(Decision.Ignore(IgnoreReason.PAUSED), decide(facts(), settings = paused(Pause.Until(10_000)), now = 9_999))
    assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(), settings = paused(Pause.Until(10_000)), now = 10_000))
}
@Test fun messageSeenWhilePausedNeverFlashesLater() {
    val seen = HandledRecord()
    decide(facts(), settings = paused(Pause.Indefinite), seen = seen)
    assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
}
@Test fun messageSeenWhileAppOffNeverFlashesLater() { /* APP_OFF with WhatsApp off, then ALREADY_HANDLED with it on */ }
@Test fun primeRecordsOnlyMessages() {
    val seen = HandledRecord()
    MessageClassifier.prime(facts(), SMS_APP, seen)
    MessageClassifier.prime(facts(key = "backup", ongoing = true), SMS_APP, seen)
    assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
    assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(key = "backup"), seen = seen))
}
@Test fun removedKeyIsForgotten() {
    val seen = HandledRecord(); decide(facts(), seen = seen); seen.remove("k1")
    assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(), seen = seen))
}
```

  `decide(…)` defaults: `BeaconSettings.DEFAULT`, `now = 0`, `SMS_APP`, a
  fresh `HandledRecord`. `paused(p)` is `DEFAULT.copy(pause = p)`.

- [ ] **Step 2: Write the failing reader tests** in
  `NotificationReaderTest`, with Robolectric. Each builds a `Notification`
  with `NotificationCompat.Builder(context, "c")`.
  - **`readsMessagingStyle`:**
    - The notification has
      `MessagingStyle(Person.Builder().setName("Me").setKey("me").build())`,
      with one message `"hello"` at 5_000 from
      `Person.Builder().setName("Ann").setKey("ann").build()`, plus
      `setWhen(4_000)` and `setCategory(CATEGORY_MESSAGE)`.
    - Expected: `hasMessagingStyle`, `newestMessageTime == 5_000L`,
      `newestFromOwner == false`, `whenMillis == 4_000L`, category `"msg"`.
  - **`newestMessageDecides`:** add `"ok"` at 6_000 with `null as Person?`.
    Expected: time 6_000, `newestFromOwner == true`.
  - **`ownerByKey`:** the newest message's person has key `"me"`. Expected:
    `newestFromOwner == true`.
  - **`readsFlags`:**
    - `setOngoing(true)` gives `ongoing`.
    - `setGroup("g").setGroupSummary(true)` gives `groupSummary`.
    - A plain notification with no style gives `hasMessagingStyle == false`
      and both newest fields null.
  - **`holdsNoContent`:** `facts.toString()` contains none of `"hello"`,
    `"ok"`, `"Ann"`, `"Me"`.

- [ ] **Step 3: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.alert.MessageClassifierTest" --tests "dev.beacon.alert.NotificationReaderTest"'`
  - Expected: FAIL to compile.

- [ ] **Step 4: Implement the facts, the reader and the classifier.**
  - **`sourceOf`:**
    - A package in a source's `fixedPackages` matches that source.
    - Otherwise, a package equal to `defaultSmsPackage` (when not null) is
      `SMS`.
    - Otherwise null.
  - **`decide`:** applies spec section 6's rules 1 to 8 in order.
    - The time for rule 5 is `newestMessageTime ?: whenMillis`.
    - After rules 1 to 4 pass and the time is new, it calls
      `seen.record(key, time)` before rules 6 to 8. So messages that are
      paused, OFF or the owner's own are recorded too.
    - Rule 7 uses `settings.pause.isPausedAt(nowEpochMillis)`.
    - Rule 8 uses `settings.apps[source]?.enabled ?: source.defaultEnabled`.
  - **`prime`:** applies rules 1 to 4 and records the time. It never
    decides.
  - **`factsOf`:**
    - It uses
      `NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification`.
    - The newest message is the one with the largest timestamp, the last
      one on a tie.
    - The owner rule is ruling 7 above.
    - `ongoing` is `FLAG_ONGOING_EVENT`, and `groupSummary` is
      `FLAG_GROUP_SUMMARY`.
    - If reading the style throws a `RuntimeException` (another app's
      malformed extras), the facts say there is no MessagingStyle. Add an
      inline comment saying so.

- [ ] **Step 5: Run the tests and check that they pass.**
  - Run: the Step 3 command.
  - Expected: PASS.

- [ ] **Step 6: Commit:** "Add the message facts and the classifier's rules".

---

### Task 2: Shared blink parts and the alert screen

**Files:**
- Move: `ui/testalert/*` to `ui/blink/*`, as package `dev.beacon.ui.blink`,
  and the same for the tests (`git mv`, then fix the imports)
- Create: `ui/blink/SystemBars.kt`, `ui/blink/OverlayParts.kt`, `ui/blink/AlertScreen.kt`
- Modify: `ui/blink/BlinkScreen.kt`, `ui/blink/TestAlertOverlay.kt`, `res/values/strings.xml`, and the imports in `ui/BeaconAppScreen.kt` and `ui/MainViewModel.kt`
- Test: `ui/blink/AlertScreenTest.kt`, `ui/blink/OverlayLogicTest.kt`, `ui/blink/TestAlertOverlayTest.kt`, `ui/blink/SystemBarsTest.kt`

**Interfaces:**
- Consumes: `AlertState.Running` (`request`, `clock`, `lightFlashing`),
  `BlinkScreen`, `FilledButton`, `SectionHeading`, `BeaconType`,
  `BeaconColors`.
- **Produces, in `OverlayParts.kt`:**
  - `internal fun Modifier.overlayCard(radius: Dp, horizontal: Dp, vertical: Dp): Modifier`,
    moved from `TestAlertOverlay.kt`
  - `@Composable internal fun TitleCard(label: String, title: String)`:
    `label` is a 28 sp heading, and `title` is a 34 sp heading with a polite
    live region
  - `@Composable internal fun StopButton(onStop: () -> Unit, modifier: Modifier = Modifier)`:
    the PR 1 Stop button (white, 4 dp black border, at least 96 dp tall,
    32 sp)
- **Produces the alert screen:**
  `@Composable fun AlertScreen(running: AlertState.Running, appName: String, onStop: () -> Unit)`
- **Produces the bar control:** `@Composable fun HideSystemBars()`. While it
  is in the composition, the status and navigation bars are hidden with
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, and they show again when it
  leaves. `BlinkScreen` calls it, so both screens hide the bars (ruling 1).
- **Produces the camera line:**
  `internal fun cameraLineRes(hasLight: Boolean, lightFlashing: Boolean): Int`
- **New strings:**
  - `alert_title`: "New message"
  - `test_camera_hot`: "Off while the phone is hot"

- [ ] **Step 1: Move the package.**
  - Run `git mv` on the three source files and four test files.
  - Fix the imports. `BeaconAppScreen.kt` and `MainViewModel.kt` import
    from `ui.blink`.
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS, with 120 tests.
  - Commit: "Rename the test alert package to ui.blink".

- [ ] **Step 2: Write the failing tests.**

```kotlin
// OverlayLogicTest
@Test fun cameraLine() {
    assertEquals(R.string.test_camera_on, cameraLineRes(hasLight = true, lightFlashing = true))
    assertEquals(R.string.test_camera_hot, cameraLineRes(hasLight = true, lightFlashing = false))
    assertEquals(R.string.test_camera_none, cameraLineRes(hasLight = false, lightFlashing = false))
}
```

  `AlertScreenTest` uses Robolectric Compose. Its `running` is a real alert:
  WhatsApp, Yellow, Medium, 60_000 ms.
  - **`showsOnlyTitleNameAndStop`:**
    - "New message" and "WhatsApp messages" are shown, both as headings.
    - "WhatsApp messages" has `LiveRegionMode.Polite`.
    - A Stop button is shown.
    - The only text nodes are those three, so no message content can
      appear.
  - **`stopIsFirstForTalkBack`:**
    - The Stop node's `SemanticsProperties.TraversalIndex` is less than the
      title card's.
    - Their common parent has `IsTraversalGroup == true`.
  - **`stopCallsOnStop`.**
  - **`noOverflowAtLargestFont`:** uses `WithFontScale(2f)` and
    `assertNoTextOverflow()`, in portrait and in landscape
    (`@Config(qualifiers = "land")`).

  `TestAlertOverlayTest` gets two more tests:
  - **`countdownReachesTalkBackEverySecond`:**
    - Capture content-change events from the countdown's live region with
      the helper `AlertSettingsScreenTest` uses for the Custom value (D4).
    - Advance from 3 to 2 to 1.
    - Expected: one subtree-change event at each new second.
  - **`cameraLineSaysHotWhenLightStopped`:** compose `TestAlertOverlay`
    directly, with a `Running` whose `lightFlashing` is false and
    `hasLight = true`. Expected: "Off while the phone is hot" is shown.

  `SystemBarsTest` uses Robolectric with a `ComponentActivity`:
  - **`blinkScreenSetsSwipeBehaviour`:** while `BlinkScreen` is shown,
    `WindowCompat.getInsetsController(window, decorView).systemBarsBehavior`
    is `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`.
  - Whether the bars are really hidden is checked on the emulator in
    Task 8, because Robolectric doesn't report bar visibility.

- [ ] **Step 3: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.blink.*"'`
  - Expected: FAIL to compile.

- [ ] **Step 4: Implement the changes.**
  - **`BlinkScreen`:**
    - It calls `HideSystemBars()`.
    - It pads its content with
      `WindowInsets.systemBars.union(WindowInsets.displayCutout)` instead of
      `safeDrawing`, which also included the keyboard. With the bars hidden,
      only the cutout is left.
  - **`TestAlertOverlay`:**
    - It uses `TitleCard` and `StopButton`.
    - The countdown's text sits inside `key(seconds) { … }` under the merged
      polite live region, so each second is a new text node (the D4
      technique).
    - Its camera row uses `cameraLineRes(hasLight, running.lightFlashing)`.
  - **`AlertScreen`:**
    - `BlinkScreen` with a column that scrolls and is at least the window's
      height, like the overlay.
    - The column holds `TitleCard(label = "New message", title = appName)`
      at the top and `StopButton` at the bottom, with `SpaceBetween`.
    - The column is a traversal group, and Stop has `traversalIndex = -1f`.
    - There is no countdown and no details card.
  - **`HideSystemBars`:**
    - It finds the window from `LocalView`'s context, unwrapping
      `ContextWrapper` to the `Activity`.
    - It calls hide in a `DisposableEffect` and show in `onDispose`.

- [ ] **Step 5: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS.

- [ ] **Step 6: Commit:** "Add the alert screen and hide the bars while a flash shows".

---

### Task 3: The alert window

**Files:**
- Create: `alert/AlertActivity.kt`
- Modify: `AndroidManifest.xml`, `res/values/strings.xml`
- Test: `alert/AlertActivityTest.kt` (Robolectric, `ActivityScenario`)

**Interfaces:**
- Consumes: `BeaconApp.alertController` (`state`, `stop()`), `AlertScreen`,
  `BeaconTheme`, `MessageSource.nameRes`.
- **Produces `class AlertActivity : ComponentActivity`:**
  - `companion object { fun intent(context: Context): Intent }`, with
    `FLAG_ACTIVITY_NEW_TASK`
  - It shows only a `Running` real alert, and closes itself
    (`finishAndRemoveTask()`) whenever no real alert is running.
- **The manifest entry:**
  - `exported="false"`, `excludeFromRecents="true"`
  - `launchMode="singleInstance"`, `taskAffinity=""`
  - `showWhenLocked="true"`, `turnScreenOn="true"`
  - `theme="@style/Theme.Beacon"`
  - No `configChanges`, so rotation recreates it and it redraws from the
    controller's clock.
- **New string:** `alert_window_title`: "New message: %1$s". It is the
  window title, which TalkBack reads when the window opens.

- [ ] **Step 1: Write the failing tests.**
  - Setup: start a real alert on the app's controller before launching:
    Yellow, Medium, 60_000 ms, `MessageSource.WHATSAPP`.
  - After each test, stop the controller.

  The tests:
  - **`showsTheAppNameAndTitle`:**
    - The Compose tree shows "New message" and "WhatsApp messages".
    - `activity.title == "New message: WhatsApp messages"`.
    - The window has `FLAG_KEEP_SCREEN_ON`.
  - **`stopEndsAndCloses`:** tap Stop. Expected: the controller is `Idle`,
    and `activity.isFinishing`.
  - **`backEnds`:** `activity.onBackPressedDispatcher.onBackPressed()`.
    Expected: `Idle`.
  - **`homeEnds`:**
    `InstrumentationRegistry.getInstrumentation().callActivityOnUserLeaving(activity)`.
    Expected: `Idle`.
  - **`screenOffEnds`:**
    `activity.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))`, then idle
    the main looper. Expected: `Idle`.
  - **`finishesWhenNothingRuns`:** with the controller `Idle`, run
    `Robolectric.buildActivity(AlertActivity::class.java).create()`. Use
    this rather than `ActivityScenario`, which waits for a resumed activity.
    Expected: `isFinishing` right after `onCreate`.
  - **`finishesWhenAlertEnds`:** advance the main looper 60 s. Expected:
    `Idle`, and the activity is finishing.
  - **`rotationKeepsAlert`:** `scenario.recreate()`. Expected: still
    `Running`, with the same `clock.startUptimeMillis` as before.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests dev.beacon.alert.AlertActivityTest'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement `AlertActivity` and its manifest entry.**
  - **`onCreate`:**
    - `setShowWhenLocked(true)`, `setTurnScreenOn(true)` and
      `FLAG_KEEP_SCREEN_ON`.
    - `enableEdgeToEdge` with dark transparent bars, as in `MainActivity`.
    - If the state isn't a real `Running`, it finishes and returns.
    - Otherwise it sets the title from `alert_window_title` and the app's
      name.
    - It registers the screen-off receiver with
      `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`, and
      unregisters it in `onDestroy`.
  - **Content:** collects the controller's state with lifecycle. Not a real
    `Running` means `finishAndRemoveTask()`. Otherwise it shows
    `AlertScreen(running, appName) { controller.stop() }`.
  - **Back:** an `OnBackPressedCallback` calls `stop()`.
  - **Home:** `onUserLeaveHint()` calls `stop()`.
  - **Screen off:** the receiver calls `stop()`.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add the alert window over other apps and the lock screen".

---

### Task 4: The full alert controller

**Files:**
- Create: `alert/AlertPlatform.kt`
- Modify: `alert/AlertController.kt`, `alert/AlertModel.kt`, `alert/TorchSwitch.kt`, `BeaconApp.kt`, `AndroidManifest.xml`
- Test: `alert/AlertControllerTest.kt` (more tests), `alert/FakeAlertPlatform.kt`, `alert/AndroidAlertPlatformTest.kt` (Robolectric)

**Interfaces:**
- Consumes: `TorchBlinker`, `BlinkClock`, `AlertActivity.intent(context)`.
- **Produces `interface AlertPlatform`:**
  - `fun holdWakeLock(timeoutMillis: Long)`, which renews it when it is
    already held (the lock isn't reference counted)
  - `fun releaseWakeLock()`
  - `fun isTooHot(): Boolean`, true at `THERMAL_STATUS_SEVERE` or worse
  - `fun watchHeat(onTooHot: () -> Unit)` and `fun stopWatchingHeat()`
  - `fun canShowOverOtherApps(): Boolean`
  - `fun showAlertScreen()`
- **Produces `class AndroidAlertPlatform(context: Context) : AlertPlatform`:**
  - The wake lock is a `PARTIAL_WAKE_LOCK` tagged `"beacon:alert"`.
  - The heat listener runs on the main executor.
  - `showAlertScreen` calls `startActivity(AlertActivity.intent(app))` and
    absorbs `RuntimeException`, so a refused start leaves a light-only
    alert.
- **Produces, in `AlertModel.kt`:** `const val WAKE_LOCK_MARGIN_MILLIS = 10_000L`
  and `const val WAKE_LOCK_PERIOD_MILLIS = 3_600_000L`.
- **Changes `AlertController`:**
  - The constructor becomes
    `AlertController(torch: TorchSwitch, torchLooper: Looper, mainLooper: Looper, platform: AlertPlatform)`.
  - `start(request)`:
    - A real request replaces a running test. Every other start while
      running is dropped and returns false.
    - It holds the wake lock (ruling 4).
    - It starts the light unless `isTooHot()`. `lightFlashing` is false
      when there's no light or the phone is too hot.
    - It watches the heat.
    - For a real request, it calls `showAlertScreen()` only when
      `canShowOverOtherApps()`.
  - At too-hot: it stops the blinker and sets the state to
    `running.copy(lightFlashing = false)`. The screen keeps blinking.
  - `stop()` also releases the wake lock, cancels the renewal and stops
    watching the heat.
  - New: `fun stopIf(kind: AlertKind)`, which stops only an alert of that
    kind.
- **`CameraTorchSwitch`:** the camera lookup is
  `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)`, run on first use of
  `available` or `set`.
- **`BeaconApp`:**
  - It builds the switch in `onCreate` and posts `{ torch.available }` to
    the torch thread, so the lookup runs off the main thread (PR 1
    carry-over).
  - It passes `AndroidAlertPlatform(this)` to the controller.
- **Manifest:** `<uses-permission>` for `android.permission.WAKE_LOCK` and
  `android.permission.SYSTEM_ALERT_WINDOW`.

- [ ] **Step 1: Write the failing tests.**
  - `FakeAlertPlatform(tooHot: Boolean = false, canShow: Boolean = false)`
    records:
    - `wakeLocks: List<Long>`, every `holdWakeLock` timeout in order
    - `released: Boolean`, `watching: Boolean` and `screensShown: Int`
  - `fireTooHot()` calls the watched callback.
  - The existing tests pass a `FakeAlertPlatform()`.
  - `real(length)` is a WhatsApp request in Green at Medium.

```kotlin
@Test fun realReplacesTest() {
    val p = FakeAlertPlatform(canShow = true); val c = controller(FakeTorchSwitch(), p)
    c.start(testRequest()); assertTrue(c.start(real(60_000)))
    assertEquals(AlertKind.REAL, (c.state.value as AlertState.Running).request.kind); assertEquals(1, p.screensShown)
}
@Test fun testNeverReplacesAnAlert() { /* real then test: false; test then test: false (PR 1) */ }
@Test fun realNeverReplacesReal() { /* real then real: false, screensShown == 1 */ }
@Test fun noScreenWithoutOverlayPermission() {
    val p = FakeAlertPlatform(canShow = false); val c = controller(FakeTorchSwitch(), p)
    assertTrue(c.start(real(60_000))); assertEquals(0, p.screensShown); assertTrue(c.state.value is AlertState.Running)
}
@Test fun testNeverOpensTheAlertScreen() { /* canShow = true, test: screensShown == 0 */ }
@Test fun wakeLockCoversLengthPlusMargin() {
    val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p)
    c.start(real(60_000)); assertEquals(listOf(70_000L), p.wakeLocks)
    c.stop(); assertTrue(p.released)
}
@Test fun untilStoppedRenewsEveryHour() {
    val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p)
    c.start(real(null)); assertEquals(listOf(3_610_000L), p.wakeLocks)
    idle(3_600_000); assertEquals(listOf(3_610_000L, 3_610_000L), p.wakeLocks)
    c.stop(); idle(3_600_000); assertEquals(2, p.wakeLocks.size)
}
@Test fun tooHotAtStartMeansNoLight() {
    val fake = FakeTorchSwitch(); val c = controller(fake, FakeAlertPlatform(tooHot = true))
    c.start(real(60_000)); idle(1_000)
    assertFalse((c.state.value as AlertState.Running).lightFlashing); assertTrue(fake.events.isEmpty()); assertTrue(c.hasLight)
}
@Test fun heatStopsLightAndKeepsScreen() {
    val fake = FakeTorchSwitch(); val p = FakeAlertPlatform(); val c = controller(fake, p)
    c.start(real(60_000)); idle(200); p.fireTooHot(); idle(10)
    val r = c.state.value as AlertState.Running
    assertFalse(r.lightFlashing); assertFalse(fake.events.last().second)
    idle(1_000); assertTrue(fake.events.none { it.first > r.clock.startUptimeMillis + 300 && it.second })
}
@Test fun stopIfMatchesOnlyItsKind() { /* test running: stopIf(REAL) keeps it, stopIf(TEST) ends it */ }
@Test fun stopEndsHeatWatch() { /* start, stop: watching == false */ }
```

  `AndroidAlertPlatformTest` uses Robolectric shadows:
  - **`wakeLockHeldAndReleased`:** `ShadowPowerManager.getLatestWakeLock()`
    is held after `holdWakeLock(70_000)`, and not held after
    `releaseWakeLock()`.
  - **`overlayPermissionFollowsSettings`:** uses
    `ShadowSettings.setCanDrawOverlays(true/false)`.
  - **`tooHotFromSevere`:** `ShadowPowerManager.setCurrentThermalStatus`.
    `THERMAL_STATUS_MODERATE` gives false, and `THERMAL_STATUS_SEVERE`
    gives true.
  - **`showsAlertActivityInNewTask`:** `shadowOf(app).nextStartedActivity`
    has `AlertActivity`'s component and `FLAG_ACTIVITY_NEW_TASK`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.alert.*"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement the platform, the controller changes, the lazy
  lookup and the wiring.**
  - Renewal is posted on the main handler with its own token every
    `WAKE_LOCK_PERIOD_MILLIS`, and only while an alert with no end runs.
  - When a real request replaces a test, `stop()` runs first and then the
    normal start. The old blinker's off is queued on the torch thread before
    the new blinker's start.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS. PR 1's overlay tests run on the real
    `AndroidAlertPlatform` through Robolectric.

- [ ] **Step 5: Commit:** "Run real alerts: replace a test, wake lock, heat and the alert window".

---

### Task 5: Listening for messages

**Files:**
- Create: `alert/MessageWatcher.kt`, `alert/BeaconListener.kt`
- Modify: `BeaconApp.kt`, `AndroidManifest.xml`, `app/build.gradle.kts` (`buildFeatures { buildConfig = true }`)
- Test: `alert/MessageWatcherTest.kt`, `alert/BeaconListenerTest.kt` (both Robolectric)

**Interfaces:**
- Consumes: `factsOf`, `MessageClassifier`, `HandledRecord`, `Decision`,
  `AlertController.start`, `BeaconSettings.colourFor` and
  `alertLengthMillis()`.
- **Produces `class MessageWatcher`:**

  ```kotlin
  class MessageWatcher(
      settings: StateFlow<BeaconSettings?>,
      alerts: AlertController,
      scope: CoroutineScope,                 // main thread, Dispatchers.Main.immediate
      defaultSmsPackage: () -> String?,
      nowEpochMillis: () -> Long = System::currentTimeMillis,
      log: (Decision) -> Unit = {},
  )
  ```

  - `fun onConnected(showing: List<NotificationFacts>)`: primes the record.
  - `fun onPosted(facts: NotificationFacts)`: waits for the first non-null
    settings, then decides, logs and on `Alert(source)` starts
    `AlertRequest(REAL, settings.colourFor(source), settings.speed, settings.alertLengthMillis(), source)`.
  - `fun onRemoved(key: String)`.
- **Produces `class BeaconListener : NotificationListenerService`:**
  - It forwards to `app.messageWatcher`. `onNotificationPosted` and the
    priming build facts with
    `factsOf(sbn.packageName, sbn.key, sbn.notification)`, and
    `onNotificationRemoved` passes only `sbn.key`.
  - `onListenerConnected` primes from `activeNotifications`, treating a
    `SecurityException` as an empty list.
  - `onListenerDisconnected` sets the flag and calls `requestRebind`.
- **Produces `object ListenerConnection`:**
  - `@Volatile var connected: Boolean` (set only by `BeaconListener`)
  - `fun reconnectIfDropped(context: Context)`: when access is granted
    (`NotificationManager.isNotificationListenerAccessGranted`) and not
    connected, it sets the listener component to DISABLED and then ENABLED,
    with `DONT_KILL_APP`. This is the Step 1 fix.
- **`BeaconApp` adds:**
  - `val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`
  - `val settingsState: StateFlow<BeaconSettings?>`, which is the
    repository's settings `stateIn(appScope, Eagerly, null)`, lazily
  - `val messageWatcher` (lazy). Its default SMS package is
    `Telephony.Sms.getDefaultSmsPackage(this)`, read on every call. Its log
    is `if (BuildConfig.DEBUG) Log.d("Beacon", it.toString())`.
- **The manifest service:**
  - `.alert.BeaconListener`, `exported="true"`
  - `permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"`
  - an intent filter for
    `android.service.notification.NotificationListenerService`

- [ ] **Step 1: Write the failing tests.**

  `MessageWatcherTest` uses a `MutableStateFlow<BeaconSettings?>`, the
  controller from Task 4 on a `FakeAlertPlatform`, and
  `CoroutineScope(Dispatchers.Main.immediate)`. The SMS app is `SMS_APP`.
  - **`waitsForSavedSettings`:**
    - Settings `null`, then `onPosted(sms)`. Expected: `Idle`.
    - Then emit the defaults with `pause = Pause.Indefinite`. Expected:
      still `Idle`. The saved pause wins, never the defaults.
    - Then a newer SMS on another key with `pause = None`. Expected:
      `Running`.
  - **`alertUsesTheSavedChoices`:** settings with FAST and FIVE_MINUTES.
    Expected: the request is `REAL`, Yellow, FAST, 300_000, `SMS`.
  - **`perAppColourIsUsed`:** `PER_APP` with WhatsApp in Red. A WhatsApp
    message gives the colour RED.
  - **`busyAlertRecordsButDoesNotStack`:**
    - Two keys arrive while the first alert runs. Expected: one alert.
    - After `stop()`, the second key is posted again with the same time.
      Expected: no alert.
  - **`primedNotificationsDontFlash`:** `onConnected(listOf(f))`, then
    `onPosted(f)`. Expected: `Idle`.
  - **`removedThenPostedAgainFlashes`:** `onPosted(f)`, `stop()`,
    `onRemoved(f.key)`, `onPosted(f)`. Expected: `Running`.
  - **`logsEveryDecision`:** the `log` lambda receives
    `Decision.Alert(SMS)` and then `Decision.Ignore(ALREADY_HANDLED)`.

  `BeaconListenerTest` uses Robolectric with
  `Robolectric.buildService(BeaconListener::class.java)`:
  - **`connectFlag`:** `onListenerConnected()` sets
    `ListenerConnection.connected` to true, and `onListenerDisconnected()`
    sets it to false.
  - **`reconnectOnlyWhenGrantedAndDropped`:**
    - Not granted: the component's enabled setting stays `DEFAULT`.
    - Granted
      (`ShadowNotificationManager.setNotificationListenerAccessGranted`) and
      not connected: it ends `ENABLED`.
    - Granted and connected: it stays `DEFAULT`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.alert.MessageWatcherTest" --tests "dev.beacon.alert.BeaconListenerTest"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement the watcher, the listener, the connection helper
  and the wiring.**
  - Facts are built at once in the callback. Only the decision waits for
    the settings.
  - `onNotificationPosted(sbn: StatusBarNotification?)` ignores a null
    `sbn`.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS.
  - Then run `zsh -lc './gradlew :app:assembleDebug'`. Check that the
    merged manifest has `BIND_NOTIFICATION_LISTENER_SERVICE` on the service
    and still no `INTERNET`.

- [ ] **Step 5: Commit:** "Listen for messages and start real alerts".

---

### Task 6: Beacon's main screen and a running alert

**Files:**
- Modify: `ui/MainActivity.kt`, `ui/BeaconAppScreen.kt`
- Test: `ui/MainActivityAlertTest.kt` (Robolectric), `ui/AppShellTest.kt` (one more test)

**Interfaces:**
- Consumes: `AlertController.stopIf`, `ListenerConnection.reconnectIfDropped`.
- **`MainActivity`:**
  - `onStart` calls `stopIf(AlertKind.REAL)`. Opening Beacon ends a real
    alert, which is how a light-only alert is stopped.
  - `onResume` calls `ListenerConnection.reconnectIfDropped(this)`.
  - `onStop` calls `stopIf(AlertKind.TEST)`, but only when
    `!isChangingConfigurations`.
- **The shell:** while the test overlay shows, keyboard focus can't enter
  the shell under it. Use
  `Modifier.focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()`
  on the shell, next to its cleared semantics.

- [ ] **Step 1: Write the failing tests.** All use `ActivityScenario` and
  the app's controller.
  - **`leavingStopsTest`:** start a test, then
    `scenario.moveToState(Lifecycle.State.CREATED)`. Expected: `Idle`.
  - **`rotationKeepsTest`:** start a test, then `scenario.recreate()`.
    Expected: `Running`, with the same `clock.startUptimeMillis`.
  - **`openingBeaconStopsRealAlert`:** start a real alert with "Display
    over other apps" off, then launch `MainActivity`. Expected: `Idle`.
  - **`realAlertSurvivesMainScreenStopping`:** launch, start a test, start
    a real alert (it replaces the test), then `moveToState(CREATED)`.
    Expected: still a real `Running`.
  - **`keyboardFocusStaysOnTheOverlay`** (in `AppShellTest`):
    - Start a test, then `requestFocus()` on Stop.
    - Press Tab 10 times with `performKeyInput { pressKey(Key.Tab) }`.
    - Expected: no focused node is inside `SCROLL_CONTENT_TAG` or the tab
      bar.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.MainActivityAlertTest" --tests "dev.beacon.ui.AppShellTest"'`
  - Expected: FAIL.

- [ ] **Step 3: Implement the lifecycle rules and the focus block.**

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS.

- [ ] **Step 5: Commit:** "End alerts from Beacon's main screen and keep focus on the overlay".

---

### Task 7: The "Finish setup" card

**Files:**
- Create: `setup/SetupChecker.kt`, `setup/SetupIntents.kt`, `ui/home/SetupCard.kt`
- Modify: `ui/home/HomeScreen.kt`, `ui/home/AppIcon.kt`, `ui/MainViewModel.kt`, `ui/BeaconAppScreen.kt`, `res/values/strings.xml`
- Test: `setup/SetupCheckerTest.kt`, `setup/SetupIntentsTest.kt`, `ui/home/HomeScreenTest.kt` (more tests), `ui/MainViewModelTest.kt` (one more test)

**Interfaces:**
- **Produces, in `SetupChecker.kt`:**
  - `enum class SetupItem { NOTIFICATIONS, OVERLAY, BATTERY }`
  - `data class SetupStatus(val notificationAccess: Boolean, val displayOverApps: Boolean, val batteryUnrestricted: Boolean)`,
    with `val complete: Boolean` and `fun isOn(item: SetupItem): Boolean`
  - `class SetupChecker(context: Context) { fun check(): SetupStatus }`. It
    uses `NotificationManagerCompat.getEnabledListenerPackages`,
    `Settings.canDrawOverlays` and
    `PowerManager.isIgnoringBatteryOptimizations`.
- **Produces `object SetupIntents`:**
  - `fun intentFor(item: SetupItem, context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Intent`
  - `fun open(item: SetupItem, context: Context)`. If the page is missing,
    it falls back to App info, and if that is missing too, it does nothing.
  - `fun isXiaomi(manufacturer: String = Build.MANUFACTURER): Boolean`,
    which ignores case
- **Produces the card:**
  `@Composable fun SetupCard(status: SetupStatus, xiaomi: Boolean, onOpen: (SetupItem) -> Unit)`
- **Changes `HomeScreen`:**
  - It becomes `HomeScreen(settings: BeaconSettings, setup: SetupStatus?, actions: HomeActions)`.
  - `HomeActions` gains `openSetup: (SetupItem) -> Unit`.
  - The card shows between the title and the alerts card when
    `setup != null && !setup.complete`.
- **Changes `MainViewModel`:**
  - It becomes `MainViewModel(repo, alerts, checkSetup: () -> SetupStatus)`.
  - It gains `val setup: StateFlow<SetupStatus?>` (starting null) and
    `fun refreshSetup()`.
- **The shell:** Home's content has
  `LifecycleResumeEffect(Unit) { viewModel.refreshSetup(); onPauseOrDispose {} }`.
  So the checks run each time Home comes into view, including after a
  return from Settings. `openSetup` calls
  `SetupIntents.open(item, context)`.
- **`AppIcon`** (PR 1 carry-over): it loads the icon with
  `produceState(null, source, px) { value = withContext(Dispatchers.IO) { … } }`.
  It shows the glyph until the icon is ready, and keeps the glyph if there
  is no icon.
- **New strings:**

  | Name | Text |
  |---|---|
  | `setup_heading` | Finish setup |
  | `setup_hint` | Beacon needs these to flash for your messages. |
  | `setup_notifications` | Notification access |
  | `setup_notifications_reason` | So Beacon knows when a message arrives. |
  | `setup_overlay` | Display over other apps |
  | `setup_overlay_reason` | So the screen can flash over other apps and the lock screen. |
  | `setup_battery` | Battery: No restrictions |
  | `setup_battery_reason` | So alerts still arrive while the phone sleeps. |
  | `setup_battery_steps_xiaomi` | Tap Battery saver, then No restrictions. |
  | `setup_battery_steps_other` | Tap Battery, then Unrestricted. |
  | `setup_open` | Open settings |
  | `setup_open_for` | Open settings for %1$s |
  | `state_on` | On |
  | `state_off` | Off |

- **The card's look:**
  - `BeaconColors.Card` (`#1B1B20`) with a 4 dp white border and a 24 dp
    radius, like the PAUSED card.
  - The heading is a `SectionHeading`, then the hint line in body text.
  - **Each row:**
    - The name at 24 sp, weight 800.
    - The reason in body text.
    - For the battery row, the steps line from `isXiaomi()`.
    - An `OnOffPill`, which wraps under the text like the app rows.
    - An `OutlineButton` "Open settings" in white, at least 64 dp tall and
      full width.
  - **TalkBack:**
    - The name, reason and pill are one merged item, with
      `stateDescription` "On" or "Off". The pill's own text isn't read a
      second time.
    - The button's spoken label is "Open settings for <name>".
  - Every row shows, ON or OFF, while the card is shown.

- [ ] **Step 1: Write the failing tests.**

  `SetupCheckerTest` uses Robolectric:
  - **`allOffAtFirst`:** the status is all false.
  - **`readsEachGrant`:**
    - Put `"dev.beacon/dev.beacon.alert.BeaconListener"` into
      `Settings.Secure` `"enabled_notification_listeners"`.
    - Call `ShadowSettings.setCanDrawOverlays(true)`.
    - Call `ShadowPowerManager.setIgnoringBatteryOptimizations("dev.beacon", true)`.
    - Expected: each grant turns on its own field only, and all three give
      `complete`.

  `SetupIntentsTest`:
  - **`notificationsOnAndroid11Plus`:**
    - With `sdkInt = 30`: the action is
      `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS`, with
      `EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME` set to
      `"dev.beacon/dev.beacon.alert.BeaconListener"`.
    - With `sdkInt = 29`: `ACTION_NOTIFICATION_LISTENER_SETTINGS`.
  - **`overlayPage`:** `ACTION_MANAGE_OVERLAY_PERMISSION`, with data
    `package:dev.beacon`.
  - **`batteryPage`:** `ACTION_APPLICATION_DETAILS_SETTINGS`, with data
    `package:dev.beacon`.
  - **`xiaomiNames`:** `isXiaomi("Xiaomi")` and `isXiaomi("xiaomi")` are
    true, and `isXiaomi("Google")` is false.
  - **`missingPageFallsBackToAppInfo`:**
    - Call `shadowOf(app).checkActivities(true)`.
    - Make only App info resolvable with
      `shadowOf(packageManager).addResolveInfoForIntent`.
    - Expected: `open(OVERLAY)` starts App info.

  `HomeScreenTest` additions:
  - **`setupCardHiddenWhenComplete`.**
  - **`setupCardShowsAllRowsWhenOneIsOff`:**
    - The heading "Finish setup" is a heading.
    - There are three rows.
    - The notification row's merged node has `stateDescription` "Off".
  - **`openSettingsSpeaksTheRow`:** a node with content description "Open
    settings for Display over other apps" calls `openSetup(OVERLAY)`.
  - **`batteryStepsFollowTheMaker`:** the Xiaomi steps when `xiaomi = true`,
    and the other steps when false.
  - **`setupCardNoOverflowAtLargestFont`.**
  - **`setupCardIsAboveTheAlertsCard`:** compare the bounds' tops.
  - The existing Home tests pass `setup = null` and a no-op `openSetup`.
  - Tests that expect a launcher icon wait for it with
    `composeRule.waitUntil { … }`.

  `MainViewModelTest`:
  - **`refreshSetupPublishesStatus`:** `setup` starts null, and after
    `refreshSetup()` it equals the fake's status.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.setup.*" --tests "dev.beacon.ui.home.HomeScreenTest" --tests "dev.beacon.ui.MainViewModelTest"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement the checker, the intents, the card, the Home and
  view-model changes, the resume refresh and the icon loading.**

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add the Finish setup card and load app icons off the main thread".

---

### Task 8: Emulator checks, docs and the PR draft

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `docs/specs/2026-11-08-step2-design.md`
- Create: `docs/checks/2026-11-step2-pr2.md`, with screenshots under `docs/checks/step2-pr2/` (scaled to 1200 px)

- [ ] **Step 1: Install and open Beacon on the emulator** `beacon-api36`
  (`-s emulator-5554`). Expected: Home shows the "Finish setup" card with
  all three rows OFF. Take a screenshot.

- [ ] **Step 2: Check the setup card.**
  - Tap each "Open settings" button and take a screenshot of the page it
    opens.
  - Grant each item on that page with taps.
  - If the emulator's page can't be driven, use these commands on the
    emulator only:
    - `adb -s emulator-5554 shell cmd notification allow_listener dev.beacon/dev.beacon.alert.BeaconListener`
    - `adb -s emulator-5554 shell appops set dev.beacon SYSTEM_ALERT_WINDOW allow`
    - `adb -s emulator-5554 shell dumpsys deviceidle whitelist +dev.beacon`
  - Expected: each row turns ON on returning to Home, and the card goes
    once all three are ON.

- [ ] **Step 3: Check a real SMS alert from end to end.**
  - Run: `adb -s emulator-5554 emu sms send 5551234 "Beacon test"`
  - Check it three ways, with a screenshot of each:
    1. Over another app (the launcher).
    2. With the screen off and locked (`input keyevent KEYCODE_SLEEP`
       first).
    3. With Beacon open on Alert settings and a test running.
  - Expected:
    - The alert screen shows "New message" and "Texts (SMS)".
    - It blinks the chosen colour, with the system bars hidden.
    - On the lock screen, the screen wakes and the phone stays locked
      behind the alert.

- [ ] **Step 4: Check each ending:**
  - Stop, Back, Home and the power button (`KEYCODE_POWER`)
  - the timer (1 min)
  - Expected: each ends the alert and leaves no Beacon window in Recents.
  - **Light-only alert:** revoke overlay access
    (`appops set dev.beacon SYSTEM_ALERT_WINDOW deny`) and send an SMS.
    Expected:
    - No alert screen opens.
    - `adb -s emulator-5554 shell dumpsys power | grep beacon:alert` shows
      the wake lock held while the alert runs.
    - Opening Beacon removes the wake lock.

- [ ] **Step 5: Check the false-alert cases.**
  - Send two SMS 5 seconds apart. Expected: the second arrives during the
    first alert and doesn't start another, and no alert comes back for it
    after Stop.
  - Send an SMS while alerts are paused. Expected: no alert, and none after
    turning alerts back ON.

- [ ] **Step 6: Check fonts and TalkBack.**
  - At the largest font: the setup card, the alert screen (portrait and
    landscape) and the test overlay. Expected: nothing is clipped.
  - With TalkBack:
    - The setup rows read their name and "On" or "Off".
    - "Open settings for …" names its row.
    - The alert screen focuses Stop first and announces the window title.
    - The countdown reads each second.

- [ ] **Step 7: Write the checks file.** It lists what is **Not tested on
  the Redmi 15C 5G, Android 16**, until the phone is connected:
  - WhatsApp detection
  - own replies typed into a notification
  - WhatsApp's backup pattern
  - alerts with the phone locked, asleep and on battery (a real cable-out
    test, per Step 1)
  - HyperOS's listener reconnect
  - the camera light and the heat stop
  - TalkBack and the largest font on the phone
  - a quick second tap on Stop (ruling 11)

  Telegram and WhatsApp Business stay Not tested.

- [ ] **Step 8: Update the docs.**
  - README: Step 2 status and what PR 2 delivers.
  - CHANGELOG: entries under `[Unreleased]`.
  - Spec:
    - Status: PR 2 built.
    - Section 7: a real alert replaces a test; the wake-lock margin
      (ruling 4); the screen-off ending (ruling 3); `lightFlashing` after
      the heat stop.
    - Section 8: the camera line's third value (ruling 2).
    - Section 11: the system bars are hidden while a flash fills the window
      (ruling 1), replacing "decided for real alerts with PR 2".
    - Section 13: the row for ruling 8.
    - Section 16: PR 2 built, with a link to its checks.

- [ ] **Step 9: Request a whole-branch review** with
  superpowers:requesting-code-review, and fix what it confirms.

- [ ] **Step 10: Commit:** "Record PR 2 checks and update docs".

- [ ] **Step 11: Write the PR draft** for the owner: Title, Context/Why,
  Summary of Changes (with this plan's rulings and any spec section 3
  conflicts), Test Steps and Doc Impact. Push and open the PR only after the
  owner's OK, using the keychain-token `gh` command from `CLAUDE.md`.
