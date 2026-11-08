# Step 2, PR 1: screens and test alert — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Beacon app project with its theme, saved settings, Home,
Alert settings and the 3-second Test alert, which blinks the screen and the
camera light.

**Architecture:**
- One Gradle module, `app`, with packages by job: `settings`, `alert`, `ui`.
- Settings live in DataStore behind `SettingsRepository`.
- `AlertController` owns the alert's timing through `BlinkClock`. The screen
  (`BlinkScreen`) and the light (`TorchBlinker`) both draw from that clock.
- The UI is Compose Foundation with Beacon's own components, driven by one
  `MainViewModel`.

**Tech Stack:**
- Kotlin 2.2.10 (AGP 9.5.1's built-in Kotlin)
- Jetpack Compose (BOM 2026.09.00)
- DataStore Preferences 1.3.0
- JUnit 4 and Robolectric 4.17

**Spec:** [`docs/specs/2026-11-08-step2-design.md`](../specs/2026-11-08-step2-design.md).
This plan covers spec section 16, PR 1. Executors read the spec too.

## Global Constraints

- **Branch:** `feature/step2-screens`, created from `main` after PR 0 (the
  spec and this plan) has merged. Never commit to `main`. Each commit
  message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Running Gradle:** from the repo root, through `zsh -lc '…'`, so
  `JAVA_HOME` (JDK 21) and `ANDROID_HOME` load.
- **SDK and app:** minSdk 29, compileSdk 36, targetSdk 36.
  applicationId and namespace are `dev.beacon`. versionCode is 1 and
  versionName is `0.1.0`. Java and Kotlin target 17.
- **Exact versions:**
  - Gradle wrapper 9.8.2, AGP 9.5.1, Compose compiler plugin
    `org.jetbrains.kotlin.plugin.compose` 2.2.10
  - Compose BOM 2026.09.00 (foundation, ui, material-ripple, ui-test-junit4
    and ui-test-manifest 1.12.1)
  - activity-compose 1.13.0, lifecycle-viewmodel-compose and
    lifecycle-runtime-compose 2.11.0
  - datastore-preferences 1.3.0, core-ktx 1.19.1
  - junit 4.13.2, robolectric 4.17, androidx.test core 1.7.0,
    androidx.test.ext junit 1.3.0, kotlinx-coroutines-test 1.11.0
- **No Material 3, and no Material dynamic colour.** The only Material
  artifact allowed is `material-ripple`, for press feedback.
- **The manifest:**
  - It asks for no `INTERNET` permission, and the merged manifest must not
    contain it.
  - It sets `allowBackup="false"`, with data extraction rules that exclude
    everything.
  - It sets `supportsRtl="true"`.
- **Text and layout:**
  - All user-facing text is in `res/values/strings.xml`. Kotlin code holds
    no visible text.
  - Layouts use start/end only. Text sizes are in sp.
  - Body text is at least 20 sp. Headings are at least 28 sp, bold, and
    marked `heading()` in semantics.
  - Touch targets are at least 64 × 64 dp.
- **Blink speed:** any code that blinks takes a `BlinkSpeed`, never a
  number. There are never more than 3 flashes per second.
- **Comments:** a block comment for the "why" of each file's main section.
  KDoc on every class and function, with `@param`, `@return` and `@throws`
  where they apply. Inline comments for edge cases and fallbacks.
- **Privacy:** no `Log` calls in this PR.
- **Units:** 1 design px = 1 dp for sizes and 1 sp for text.

## Review Focus

These are the inputs and conditions most likely to bite a person using the
app, which the spec implies but no feature test exercises on its own. Each
one's test lives in the task that owns the code.

1. **Largest font size (font scale 2.0).**
   - What breaks: the 100 × 60 dp ON/OFF pill, the option grids, the fixed
     bottom area and the countdown can clip text.
   - Expected: nothing clips. The pill grows, the grids drop to 1 column,
     and the Test alert button moves into the scrolling content.
   - Tests: Tasks 7, 8, 9, 10 and 11.
2. **Right-to-left layout.**
   - Expected: the app rows, pills and the "Change settings" chevron mirror.
   - Tests: Tasks 7 and 9.
3. **Rotating or recreating the screen during a test alert.**
   - Expected: the countdown carries on from the controller's clock and
     doesn't restart at 3.
   - Test: Task 11.
4. **Tapping Test alert twice quickly.**
   - Expected: one test runs, with one light session.
   - Tests: Tasks 6 and 11.
5. **A phone with no camera light (the emulator, and Robolectric).**
   - Expected: the test still blinks the screen, and its details say
     "Not on this phone".
   - Tests: Tasks 5, 6 and 11.

---

## File structure

```text
settings.gradle.kts, build.gradle.kts, gradle.properties     root build
gradle/libs.versions.toml                                    version catalog
gradle/wrapper/*, gradlew, gradlew.bat                       copied from spikes/feasibility
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml                          all text (Task 1)
app/src/main/res/values/themes.xml                           platform theme, dark window
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/res/font/atkinson_hyperlegible_next.ttf         variable font, OFL
app/src/main/assets/licenses/atkinson_hyperlegible_next_OFL.txt
app/src/main/kotlin/dev/beacon/
  BeaconApp.kt                       Application: repository, controller, torch thread
  settings/  BlinkSpeed.kt  FlashColour.kt  Duration.kt  Pause.kt
             BeaconSettings.kt  SettingsCodec.kt  SettingsRepository.kt
  alert/     MessageSource.kt  BlinkClock.kt  TorchSwitch.kt  TorchBlinker.kt
             AlertModel.kt  AlertController.kt
  ui/        MainActivity.kt  MainViewModel.kt  BeaconAppScreen.kt  BottomArea.kt  Summaries.kt
  ui/theme/  BeaconColors.kt  Contrast.kt  BeaconType.kt  BeaconTheme.kt
  ui/components/  BeaconIcons.kt  Buttons.kt  OptionButton.kt  OnOffPill.kt
                  AppSwitchRow.kt  SwatchGrid.kt  AdaptiveGrid.kt  Headings.kt
  ui/home/   HomeScreen.kt  AlertsCard.kt  AppsSection.kt  SummaryCard.kt  AppIcon.kt
  ui/settings/  AlertSettingsScreen.kt  DurationSection.kt  SpeedSection.kt  ColourSection.kt
  ui/testalert/  TestAlertOverlay.kt  BlinkScreen.kt  TestColour.kt
app/src/test/kotlin/dev/beacon/…     mirrors the packages above
app/src/test/kotlin/dev/beacon/ui/UiTestSupport.kt   shared Compose test helpers
```

---

### Task 1: Project scaffold

**Files:**
- Create: root `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`
- Create: the wrapper files, copied from `spikes/feasibility` (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`)
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `res/values/strings.xml`, `res/values/themes.xml`, `res/xml/data_extraction_rules.xml`
- Create: `dev/beacon/BeaconApp.kt`, `dev/beacon/ui/MainActivity.kt` (empty content for now)
- Test: `app/src/test/kotlin/dev/beacon/ui/MainActivitySmokeTest.kt`

**Interfaces:**
- Produces: `class BeaconApp : Application`. Later tasks add properties to it.
- Produces: `class MainActivity : ComponentActivity`.
- Produces: string resource names (below). Every later task uses these
  names.

- [ ] **Step 1: Write the build files.**
  - `settings.gradle.kts`: same repositories as the spike, `rootProject.name = "beacon"`, and `include(":app")`.
  - `gradle.properties`: `android.useAndroidX=true` and `org.gradle.jvmargs=-Xmx4g`.
  - `app/build.gradle.kts`:
    - Plugins: `com.android.application` and `org.jetbrains.kotlin.plugin.compose`.
    - `buildFeatures { compose = true }`.
    - `testOptions.unitTests.isIncludeAndroidResources = true`.
    - The dependencies from Global Constraints, through the catalog.

- [ ] **Step 2: Write the manifest.**
  - `<application>`:
    - `android:name=".BeaconApp"`, `android:label="@string/app_name"`.
    - `allowBackup="false"`, `dataExtractionRules="@xml/data_extraction_rules"`.
    - `supportsRtl="true"`, `theme="@style/Theme.Beacon"`.
  - `MainActivity`: exported, with the launcher intent filter.
  - `<queries>`, so Task 9 can load app icons:
    - `<package>` for `com.whatsapp`, `com.whatsapp.w4b`,
      `org.telegram.messenger` and `org.telegram.messenger.web`.
    - An `<intent>` with action `android.intent.action.SENDTO` and data
      scheme `smsto`.
  - `data_extraction_rules.xml` excludes every domain for both cloud
    backup and device transfer.
  - `Theme.Beacon`:
    - Parent `android:Theme.Material.NoActionBar`.
    - `android:windowBackground` `#0B0B0D`.
    - Transparent status and navigation bars.

- [ ] **Step 3: Write `strings.xml` with exactly this copy.** Later tasks
  only reference these names. PR 2 and PR 3 add their own strings.

```xml
<string name="app_name">Beacon</string>
<string name="home_title">Beacon</string>
<string name="alerts_heading">Visual alerts</string>
<string name="alerts_on">ON</string>
<string name="alerts_paused">PAUSED</string>
<string name="alerts_on_body">Your screen and camera light will flash when a message arrives.</string>
<string name="alerts_paused_body">You will NOT see any flashes until you turn alerts back on.</string>
<string name="alerts_pause_button">Pause all alerts</string>
<string name="alerts_resume_button">Turn alerts back ON</string>
<string name="apps_heading">Flash for these apps</string>
<string name="apps_hint">Tap an app to turn it ON or OFF.</string>
<string name="apps_paused_banner">All alerts are paused right now.</string>
<string name="app_sms">Texts (SMS)</string>
<string name="app_whatsapp">WhatsApp messages</string>
<string name="app_telegram">Telegram</string>
<string name="app_flash_line">%1$s flash</string>
<string name="app_no_flash">No flash</string>
<string name="switch_on">ON</string>
<string name="switch_off">OFF</string>
<string name="summary_heading">How alerts look</string>
<string name="summary_flashes_for">Flashes for</string>
<string name="summary_blink_speed">Blink speed</string>
<string name="summary_screen_color">Screen color</string>
<string name="summary_per_app">Different per app</string>
<string name="summary_change">Change settings</string>
<string name="settings_title">Alert settings</string>
<string name="duration_heading">How long to flash</string>
<string name="duration_hint">After a message arrives.</string>
<string name="duration_one_minute">1 min</string>
<string name="duration_five_minutes">5 min</string>
<string name="duration_custom">Custom</string>
<string name="duration_until_stopped">Until I stop it</string>
<string name="minutes_short">%1$d min</string>
<plurals name="duration_minutes">
    <item quantity="one">%1$d minute</item>
    <item quantity="other">%1$d minutes</item>
</plurals>
<string name="custom_heading">Custom length</string>
<string name="custom_shorter">Shorter</string>
<string name="custom_longer">Longer</string>
<string name="custom_range">From 1 to 60 minutes.</string>
<string name="speed_heading">Blink speed</string>
<string name="speed_hint">All three speeds are safe limits that protect your camera light and battery.</string>
<string name="speed_slow">Slow</string>
<string name="speed_medium">Medium</string>
<string name="speed_fast">Fast</string>
<string name="speed_rate">%1$d blinks a minute</string>
<string name="speed_fast_note">Highest safe speed</string>
<string name="speed_summary">%1$s, %2$d a minute</string>
<string name="colour_heading">Screen flash color</string>
<string name="colour_hint">Your whole screen blinks this color. Each color has its name written on it.</string>
<string name="colour_mode_same">Same color for all apps</string>
<string name="colour_mode_per_app">Different color for each app</string>
<string name="colour_red">Red</string>
<string name="colour_yellow">Yellow</string>
<string name="colour_blue">Blue</string>
<string name="colour_green">Green</string>
<string name="colour_white">White</string>
<string name="colour_change">Change</string>
<string name="colour_done">Done</string>
<string name="colour_change_for">Change color for %1$s</string>
<string name="colour_done_for">Done choosing color for %1$s</string>
<string name="state_expanded">Expanded</string>
<string name="state_collapsed">Collapsed</string>
<string name="test_button">Test alert</string>
<string name="test_button_length">(3 sec)</string>
<string name="tab_home">Home</string>
<string name="tab_settings">Alert settings</string>
<string name="test_label">Test alert</string>
<string name="test_title">This is how your alerts will look</string>
<plurals name="test_seconds_left">
    <item quantity="one">second left</item>
    <item quantity="other">seconds left</item>
</plurals>
<string name="test_colour">Color</string>
<string name="test_colour_per_app">%1$s (%2$s)</string>
<string name="test_speed">Speed</string>
<string name="test_camera">Camera light</string>
<string name="test_camera_on">Flashing too</string>
<string name="test_camera_none">Not on this phone</string>
<string name="test_real_length">Real alerts last</string>
<string name="stop">Stop</string>
```

- [ ] **Step 4: Write the failing smoke test.**

```kotlin
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @Test fun launches() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.onActivity { assertFalse(it.isFinishing) }
        }
    }
}
```

- [ ] **Step 5: Run it and check that it fails.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests dev.beacon.ui.MainActivitySmokeTest'`
  - Expected: FAIL, because `MainActivity` doesn't exist yet.

- [ ] **Step 6: Add `BeaconApp` and an empty `MainActivity`.**
  `MainActivity` calls `enableEdgeToEdge()` and `setContent {}`.

- [ ] **Step 7: Run it and check that it passes, then build.**
  - Run the Step 5 command, then `zsh -lc './gradlew :app:assembleDebug'`.
  - Expected: the test passes and `app/build/outputs/apk/debug/app-debug.apk`
    exists.

- [ ] **Step 8: Check the merged manifest has no internet permission.**
  - Run: `grep -c 'android.permission.INTERNET' app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`
  - Expected: `0`. If AGP 9.5.1 puts the merged manifest somewhere else,
    find it with `find app/build -name AndroidManifest.xml -path '*merged*'`.

- [ ] **Step 9: Commit:** "Set up the Beacon app project".

---

### Task 2: Settings types and defaults

**Files:**
- Create: `settings/BlinkSpeed.kt`, `settings/FlashColour.kt`, `settings/Duration.kt`, `settings/Pause.kt`, `settings/BeaconSettings.kt`, `alert/MessageSource.kt`
- Test: `settings/BlinkSpeedTest.kt`, `settings/CustomLengthTest.kt`, `settings/BeaconSettingsTest.kt`

**Interfaces:**
- **Produces `BlinkSpeed`:**
  ```kotlin
  enum class BlinkSpeed(val blinksPerMinute: Int, @StringRes val nameRes: Int) {
      SLOW(30, R.string.speed_slow), MEDIUM(60, R.string.speed_medium), FAST(90, R.string.speed_fast)
  }
  ```
  - `val halfPeriodMillis: Long = 30_000L / blinksPerMinute`
  - `init` throws `IllegalArgumentException` above 180.
- **Produces `FlashColour`:**
  ```kotlin
  enum class FlashColour(val argb: Long, @StringRes val nameRes: Int) {
      RED(0xFFFF3B30, R.string.colour_red), YELLOW(0xFFFFD60A, R.string.colour_yellow),
      BLUE(0xFF0A84FF, R.string.colour_blue), GREEN(0xFF30D158, R.string.colour_green),
      WHITE(0xFFFFFFFF, R.string.colour_white)
  }
  ```
  Declaration order is display order.
- **Produces `DurationChoice` and `CustomLength`:**
  - `enum class DurationChoice { ONE_MINUTE, FIVE_MINUTES, CUSTOM, UNTIL_STOPPED }`
  - `@JvmInline value class CustomLength private constructor(val minutes: Int)`
  - `companion`:
    - `STEPS = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)`
    - `DEFAULT` (10)
    - `fun of(minutes: Int): CustomLength`, where an unknown value becomes `DEFAULT`
  - `fun shorter(): CustomLength`, `fun longer(): CustomLength`, which stay put at the ends
  - `val isShortest: Boolean`, `val isLongest: Boolean`
- **Produces `Pause`:**
  - `sealed interface Pause` with `data object None`, `data class Until(val epochMillis: Long)` and `data object Indefinite`
  - `fun Pause.isPausedAt(nowEpochMillis: Long): Boolean`
- **Produces `MessageSource`:**
  ```kotlin
  enum class MessageSource(
      @StringRes val nameRes: Int,
      val defaultEnabled: Boolean,
      val defaultColour: FlashColour,
      val fixedPackages: List<String>,
  )
  ```
  - `SMS`: `fixedPackages` is empty, because the default SMS app is looked
    up at run time.
  - `WHATSAPP`: `com.whatsapp`, `com.whatsapp.w4b`.
  - `TELEGRAM`: `org.telegram.messenger`, `org.telegram.messenger.web`.
  - Declaration order is display order.
- **Produces the settings model:**
  - `data class AppSetting(val enabled: Boolean, val colour: FlashColour)`
  - `enum class ColourMode { SAME_FOR_ALL, PER_APP }`
  - `data class BeaconSettings(pause, apps: Map<MessageSource, AppSetting>, colourMode, globalColour, duration, customLength, speed)`, as in spec section 5
  - `companion val DEFAULT`
  - `fun colourFor(source: MessageSource): FlashColour`
  - `fun alertLengthMillis(): Long?`, which is `null` for `UNTIL_STOPPED`

- [ ] **Step 1: Write the failing tests.**

```kotlin
// BlinkSpeedTest
@Test fun ratesMatchPresets() { assertEquals(listOf(30, 60, 90), BlinkSpeed.entries.map { it.blinksPerMinute }) }
@Test fun halfPeriods() { assertEquals(listOf(1000L, 500L, 333L), BlinkSpeed.entries.map { it.halfPeriodMillis }) }
@Test fun neverMoreThanThreeFlashesPerSecond() {
    BlinkSpeed.entries.forEach { assertTrue(it.name, 1000.0 / (2 * it.halfPeriodMillis) <= 3.0) }
}

// CustomLengthTest
@Test fun stepsInOrder() { assertEquals(listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60), CustomLength.STEPS) }
@Test fun unknownSnapsToTen() { assertEquals(10, CustomLength.of(7).minutes); assertEquals(10, CustomLength.of(0).minutes) }
@Test fun stepsMoveOneAtATime() { assertEquals(15, CustomLength.of(10).longer().minutes); assertEquals(5, CustomLength.of(10).shorter().minutes) }
@Test fun endsStayPut() {
    assertEquals(1, CustomLength.of(1).shorter().minutes); assertTrue(CustomLength.of(1).isShortest)
    assertEquals(60, CustomLength.of(60).longer().minutes); assertTrue(CustomLength.of(60).isLongest)
}

// BeaconSettingsTest
@Test fun defaultsMatchSpec() {
    val d = BeaconSettings.DEFAULT
    assertEquals(Pause.None, d.pause)
    assertEquals(AppSetting(true, FlashColour.YELLOW), d.apps[MessageSource.SMS])
    assertEquals(AppSetting(true, FlashColour.GREEN), d.apps[MessageSource.WHATSAPP])
    assertEquals(AppSetting(false, FlashColour.BLUE), d.apps[MessageSource.TELEGRAM])
    assertEquals(ColourMode.SAME_FOR_ALL, d.colourMode); assertEquals(FlashColour.YELLOW, d.globalColour)
    assertEquals(DurationChoice.ONE_MINUTE, d.duration); assertEquals(10, d.customLength.minutes)
    assertEquals(BlinkSpeed.MEDIUM, d.speed)
}
@Test fun colourForFollowsMode() {
    val d = BeaconSettings.DEFAULT
    assertEquals(FlashColour.YELLOW, d.colourFor(MessageSource.WHATSAPP))
    assertEquals(FlashColour.GREEN, d.copy(colourMode = ColourMode.PER_APP).colourFor(MessageSource.WHATSAPP))
}
@Test fun alertLengths() {
    val d = BeaconSettings.DEFAULT
    assertEquals(60_000L, d.alertLengthMillis())
    assertEquals(300_000L, d.copy(duration = DurationChoice.FIVE_MINUTES).alertLengthMillis())
    assertEquals(900_000L, d.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(15)).alertLengthMillis())
    assertNull(d.copy(duration = DurationChoice.UNTIL_STOPPED).alertLengthMillis())
}
@Test fun pauseEdges() {
    assertFalse(Pause.None.isPausedAt(5)); assertTrue(Pause.Indefinite.isPausedAt(5))
    assertTrue(Pause.Until(10).isPausedAt(9)); assertFalse(Pause.Until(10).isPausedAt(10))
}
```

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.settings.*"'`
  - Expected: FAIL to compile, because the types don't exist yet.

- [ ] **Step 3: Implement the types above.** `BlinkSpeed`'s `init` uses
  `require` with the message "<name> exceeds 3 flashes per second".

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add Beacon's settings types and defaults".

---

### Task 3: Saved settings (DataStore)

**Files:**
- Create: `settings/SettingsCodec.kt`, `settings/SettingsRepository.kt`
- Modify: `BeaconApp.kt` (add `val settingsRepository`)
- Test: `settings/SettingsCodecTest.kt`, `settings/SettingsRepositoryTest.kt`

**Interfaces:**
- Consumes: the Task 2 types.
- **Produces `SettingsCodec`:**
  - `object SettingsCodec { fun decode(prefs: Preferences): BeaconSettings }`
  - Keys: `pause_kind` (`NONE` | `UNTIL` | `INDEFINITE`), `pause_until`
    (Long), `app_<SOURCE>_enabled`, `app_<SOURCE>_colour`, `colour_mode`,
    `global_colour`, `duration`, `custom_minutes` (Int), `speed`.
  - Enums are stored by `name`.
- **Produces `SettingsRepository(dataStore: DataStore<Preferences>)`:**
  - `val settings: Flow<BeaconSettings>`
  - `suspend fun setPause(pause: Pause)`
  - `suspend fun setAppEnabled(source: MessageSource, enabled: Boolean)`
  - `suspend fun setAppColour(source: MessageSource, colour: FlashColour)`
  - `suspend fun setColourMode(mode: ColourMode)`
  - `suspend fun setGlobalColour(colour: FlashColour)`
  - `suspend fun setDuration(choice: DurationChoice)`
  - `suspend fun stepCustomLength(longer: Boolean)`
  - `suspend fun setSpeed(speed: BlinkSpeed)`
  - `companion fun create(context: Context): SettingsRepository`
- **Produces on `BeaconApp`:** `val settingsRepository: SettingsRepository`
  (lazy).

- [ ] **Step 1: Write the failing tests.**

```kotlin
// SettingsCodecTest
@Test fun emptyIsDefault() { assertEquals(BeaconSettings.DEFAULT, SettingsCodec.decode(emptyPreferences())) }
@Test fun unknownNamesFallBack() {
    val p = preferencesOf(stringPreferencesKey("speed") to "WARP", stringPreferencesKey("global_colour") to "PINK",
        intPreferencesKey("custom_minutes") to 7, stringPreferencesKey("pause_kind") to "SOMETIMES")
    val s = SettingsCodec.decode(p)
    assertEquals(BlinkSpeed.MEDIUM, s.speed); assertEquals(FlashColour.YELLOW, s.globalColour)
    assertEquals(10, s.customLength.minutes); assertEquals(Pause.None, s.pause)
}
@Test fun untilWithoutTimeIsNone() {
    assertEquals(Pause.None, SettingsCodec.decode(preferencesOf(stringPreferencesKey("pause_kind") to "UNTIL")).pause)
}

// SettingsRepositoryTest: a real DataStore in a TemporaryFolder, with runTest
@Test fun roundTripEveryField() = runTest {
    val repo = repoIn(tmp, backgroundScope)
    repo.setPause(Pause.Until(1234L)); repo.setAppEnabled(MessageSource.TELEGRAM, true)
    repo.setAppColour(MessageSource.SMS, FlashColour.RED); repo.setColourMode(ColourMode.PER_APP)
    repo.setGlobalColour(FlashColour.WHITE); repo.setDuration(DurationChoice.CUSTOM)
    repo.stepCustomLength(longer = true); repo.setSpeed(BlinkSpeed.FAST)
    val s = repo.settings.first()
    assertEquals(Pause.Until(1234L), s.pause); assertTrue(s.apps.getValue(MessageSource.TELEGRAM).enabled)
    assertEquals(FlashColour.RED, s.apps.getValue(MessageSource.SMS).colour); assertEquals(ColourMode.PER_APP, s.colourMode)
    assertEquals(FlashColour.WHITE, s.globalColour); assertEquals(DurationChoice.CUSTOM, s.duration)
    assertEquals(15, s.customLength.minutes); assertEquals(BlinkSpeed.FAST, s.speed)
}
@Test fun corruptFileGivesDefaults() = runTest {
    File(tmp.root, "beacon_settings.preferences_pb").writeBytes(byteArrayOf(1, 2, 3, 4, 5))
    assertEquals(BeaconSettings.DEFAULT, repoIn(tmp, backgroundScope).settings.first())
}
```

  `repoIn` builds the repository with
  `PreferenceDataStoreFactory.create(corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }, scope = scope) { File(tmp.root, "beacon_settings.preferences_pb") }`.
  `create(context)` uses the same handler, with
  `context.preferencesDataStoreFile("beacon_settings")`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.settings.Settings*"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement `SettingsCodec`, `SettingsRepository` and
  `BeaconApp.settingsRepository`.**
  - `stepCustomLength` reads the current stored value through
    `CustomLength.of`, then writes `longer()` or `shorter()`.
  - A `Pause` is written as two keys in one `edit`.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Save Beacon's settings with DataStore".

---

### Task 4: Theme, contrast and font

**Files:**
- Create: `ui/theme/BeaconColors.kt`, `ui/theme/Contrast.kt`, `ui/theme/BeaconType.kt`, `ui/theme/BeaconTheme.kt`
- Create: `res/font/atkinson_hyperlegible_next.ttf`, `assets/licenses/atkinson_hyperlegible_next_OFL.txt`
- Test: `ui/theme/ContrastTest.kt`

**Interfaces:**
- **Produces `BeaconColors`:** an `object` with these tokens (all `Color`):

  | Token | Value |
  |---|---|
  | `Page` | `#0B0B0D` |
  | `Text` | `#FFFFFF` |
  | `TextSecondary` | `#D9D9DF` |
  | `Card` | `#1B1B20` |
  | `IconTile` | `#2A2A31` |
  | `TabBar` | `#141418` |
  | `Border` | `#A1A1AA` |
  | `Divider` | `#3A3A42` |
  | `Disabled` | `#AEAEB6` |
  | `Accent` | `#FFD60A` |
  | `OnAccent` | `#0B0B0D` |
  | `Black` | `#000000` |
  | `White` | `#FFFFFF` |

  It also has `val textPairs: List<TextPair>`, where
  `data class TextPair(val text: Color, val background: Color, val use: String)`
  holds the 11 rows of spec section 11.
- **Produces `fun contrastRatio(text: Color, background: Color): Double`**,
  using the WCAG relative-luminance formula.
- **Produces `BeaconType`:** an `object` of `TextStyle`s using
  `BeaconFontFamily`.

  | Style | Size (sp) | Weight | Line height (em) |
  |---|---|---|---|
  | `ScreenTitle` | 36 | 800 | 1.15 |
  | `State` | 56 | 800 | 1.05 |
  | `Heading` | 28 | 800 | 1.2 |
  | `OptionLabel` | 28 | 800 | 1.15 |
  | `RowTitle` | 24 | 800 | 1.2 |
  | `Button` | 24 | 800 | 1.2 |
  | `SmallButton` | 22 | 800 | 1.2 |
  | `BodyStrong` | 22 | 700 | 1.4 |
  | `Body` | 20 | 400 | 1.4 |
  | `Sub` | 20 | 700 | 1.4 |
  | `TestButton` | 26 | 800 | 1.2 |
  | `Stop` | 32 | 800 | 1.2 |
  | `OverlayTitle` | 34 | 800 | 1.15 |
  | `CustomValue` | 40 | 800 | 1.1 |
  | `Countdown` | 96 | 800 | 1.0 |

- **Produces `@Composable fun BeaconTheme(content: @Composable () -> Unit)`.**
  It provides:
  - `LocalIndication` as a white `ripple()`
  - `val LocalBeaconTextStyle = compositionLocalOf { BeaconType.Body }`
  - `val LocalContentColour = compositionLocalOf { BeaconColors.Text }`

  Components draw text with `BasicText`, using these two locals unless they
  pass their own style.

- [ ] **Step 1: Write the failing test.**

```kotlin
@Test fun knownRatios() {
    assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
    assertEquals(7.78, contrastRatio(Color(0xFFAEAEB6), Color(0xFF1B1B20)), 0.01)
}
@Test fun everyTextPairIsAtLeast7to1() {
    BeaconColors.textPairs.forEach { assertTrue("${it.use}: ${contrastRatio(it.text, it.background)}", contrastRatio(it.text, it.background) >= 7.0) }
}
@Test fun elevenPairsListed() { assertEquals(11, BeaconColors.textPairs.size) }
```

- [ ] **Step 2: Run the test and check that it fails.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests dev.beacon.ui.theme.ContrastTest'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Add the font.** The owner approved this download with the
  plan.
  - Source: `https://github.com/google/fonts/raw/main/ofl/atkinsonhyperlegiblenext/`.
  - Files: `AtkinsonHyperlegibleNext[wght].ttf` (114,552 bytes), saved as
    `res/font/atkinson_hyperlegible_next.ttf`, and `OFL.txt` (4,431 bytes),
    saved as `assets/licenses/atkinson_hyperlegible_next_OFL.txt`.
  - Check that both sizes match before using them.
  - `BeaconFontFamily` has three `Font(R.font.atkinson_hyperlegible_next, weight, variationSettings = FontVariation.Settings(FontVariation.weight(n)))`
    entries, for weights 400, 700 and 800.

- [ ] **Step 4: Implement the colours, `contrastRatio`, `BeaconType` and
  `BeaconTheme`.**

- [ ] **Step 5: Run the test and check that it passes.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 6: Commit:** "Add Beacon's colours, type and bundled font".

---

### Task 5: Blink clock and camera light

**Files:**
- Create: `alert/BlinkClock.kt`, `alert/TorchSwitch.kt`, `alert/TorchBlinker.kt`
- Create (test sources): `app/src/test/kotlin/dev/beacon/alert/FakeTorchSwitch.kt`
- Test: `alert/BlinkClockTest.kt`, `alert/TorchBlinkerTest.kt` (Robolectric)

**Interfaces:**
- **Produces `class BlinkClock(val startUptimeMillis: Long, val speed: BlinkSpeed, val lengthMillis: Long?)`:**
  - `val endUptimeMillis: Long?`
  - `fun isColourPhase(nowUptimeMillis: Long): Boolean`. Before the start
    counts as colour.
  - `fun isFinished(nowUptimeMillis: Long): Boolean`
  - `fun remainingMillis(nowUptimeMillis: Long): Long?`, which is `null`
    when there is no end and never below 0
  - `fun flipTime(step: Long): Long = startUptimeMillis + step * speed.halfPeriodMillis`
- **Produces `TorchSwitch`:**
  ```kotlin
  interface TorchSwitch {
      val available: Boolean
      fun set(on: Boolean): Boolean  // false when refused
  }
  ```
  `class CameraTorchSwitch(context: Context) : TorchSwitch` finds the camera
  with a flash, preferring the back camera (the spike's
  `findFlashCamera`). Its `set` catches `CameraAccessException` and
  `IllegalArgumentException` and returns `false`.
- **Produces `class TorchBlinker(torch: TorchSwitch, clock: BlinkClock, looper: Looper)`:**
  - `fun start(): Boolean`, which returns `false` when
    `!torch.available`
  - `fun stop()`
- **Produces, in test sources,
  `class FakeTorchSwitch(override val available: Boolean = true, private val refuseFrom: Int = Int.MAX_VALUE) : TorchSwitch`:**
  - It records `events: List<Pair<Long, Boolean>>` as
    (`SystemClock.uptimeMillis()`, on).
  - From call number `refuseFrom` on, it returns `false`.
  - Task 6 reuses it.

- [ ] **Step 1: Write the failing tests.**

```kotlin
// BlinkClockTest
@Test fun phases() {
    val c = BlinkClock(1_000, BlinkSpeed.MEDIUM, 3_000)
    assertTrue(c.isColourPhase(1_000)); assertTrue(c.isColourPhase(1_499))
    assertFalse(c.isColourPhase(1_500)); assertTrue(c.isColourPhase(2_000)); assertTrue(c.isColourPhase(999))
}
@Test fun endAndRemaining() {
    val c = BlinkClock(1_000, BlinkSpeed.FAST, 3_000)
    assertEquals(4_000L, c.endUptimeMillis); assertFalse(c.isFinished(3_999)); assertTrue(c.isFinished(4_000))
    assertEquals(2_000L, c.remainingMillis(2_000)); assertEquals(0L, c.remainingMillis(9_000))
}
@Test fun untilStoppedNeverEnds() {
    val c = BlinkClock(0, BlinkSpeed.SLOW, null)
    assertNull(c.endUptimeMillis); assertFalse(c.isFinished(Long.MAX_VALUE / 2)); assertNull(c.remainingMillis(5))
}

// TorchBlinkerTest: a FakeTorchSwitch records (SystemClock.uptimeMillis(), on), on the paused main looper
@Test fun flipsOnTheClock() {
    val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
    val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.MEDIUM, null), Looper.getMainLooper())
    assertTrue(b.start()); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_000))
    assertEquals(listOf(0L to true, 500L to false, 1_000L to true), fake.events.map { (t, on) -> (t - now) to on })
}
@Test fun stopLeavesLightOff() { /* start, idle 700 ms, stop, idle: fake.events.last().second == false, no events after stop */ }
@Test fun noLightReturnsFalse() { assertFalse(TorchBlinker(FakeTorchSwitch(available = false), BlinkClock(0, BlinkSpeed.FAST, null), Looper.getMainLooper()).start()) }
@Test fun refusedLightStopsQuietly() { /* fake refuses from the 2nd call: after idling 2 s, it was called at most twice more (the final off) */ }
```

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.alert.*"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement.**
  - **`TorchBlinker` scheduling.** Flip `n` happens at `clock.flipTime(n)`.
    The light is ON on even `n`, so it matches
    `BlinkClock.isColourPhase`. Each flip is posted with
    `Handler(looper).postAtTime`, so delays don't add up (the spike's
    method).
  - **When the light is refused.** The first `set` that returns `false`
    ends the run: it removes the pending callbacks and tries `set(false)`
    once.
  - **`stop`.** Always ends with `set(false)`. Calling it twice is safe.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add the blink clock and the camera-light blinker".

---

### Task 6: Alert controller, test path

**Files:**
- Create: `alert/AlertModel.kt`, `alert/AlertController.kt`
- Modify: `BeaconApp.kt` (add `torchThread: HandlerThread`, started in `onCreate`, and `val alertController`)
- Test: `alert/AlertControllerTest.kt` (Robolectric)

**Interfaces:**
- Consumes: `BlinkClock`, `TorchSwitch`, `TorchBlinker`, `BlinkSpeed`,
  `FlashColour`, `MessageSource`.
- **Produces the alert model:**
  - `enum class AlertKind { TEST, REAL }`
  - `data class AlertRequest(val kind: AlertKind, val colour: FlashColour, val speed: BlinkSpeed, val lengthMillis: Long?, val source: MessageSource?)`
  - `sealed interface AlertState` with `data object Idle` and
    `data class Running(val request: AlertRequest, val clock: BlinkClock, val lightFlashing: Boolean)`
  - `const val TEST_LENGTH_MILLIS = 3_000L`
- **Produces `class AlertController(torch: TorchSwitch, torchLooper: Looper, mainLooper: Looper)`:**
  - `val state: StateFlow<AlertState>`
  - `val hasLight: Boolean` (`torch.available`)
  - `fun start(request: AlertRequest): Boolean`, which returns `false`
    while any alert is running. PR 2 adds the rule that a real alert
    replaces a test.
  - `fun stop()`
  - It ends by itself at `clock.endUptimeMillis`, posted on the main looper.

- [ ] **Step 1: Write the failing tests.**

```kotlin
private fun testRequest() = AlertRequest(AlertKind.TEST, FlashColour.YELLOW, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS, null)

@Test fun startRunsAndEndsAfterThreeSeconds() {
    val c = controller(FakeTorchSwitch())
    assertTrue(c.start(testRequest())); assertTrue(c.state.value is AlertState.Running)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3_000))
    assertEquals(AlertState.Idle, c.state.value)
}
@Test fun secondStartIsDropped() {
    val fake = FakeTorchSwitch(); val c = controller(fake)
    assertTrue(c.start(testRequest())); assertFalse(c.start(testRequest()))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(10))
    assertEquals(1, fake.events.count { it.second } )   // one ON flip so far: one light session
}
@Test fun stopTurnsLightOffAndGoesIdle() { /* start, idle 200 ms, stop: Idle, fake.events.last().second == false */ }
@Test fun noLightStillRunsScreenOnly() {
    val c = controller(FakeTorchSwitch(available = false))
    assertTrue(c.start(testRequest())); assertFalse((c.state.value as AlertState.Running).lightFlashing); assertFalse(c.hasLight)
}
```

  `controller(fake)` passes the main looper as both loopers, so one
  `idleFor` drives everything. `FakeTorchSwitch` comes from Task 5's test
  sources.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests dev.beacon.alert.AlertControllerTest'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement `AlertController`, and wire it into `BeaconApp`.**
  - Wiring: `CameraTorchSwitch(this)`, `torchThread.looper` and
    `Looper.getMainLooper()`.
  - `start`:
    1. Create `BlinkClock(SystemClock.uptimeMillis(), …)`.
    2. Create and start a `TorchBlinker`.
    3. Set `Running`.
    4. Post the end with a token, so `stop` can remove it.
  - No wake lock and no thermal check in this PR. The test only runs while
    the app is in front, and both come with real alerts in PR 2.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add the alert controller for test alerts".

---

### Task 7: Shared components

**Files:**
- Create: `ui/components/BeaconIcons.kt`, `Buttons.kt`, `OptionButton.kt`, `OnOffPill.kt`, `AppSwitchRow.kt`, `SwatchGrid.kt`, `AdaptiveGrid.kt`, `Headings.kt`
- Create: `app/src/test/kotlin/dev/beacon/ui/UiTestSupport.kt`
- Test: `ui/components/ComponentsTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`), `ui/components/AdaptiveGridTest.kt` (JVM)

**Interfaces:**
- **Produces `BeaconIcons`:** an `object` of `ImageVector`s built from the
  design's SVG path data, copied exactly from `Main.dc.html` and
  `Widget.dc.html`.
  - `Bolt`, `Pause`, `Sms`, `WhatsApp`, `Telegram`, `Home`, `Sliders`,
    `Plus`, `Minus`, `Stop`, `Check`
  - `Chevron`, with `autoMirror = true`
  - `SelectedCircle`, a black circle with a yellow tick
  - `EmptyCircle`, a white ring
- **Produces in `Headings.kt`:**
  - `@Composable fun ScreenTitle(text: String)`
  - `@Composable fun SectionHeading(text: String)`

  Both use `Modifier.semantics { heading() }`.
- **Produces in `Buttons.kt`:**
  - `@Composable fun FilledButton(text: String, onClick: () -> Unit, background: Color, content: Color, icon: ImageVector?, minHeight: Dp, style: TextStyle, modifier: Modifier = Modifier, enabled: Boolean = true)`
  - `@Composable fun OutlineButton(…)`: the same parameters, drawn as a
    3 dp border in the content colour on a transparent background. When
    disabled, it uses `BeaconColors.Disabled` and `disabled()` semantics.
- **Produces `@Composable fun OptionButton(label: String, selected: Boolean, onSelect: () -> Unit, minHeight: Dp, modifier: Modifier = Modifier, sub: List<String> = emptyList())`:**
  - Semantics: `Modifier.selectable(selected, role = Role.RadioButton)`.
  - Selected: yellow fill with `SelectedCircle`. Unselected: card fill,
    `Border`, `EmptyCircle`.
  - The callers put `Modifier.selectableGroup()` on the containing layout.
- **Produces `@Composable fun OnOffPill(on: Boolean)`:**
  - Minimum 100 × 60 dp, growing with its text: never a fixed height.
  - Decorative: `clearAndSetSemantics {}`, because the row carries the
    state.
- **Produces `@Composable fun AppSwitchRow(name: String, flashLine: String, swatch: Color?, enabled: Boolean, onToggle: () -> Unit, icon: @Composable () -> Unit)`:**
  - Semantics: `Modifier.toggleable(enabled, role = Role.Switch)`, which
    TalkBack reads as "On" or "Off".
  - Merged label: name, then flash line.
- **Produces `@Composable fun SwatchGrid(selected: FlashColour, onPick: (FlashColour) -> Unit, minHeight: Dp)`:**
  - Each swatch is an `OptionButton`-style `selectable` with
    `Role.RadioButton`, labelled by the colour name.
  - White spans the full row.
  - The selected ring is 4 dp `Page` inside 4 dp `White`, plus a `Check`
    in the label.
- **Produces `fun gridColumns(maxWidth: Dp, fontScale: Float): Int`:** 2
  when `maxWidth.value / fontScale >= 300f`, else 1.
- **Produces `@Composable fun AdaptiveGrid(…)`:** lays items in
  `gridColumns(...)` columns, reading `LocalDensity.current.fontScale`
  inside `BoxWithConstraints`. An item can span the full row.
- **Produces in `UiTestSupport.kt`:**
  - `fun ComposeContentTestRule.assertNoTextOverflow()`: for every node
    with `SemanticsActions.GetTextLayoutResult`, it asserts
    `!hasVisualOverflow`.
  - `@Composable fun WithFontScale(scale: Float, content: @Composable () -> Unit)`:
    it provides `LocalDensity` with the same density and `fontScale = scale`.
    This is linear scaling, which is stricter than Android's non-linear
    scaling at 200%. Use it for content-level tests.
  - `class FontScaleRule(scale: Float) : TestRule`: it calls
    `RuntimeEnvironment.setFontScale(scale)` before the test and `1f`
    after. Apply it as the outer rule in a `RuleChain` around
    `createAndroidComposeRule`, so the activity launches already scaled.
    Use it for activity-level tests.
  - `fun hasRole(role: Role): SemanticsMatcher`.

- [ ] **Step 1: Write the failing tests.**

```kotlin
// AdaptiveGridTest
@Test fun columns() {
    assertEquals(2, gridColumns(358.dp, 1.0f)); assertEquals(2, gridColumns(300.dp, 1.0f))
    assertEquals(1, gridColumns(299.dp, 1.0f)); assertEquals(1, gridColumns(358.dp, 1.3f))
}

// ComponentsTest
@Test fun appRowIsASwitchThatSaysOnOff() {
    var on by mutableStateOf(true)
    rule.setContent { BeaconTheme { AppSwitchRow("Texts (SMS)", "Yellow flash", Color.Yellow, on, { on = !on }) {} } }
    rule.onNode(isToggleable()).assert(hasRole(Role.Switch)).assertIsOn().assertTextContains("Texts (SMS)", substring = true)
    rule.onNodeWithText("ON").assertExists()
    rule.onNode(isToggleable()).performClick().assertIsOff(); rule.onNodeWithText("OFF").assertExists()
}
@Test fun optionIsRadioButton() { /* OptionButton selected=true: hasRole(Role.RadioButton), assertIsSelected() */ }
@Test fun headingsAreHeadings() { /* SectionHeading("X"): SemanticsProperties.Heading present */ }
@Test fun disabledOutlineButtonIsDisabled() { /* OutlineButton(enabled=false): assertIsNotEnabled() */ }
@Test fun swatchesAreRadioButtonsNamedByColour() { /* 5 nodes with Role.RadioButton; "Yellow" selected */ }
@Test fun pillAndRowDontClipAtLargestFont() {
    rule.setContent { BeaconTheme { WithFontScale(2.0f) { AppSwitchRow("WhatsApp messages", "Green flash", Color.Green, false, {}) {} } } }
    rule.assertNoTextOverflow()
}
@Test fun rowMirrorsInRtl() {
    /* LayoutDirection.Rtl: the icon slot's left bound > the name's left bound */
}
```

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.components.*"'`
  - Expected: FAIL to compile.

- [ ] **Step 3: Implement the components.** Sizes, radii, borders and
  colours are spec section 8's. Every component takes `Modifier` as its
  last-but-one or last parameter.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add Beacon's shared accessible components".

---

### Task 8: App shell: view model, tabs and bottom area

**Files:**
- Create: `ui/MainViewModel.kt`, `ui/BeaconAppScreen.kt`, `ui/BottomArea.kt`
- Modify: `ui/MainActivity.kt` (`setContent { BeaconTheme { BeaconAppScreen(viewModel) } }`)
- Test: `ui/BottomAreaTest.kt` (JVM), `ui/AppShellTest.kt` (Robolectric, `createAndroidComposeRule<MainActivity>()`)

**Interfaces:**
- Consumes: `SettingsRepository`, `AlertController`, the components.
- **Produces `class MainViewModel(repo: SettingsRepository, alerts: AlertController) : ViewModel`:**
  - `val settings: StateFlow<BeaconSettings?>`, which is `null` until the
    first read
  - `val alertState: StateFlow<AlertState>`
  - `val hasLight: Boolean`
  - The setters from Task 3, as non-suspending functions that launch in
    `viewModelScope`
  - `companion val Factory`, through `viewModelFactory`, reading
    `BeaconApp` from `APPLICATION_KEY`
  - Task 11 adds `startTest` and `stopTest`.
- **Produces `enum class Tab { HOME, SETTINGS }`.**
- **Produces `fun testButtonFixed(fixedAreaHeightPx: Int, windowHeightPx: Int): Boolean = fixedAreaHeightPx * 3 <= windowHeightPx`.**
- **Produces in `BeaconAppScreen(viewModel)`:**
  - The saved values `tab: Tab` and `editingApp: MessageSource?`, through
    `rememberSaveable`.
  - The screen is blank (`Page` colour) while `settings == null`.
  - Each tab gets a fresh `rememberScrollState()` under `key(tab)`, so
    each tab opens at its top.
  - A `BackHandler` on the Settings tab goes to `HOME`.
  - The scrolling content has the test tag `scroll_content`.
- **Produces in `BottomArea.kt`:**
  - `@Composable fun TestAlertButton(onClick: () -> Unit)`: test tag
    `test_button`. In this task `BeaconAppScreen` passes an empty lambda,
    and Task 11 wires it.
  - `@Composable fun TabBar(tab: Tab, onSelect: (Tab) -> Unit)`: a
    `selectableGroup`, with each tab `selectable(role = Role.Tab)`.
  - The layout uses `SubcomposeLayout`. It measures the tab bar and the
    test button against the window, then puts the test button either fixed
    above the tabs or as the last item of `scroll_content`, according to
    `testButtonFixed`.

- [ ] **Step 1: Write the failing tests.**

```kotlin
// BottomAreaTest
@Test fun fixedUpToAThird() { assertTrue(testButtonFixed(300, 900)); assertFalse(testButtonFixed(301, 900)) }

// AppShellTest
@Test fun twoTabsWithRolesAndSelection() {
    rule.onAllNodes(hasRole(Role.Tab)).assertCountEquals(2)
    rule.onNode(hasRole(Role.Tab) and hasText("Home")).assertIsSelected()
    rule.onNode(hasRole(Role.Tab) and hasText("Alert settings")).performClick().assertIsSelected()
    rule.onNode(hasText("Alert settings") and isHeading()).assertExists()
}
@Test fun backFromSettingsGoesHome() { /* select settings, Espresso.pressBack(): Home tab selected, activity not finishing */ }
@Test fun testButtonFixedAtNormalFont() {
    rule.onNode(hasTestTag("test_button") and hasAnyAncestor(hasTestTag("scroll_content"))).assertDoesNotExist()
}
// AppShellLargestFontTest: RuleChain.outerRule(FontScaleRule(2.0f)).around(createAndroidComposeRule<MainActivity>())
@Test @Config(qualifiers = "w360dp-h640dp") fun testButtonScrollsAtLargestFont() {
    rule.onNode(hasTestTag("test_button") and hasAnyAncestor(hasTestTag("scroll_content"))).assertExists()
}
```

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.BottomAreaTest" --tests "dev.beacon.ui.AppShell*"'`
  - Expected: FAIL.

- [ ] **Step 3: Implement the view model, the screen shell and the bottom
  area.** The Home and Settings slots get placeholder `ScreenTitle`s until
  Tasks 9 and 10.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Add the app shell with two tabs and the test button".

---

### Task 9: Home

**Files:**
- Create: `ui/home/HomeScreen.kt`, `AlertsCard.kt`, `AppsSection.kt`, `SummaryCard.kt`, `AppIcon.kt`, `ui/Summaries.kt`
- Modify: `ui/BeaconAppScreen.kt` (show `HomeScreen`)
- Test: `ui/home/HomeScreenTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`), `ui/SummariesTest.kt` (Robolectric)

**Interfaces:**
- **Produces `@Composable fun HomeScreen(settings: BeaconSettings, actions: HomeActions)`:**
  - `data class HomeActions(val pauseAll: () -> Unit, val resume: () -> Unit, val toggleApp: (MessageSource) -> Unit, val openSettings: () -> Unit)`
  - `pauseAll` sets `Pause.Indefinite`. `resume` sets `Pause.None`.
- **Produces in `Summaries.kt`:**
  - `@Composable fun durationSummary(s: BeaconSettings): String`, giving
    "1 minute", "5 minutes", "N minute(s)" through plurals, or "Until I
    stop it".
  - `@Composable fun speedSummary(speed: BlinkSpeed): String`, for example
    "Medium, 60 a minute".
  - `@Composable fun colourName(c: FlashColour): String`.
- **Produces `@Composable fun AppIcon(source: MessageSource)`:**
  - It loads the launcher icon of the first installed package:
    `Telephony.Sms.getDefaultSmsPackage` for SMS, else `fixedPackages`.
  - If none is installed, it shows the glyph, with test tag
    `glyph_<SOURCE>`.
  - It is decorative, with no content description.
- What it shows: spec section 8, Home, items 1 and 3 to 5. The "Finish
  setup" card is PR 2, and the "Back ON by itself" line is PR 3.

- [ ] **Step 1: Write the failing tests.**

```kotlin
@Test fun titleIsBeaconAndHeading() { rule.onNodeWithText("Beacon").assert(isHeading()) }
@Test fun onCardPausesIndefinitely() {
    rule.onNodeWithText("Pause all alerts").performClick(); assertEquals(listOf("pauseAll"), calls)
}
@Test fun pausedCardAndBanner() {
    show(BeaconSettings.DEFAULT.copy(pause = Pause.Indefinite))
    rule.onNodeWithText("PAUSED").assertExists(); rule.onNodeWithText("All alerts are paused right now.").assertExists()
    rule.onNodeWithText("Turn alerts back ON").performClick(); assertEquals(listOf("resume"), calls)
}
@Test fun threeAppSwitchesInOrder() {
    val rows = rule.onAllNodes(isToggleable()); rows.assertCountEquals(3)
    rows[0].assertTextContains("Texts (SMS)", substring = true).assertIsOn()
    rows[1].assertTextContains("WhatsApp messages", substring = true).assertIsOn()
    rows[2].assertTextContains("Telegram", substring = true).assertIsOff().assertTextContains("No flash", substring = true)
}
@Test fun flashLineFollowsColourMode() {
    rule.onAllNodes(isToggleable())[1].assertTextContains("Yellow flash", substring = true)
    show(BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP))
    rule.onAllNodes(isToggleable())[1].assertTextContains("Green flash", substring = true)
}
@Test fun summaryRows() {
    rule.onNodeWithText("1 minute").assertExists(); rule.onNodeWithText("Medium").assertExists(); rule.onNodeWithText("Yellow").assertExists()
    show(BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP)); rule.onNodeWithText("Different per app").assertExists()
}
@Test fun changeSettingsOpensSettings() { /* click "Change settings": calls == ["openSettings"] */ }
@Test fun glyphWhenAppNotInstalled() { rule.onNodeWithTag("glyph_TELEGRAM", useUnmergedTree = true).assertExists() }
@Test fun noOverflowAtLargestFont() { show(BeaconSettings.DEFAULT, fontScale = 2.0f); rule.assertNoTextOverflow() }
@Test fun noOverflowAtLargestFontPaused() { show(BeaconSettings.DEFAULT.copy(pause = Pause.Indefinite), fontScale = 2.0f); rule.assertNoTextOverflow() }
@Test fun chevronMirrorsInRtl() { /* Rtl: the chevron's right bound <= the "Change settings" text's left bound */ }
// SummariesTest
@Test fun durationTexts() { /* ONE_MINUTE "1 minute", FIVE_MINUTES "5 minutes", CUSTOM 1 "1 minute", CUSTOM 45 "45 minutes", UNTIL_STOPPED "Until I stop it" */ }
@Test fun speedText() { /* MEDIUM → "Medium, 60 a minute", FAST → "Fast, 90 a minute" */ }
```

  `show(settings, fontScale = 1f)` sets the content inside
  `WithFontScale(fontScale)`, with a `HomeActions` whose lambdas record
  their names in `calls`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.home.*" --tests dev.beacon.ui.SummariesTest'`
  - Expected: FAIL.

- [ ] **Step 3: Implement Home and wire it into `BeaconAppScreen`.**
  - `toggleApp` calls `setAppEnabled(source, !current)`.
  - `openSettings` sets the tab to `SETTINGS`.

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Build the Home screen".

---

### Task 10: Alert settings

**Files:**
- Create: `ui/settings/AlertSettingsScreen.kt`, `DurationSection.kt`, `SpeedSection.kt`, `ColourSection.kt`
- Modify: `ui/BeaconAppScreen.kt` (show `AlertSettingsScreen`, passing `editingApp` and its setter)
- Test: `ui/settings/AlertSettingsScreenTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`)

**Interfaces:**
- **Produces `@Composable fun AlertSettingsScreen(settings: BeaconSettings, editingApp: MessageSource?, actions: SettingsActions)`:**
  ```kotlin
  data class SettingsActions(
      val setDuration: (DurationChoice) -> Unit, val stepCustom: (longer: Boolean) -> Unit,
      val setSpeed: (BlinkSpeed) -> Unit, val setColourMode: (ColourMode) -> Unit,
      val setGlobalColour: (FlashColour) -> Unit, val setAppColour: (MessageSource, FlashColour) -> Unit,
      val setEditingApp: (MessageSource?) -> Unit,
  )
  ```
- Each option group sits in its own `Modifier.selectableGroup()` container.
  The duration grid and the swatch grids use `AdaptiveGrid`.
- **Change/Done button:**
  - Spoken label: `colour_change_for` or `colour_done_for`, with the app
    name.
  - State description: `state_collapsed` or `state_expanded`.
- **Custom value:** `liveRegion = LiveRegionMode.Polite`.
- What it shows: spec section 8, Alert settings.

- [ ] **Step 1: Write the failing tests.**

```kotlin
@Test fun durationRadioGroupDefaultOneMinute() {
    rule.onNode(hasRole(Role.RadioButton) and hasText("1 min")).assertIsSelected()
    rule.onNode(hasRole(Role.RadioButton) and hasText("Until I stop it")).performClick(); assertEquals(DurationChoice.UNTIL_STOPPED, last.duration)
}
@Test fun customPanelOnlyWhenCustom() {
    rule.onNodeWithText("Custom length").assertDoesNotExist()
    show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM))
    rule.onNodeWithText("Custom length").assertExists(); rule.onNodeWithText("10 min").assertExists()
    rule.onNodeWithText("From 1 to 60 minutes.").assertExists()
}
@Test fun shorterDisabledAtOneLongerAtSixty() {
    show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(1)))
    rule.onNodeWithText("Shorter").assertIsNotEnabled(); rule.onNodeWithText("Longer").assertIsEnabled()
    show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(60)))
    rule.onNodeWithText("Longer").assertIsNotEnabled()
}
@Test fun speedsWithFastNote() {
    rule.onNode(hasRole(Role.RadioButton) and hasText("Medium", substring = true)).assertIsSelected()
    rule.onNode(hasRole(Role.RadioButton) and hasText("Fast", substring = true)).assertTextContains("Highest safe speed", substring = true)
}
@Test fun sameColourShowsOneSwatchGrid() { rule.onAllNodes(hasRole(Role.RadioButton) and hasText("Yellow")).assertCountEquals(1) }
@Test fun perAppCardsOneOpenAtATime() {
    show(BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP), editingApp = MessageSource.WHATSAPP)
    rule.onNodeWithContentDescription("Done choosing color for WhatsApp messages").assertExists()
    rule.onNodeWithContentDescription("Change color for Telegram").performClick(); assertEquals(MessageSource.TELEGRAM, editing)
    rule.onAllNodes(hasRole(Role.RadioButton) and hasText("Red")).assertCountEquals(1)
}
@Test fun noOverflowAtLargestFont() {
    show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, colourMode = ColourMode.PER_APP), MessageSource.SMS, fontScale = 2.0f)
    rule.assertNoTextOverflow()
}
```

  `show(settings, editingApp = null, fontScale = 1f)` keeps the settings
  and `editingApp` in `mutableStateOf`, so the screen redraws when they
  change. The fake actions update them. `last` is the current settings and
  `editing` is the current `editingApp`.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.settings.*"'`
  - Expected: FAIL.

- [ ] **Step 3: Implement the screen and wire it into `BeaconAppScreen`.**

- [ ] **Step 4: Run the tests and check that they pass.**
  - Run: the Step 2 command.
  - Expected: PASS.

- [ ] **Step 5: Commit:** "Build the Alert settings screen".

---

### Task 11: Test alert overlay

**Files:**
- Create: `ui/testalert/TestColour.kt`, `BlinkScreen.kt`, `TestAlertOverlay.kt`
- Modify: `ui/BeaconAppScreen.kt`:
  - Draw the overlay above everything while `alertState` is `Running`
    with `kind == TEST`.
  - Wire the Test alert button to `startTest(editingApp)`.
  - Add a `BackHandler` that stops the test.
  - While the overlay shows, put `clearAndSetSemantics {}` on the screen
    and bottom area underneath, so TalkBack only reaches the overlay.
- Modify: `ui/MainViewModel.kt`:
  - `fun startTest(editingApp: MessageSource?)`: builds a TEST
    `AlertRequest` from `testColour(…)`, `settings.speed` and
    `TEST_LENGTH_MILLIS`. It ignores the pause.
  - `fun stopTest()`.
- Test: `ui/testalert/TestColourTest.kt` (JVM), `ui/testalert/TestAlertOverlayTest.kt` (Robolectric, `createAndroidComposeRule<MainActivity>()`, `@GraphicsMode(NATIVE)`)

**Interfaces:**
- **Produces `fun testColour(s: BeaconSettings, editingApp: MessageSource?): Pair<FlashColour, MessageSource?>`:**
  - The second value is the app named in the label, or `null` in
    same-colour mode.
  - In per-app mode: `editingApp`, else the first app that is ON, else
    `SMS`.
- **Produces `@Composable fun BlinkScreen(clock: BlinkClock, colour: FlashColour, content: @Composable () -> Unit)`:**
  - A frame loop (`withFrameMillis`) reads `SystemClock.uptimeMillis()`
    and sets the background to the colour when `clock.isColourPhase(now)`,
    otherwise `BeaconColors.Black`.
  - It fills the whole window. The content gets
    `windowInsetsPadding(WindowInsets.safeDrawing)`.
- **Produces `@Composable fun TestAlertOverlay(running: AlertState.Running, colourLabel: String, speedLabel: String, lengthLabel: String, hasLight: Boolean, onStop: () -> Unit)`:**
  - The countdown is `ceil(remainingMillis / 1000)`, clamped to 1..3 and
    computed each frame from the clock. It never keeps its own timer.
  - The title has `liveRegion = Polite`, so it is announced.

- [ ] **Step 1: Write the failing tests.**

```kotlin
// TestColourTest
@Test fun sameColourMode() { assertEquals(FlashColour.YELLOW to null, testColour(BeaconSettings.DEFAULT, MessageSource.WHATSAPP)) }
@Test fun perAppUsesEditingApp() { assertEquals(FlashColour.BLUE to MessageSource.TELEGRAM, testColour(perApp, MessageSource.TELEGRAM)) }
@Test fun perAppFirstOnApp() { assertEquals(FlashColour.YELLOW to MessageSource.SMS, testColour(perApp, null)) }
@Test fun perAppNoneOnFallsBackToSms() { /* all apps OFF, editingApp null → SMS's colour, SMS */ }

// TestAlertOverlayTest (Robolectric has no camera light)
@Test fun overlayShowsDetails() {
    openTest()
    rule.onNodeWithText("This is how your alerts will look").assertExists()
    rule.onNodeWithText("3").assertExists(); rule.onNodeWithText("seconds left").assertExists()
    rule.onAllNodesWithText("Yellow").assertCountEquals(1)   // the Home summary underneath is hidden
    rule.onNodeWithText("Medium, 60 a minute").assertExists()
    rule.onNodeWithText("Not on this phone").assertExists(); rule.onNodeWithText("1 minute").assertExists()
}
@Test fun stopAndBackClose() { /* Stop → title gone, controller Idle; open again, pressBack → gone */ }
@Test fun endsAfterThreeSeconds() { openTest(); advance(3_000); rule.onNodeWithText("This is how your alerts will look").assertDoesNotExist() }
@Test fun oneRunWhenTappedTwice() {
    // Both clicks land before the next frame, as a quick double tap would.
    rule.onNodeWithTag("test_button").performClick(); rule.onNodeWithTag("test_button").performClick()
    advance(16); rule.onAllNodesWithText("This is how your alerts will look").assertCountEquals(1)
}
@Test fun recreateContinuesCountdown() {
    openTest(); advance(1_100); rule.activityRule.scenario.recreate(); advance(16)
    rule.onNodeWithText("2").assertExists(); rule.onNodeWithText("3").assertDoesNotExist()
}
@Test fun runsWhilePaused() { /* runBlocking { app.settingsRepository.setPause(Pause.Indefinite) }; openTest(): title exists */ }
// TestAlertOverlayLargestFontTest: RuleChain.outerRule(FontScaleRule(2.0f)).around(rule)
@Test fun noOverflowAtLargestFont() { openTest(); rule.assertNoTextOverflow() }
```

  - **Turn off the test clock's auto-advance.** Every overlay test sets
    `rule.mainClock.autoAdvance = false` first, because `BlinkScreen`'s
    frame loop never idles.
  - **Helpers:**
    - `openTest()` clicks `test_button`, then `advance(16)`.
    - `advance(ms)` calls `shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))`,
      which moves `SystemClock` and runs the controller's end, then
      `rule.mainClock.advanceTimeBy(ms)`.
  - **Clean-up.** In `@After`, call `stop()` on
    `BeaconApp.alertController`, so the controller doesn't leak between
    tests.

- [ ] **Step 2: Run the tests and check that they fail.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest --tests "dev.beacon.ui.testalert.*"'`
  - Expected: FAIL.

- [ ] **Step 3: Implement `testColour`, `BlinkScreen` and the overlay, and
  wire them into the screen shell.**

- [ ] **Step 4: Run every test and check that they all pass.**
  - Run: `zsh -lc './gradlew :app:testDebugUnitTest'`
  - Expected: PASS, with no failures across the whole suite.

- [ ] **Step 5: Commit:** "Add the 3-second test alert overlay".

---

### Task 12: Emulator checks, docs and the PR draft

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `docs/specs/2026-11-08-step2-design.md` (status: PR 1 built)
- Create: `docs/checks/2026-11-step2-pr1.md`, which records each check and
  links its screenshots
- Create: screenshots under `docs/checks/step2-pr1/`

- [ ] **Step 1: Install the emulator.**
  - Ask the owner first, naming each component and its licence.
  - Install `emulator` and the `system-images;android-36;google_apis;arm64-v8a`
    image with `sdkmanager`.
  - Create a Pixel-sized AVD named `beacon-api36`.

- [ ] **Step 2: Build, install and launch.**
  - Run: `zsh -lc './gradlew :app:installDebug'`, then
    `adb shell am start -n dev.beacon/.ui.MainActivity`.
  - Expected: Home appears.

- [ ] **Step 3: Take screenshots at the default font size.**
  - Screens: Home ON, Home PAUSED, Alert settings (top, Custom open,
    per-app open) and the test overlay in its colour phase.
  - Use `adb exec-out screencap -p`.

- [ ] **Step 4: Take the same screenshots at the largest font size.**
  - Run: `adb shell settings put system font_scale 2.0`.
  - Then take the same set, plus one in landscape.
  - Set the font scale back to 1.0 afterwards. The emulator is the test
    device, never the owner's phone.

- [ ] **Step 5: Do a TalkBack pass.**
  - Enable TalkBack on the emulator.
  - Swipe through each screen and record what each control announces in
    the checks file.
  - Expected: the roles and states match spec section 11's TalkBack table.

- [ ] **Step 6: Update the docs.**
  - README: status, "Building" for the app, and what PR 1 delivers.
  - CHANGELOG: entries under `[Unreleased]`.
  - Spec status: PR 1 built.
  - Checks file: anything not run on the real phone is labelled "Not
    tested".

- [ ] **Step 7: Request a whole-branch review** with
  superpowers:requesting-code-review, and fix what it confirms.

- [ ] **Step 8: Commit:** "Record PR 1 checks and update docs".

- [ ] **Step 9: Write the PR draft** for the owner: Title, Context/Why,
  Summary of Changes (with spec section 3's conflicts that this PR
  resolves), Test Steps and Doc Impact. Push and open the PR only after the
  owner's OK, using the keychain-token `gh` command from `CLAUDE.md`.
