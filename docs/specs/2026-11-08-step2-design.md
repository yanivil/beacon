# Step 2 design: the Beacon app

| | |
|---|---|
| Status | Approved by the owner, 8 November 2026. PR 1 built (screens and test alert), with the emulator checks' defects D1 to D5 fixed. PR 2 built (the alert engine and the "Finish setup" card), with its emulator checks' defects D1 and D2 fixed. PR 3 built (the widget and the timed pause), with its emulator checks' defects D1 to D3 fixed and re-checked on the emulator, and its final review's follow-up R1 (the widget following a 12/24-hour, time-zone or language change) fixed and re-checked on the emulator. PR 3 and the launcher icon (section 8) merged on 9 November 2026 (yanivil/beacon#8 and #9). PR 4 (PR 2's last two carry-overs: the camera light's refusal reported to the alert controller, section 7, and the battery steps for each Android version, section 9) merged on 9 November 2026 (yanivil/beacon#10). The lint clean-up (sections 5, 10, 11, 12 and 16) merged on 10 November 2026 (yanivil/beacon#11). The dependency versions (Gradle 9.9.1 and Kotlin 2.4.21, sections 4 and 16) merged on 11 November 2026 (yanivil/beacon#12). compileSdk 37, with Compose 1.12, Lifecycle 2.11 and Core KTX 1.19 (sections 4 and 16), merged on 11 November 2026 (yanivil/beacon#13). The first phone checks ([`docs/checks/2026-11-phone-checks.md`](../checks/2026-11-phone-checks.md)) merged on 11 November 2026 (yanivil/beacon#14). P1, English text laid out right to left on a Hebrew phone (section 12), merged on 11 November 2026 (yanivil/beacon#15). P3, the owner's own WhatsApp reply flashing (section 6), merged on 11 November 2026 (yanivil/beacon#16). P2, the Xiaomi battery step (section 9), merged on 11 November 2026 (yanivil/beacon#17). P4, the widget's text size, by the owner's choice (sections 3 and 10), merged on 11 November 2026 (yanivil/beacon#18). The owner's decision that a real alert flashes only with the screen off or locked (sections 6 and 7) merged on 11 November 2026 (yanivil/beacon#19). P5, a Background autostart row on Xiaomi phones (sections 9 and 13), merged on 11 November 2026 (yanivil/beacon#20). Home's ON card now says it flashes when the phone isn't in use (section 8), merged on 11 November 2026 (yanivil/beacon#21). The owner's cable-out check ran SMS and WhatsApp alerts on battery in Doze on the test phone, on 11 November 2026 ([`docs/checks/2026-11-alerts-on-battery.md`](../checks/2026-11-alerts-on-battery.md)). A second one that evening ran the widget's 1-hour pause on battery, and alerts and the widget came back on by themselves ([`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md)); the resume alarm firing in Doze is still not tested there. |
| Date | 8 November 2026 |
| Approved | The technical design in chat, with the package name `dev.beacon`, then this written spec, both on 8 November 2026 |
| Plan for PR 1 | [`docs/plans/2026-11-08-step2-pr1-screens.md`](../plans/2026-11-08-step2-pr1-screens.md) |
| Plan for PR 2 | [`docs/plans/2026-11-09-step2-pr2-alert-engine.md`](../plans/2026-11-09-step2-pr2-alert-engine.md) |
| Plan for PR 3 | [`docs/plans/2026-11-09-step2-pr3-widget.md`](../plans/2026-11-09-step2-pr3-widget.md) |
| Plan for PR 4 | [`docs/plans/2026-11-09-step2-pr4-carryovers.md`](../plans/2026-11-09-step2-pr4-carryovers.md) |
| Builds on | [`docs/feasibility.md`](../feasibility.md) (Step 1, approved when PR #4 merged) |
| Design reference | https://claude.ai/code/artifact/57db235d-b0b1-4b3d-af5c-bdee1677e1f2 (version read: `1794128372-2bae`) |

This spec describes what Step 2 builds and how. It is the input for the
implementation plan. Where it says "the design", it means the HTML prototype
above. The prototype is a reference for layout, wording, colours, sizes and
behaviour. It is never ported as web code.

## 1. Goal

Step 2 turns the Step 1 findings into the real app: the Home and Alert
settings screens, the Test alert overlay, the home-screen widget, and the
engine behind them, which notices messages and runs the alert.

### Done means

1. Beacon installs on the test phone (Redmi 15C 5G, Android 16) and opens on
   Home.
2. Once the person has finished setup, an SMS or WhatsApp message makes the
   whole screen blink and the camera light flash. This works with the phone
   locked, asleep and on battery, using the colour, speed and length chosen
   in Alert settings.
3. Beacon does not flash for WhatsApp's backup progress, group summaries,
   repeated updates of the same message, or the person's own replies.
4. The widget shows ON or PAUSED. One tap pauses alerts for 1 hour, and they
   come back on by themselves.
5. Every screen passes the accessibility checks in section 11.
6. Telegram is built, and reported as "Not tested".

## 2. Scope

**In Step 2:**

- Home, Alert settings, the Test alert overlay and the bottom tab bar.
- The real alert screen (not in the design; see section 8).
- A "Finish setup" card on Home (not in the design; see section 9).
- The engine:
  - the notification listener;
  - the message rules;
  - the alert controller;
  - the camera light;
  - saved settings;
  - pause and resume.
- The home-screen widget.

**Not in Step 2:**

- Phone calls and WhatsApp video calls (owner's decision, 8 November 2026).
- The "Add other apps" button (owner's decision, 8 November 2026).
- Publishing on Google Play, release signing, store listing.
- A Hebrew translation. The layouts stay right-to-left ready (section 12).
- Any network use, analytics or crash reporting.

## 3. Where the rules and the design disagree

The project rules win. Each conflict and its resolution:

| # | Design | Resolution |
|---|---|---|
| 1 | The prototype is titled "Visual Alerts" and Home's H1 says "Visual Alerts". | The app, launcher label, Home H1 and widget-picker name are **Beacon**. The card heading "Visual alerts" on Home and on the widget stays, because it names the feature, not the app. |
| 2 | Home lists Phone Calls and WhatsApp video calls. | Removed (scope). Home lists Texts (SMS), WhatsApp messages and Telegram. |
| 3 | "…flash when a call or message arrives." and "After a call or message arrives." | "…flash when a message arrives." and "After a message arrives." |
| 4 | A dashed "Add other apps" button. | Omitted for now (owner, 8 November 2026). |
| 5 | Loads Atkinson Hyperlegible Next from Google Fonts over the web. | The font is bundled in the app (OFL licence), so nothing is downloaded. The widget uses the system font, because a home-screen widget can't load a bundled font. |
| 6 | Sizes in CSS px on a 390 px wide frame. | 1 px = 1 dp for sizes and 1 sp for text. All text is in sp. |
| 7 | App rows use generic placeholder glyphs. The design's own note says a real build shows each app's launcher icon. | Beacon shows the app's own launcher icon when that app is installed, and the design's glyph otherwise. The icon is decorative: the row's name is its label. |
| 8 | Options use `aria-pressed` toggle buttons. Tabs use `aria-current`. | Rules require platform roles: options are **RadioButton** in a group that TalkBack reads as a list ("2 of 4"), tabs are **Tab**, and the app rows are **Switch** (section 11). |
| 9 | The Test alert button and tab bar are always fixed at the bottom. | At the largest font size, a fixed area can fill most of the screen. The tab bar always stays fixed. The Test alert button stays fixed only while the fixed area takes at most a third of the window height. Otherwise it becomes the last item of the scrolling content. |
| 10 | Duration and colour grids always have 2 columns. | At large font sizes two columns would break words. A grid has 2 columns only when the available width divided by the font scale is at least 300 dp. Otherwise it has 1 column. |
| 11 | The widget has fixed text sizes in a fixed box. | A widget can't scroll. Text stays in sp, and lines are left out in a fixed order when they don't fit (section 10). Every line stays in the widget's spoken label. |
| 12 | The paused card on Home has no resume time. | When the pause came from the widget, the card also says "Back ON by itself at HH:MM." |
| 13 | Step 1's report says the design's "VISUAL ALERTS" and "TEST ALERT" headings are 20 px uppercase. | The version read for this spec already uses 28 px bold, mixed case, for both. Nothing to change. |
| 14 | A disabled Shorter or Longer button is drawn only in grey. | The rules say state is never shown by colour alone, and white against `#AEAEB6` is only about 2.2:1. A disabled button also draws its border dashed. |
| 15 | The ON/OFF pill always sits beside the app's name. | At the largest font that broke words into two-letter lines. The pill wraps under the text when a word would otherwise break. |
| 16 | The per-app colour card's Change/Done button always sits beside the name. | At large fonts the names broke mid-word. The button sits under the name when the available width divided by the font scale is under 250 dp. |
| 17 | The PAUSED card has the "Visual alerts" heading; section 8's list had left it out. | Both cards have the heading. |
| 18 | The widget's state word is 64 sp (ON) and 52 sp (PAUSED). | 48 and 40 sp, with 12 dp of padding instead of 16: at the owner's text size the design's sizes hid the resume time at 4 × 2 on the test phone (the phone checks' P4). The owner chose these values from a preview on the phone, 11 November 2026. |
| 19 | The design doesn't say whether an alert flashes while the phone is in use. | It doesn't: a real alert flashes only when the screen is off or the lock screen is showing (the owner's decision, 11 November 2026; section 6, rule 7). Home's ON card adds "and you aren’t using the phone" to its line (owner, 11 November 2026). |

## 4. Architecture

### Approach

One Gradle app module, `app`, with packages by job. This keeps the build
simple, and each unit can be tested on its own. Multi-module builds and
SharedPreferences were considered and rejected. Multi-module costs too much
build setup for an app this size. SharedPreferences makes the app and the
widget harder to keep in sync.

The Step 1 spike in `spikes/feasibility/` stays a separate throwaway project.
Code is carried over from it by copying and cleaning up, never by depending
on it.

| Package | Holds |
|---|---|
| `dev.beacon` | `BeaconApp` (the `Application`), which creates the shared repository and alert controller once per process. No dependency-injection library. |
| `dev.beacon.settings` | `BeaconSettings`, `SettingsRepository`, `BlinkSpeed`, `FlashColour`, `DurationChoice`, `CustomLength`, `Pause` |
| `dev.beacon.alert` | `MessageSource`, `NotificationFacts`, `MessageClassifier`, `BeaconListener`, `AlertController`, `BlinkClock`, `TorchBlinker`, `AlertActivity` |
| `dev.beacon.setup` | `SetupChecker` and the settings-page intents |
| `dev.beacon.ui` | `MainActivity`, theme tokens and type, shared components, Home, Alert settings, Test overlay, `BlinkScreen` |
| `dev.beacon.widget` | `BeaconWidget`, its receiver, the tap action and `WidgetRefresher` |

### Stack

| Part | Choice |
|---|---|
| Language and UI | Kotlin, Jetpack Compose (Foundation and UI). Beacon draws its own components. Material 3 is not used, so there is no dynamic colour and no Material sizing. |
| State | One `ViewModel` for `MainActivity`, exposing `StateFlow`s |
| Saved settings | Jetpack DataStore (Preferences) |
| Widget | Jetpack Glance (`glance-appwidget`) |
| Navigation | None: two tabs switched by a saved state value |
| Build | Gradle wrapper, AGP 9.5.1 with its built-in Kotlin, the Compose compiler plugin matching that Kotlin, and a version catalog. Library versions are pinned to exact releases looked up on Google Maven when the plan is written. Gradle 9.8.2 and Kotlin 2.2.10 (AGP's own) until the dependency-version PR, which moved them to Gradle 9.9.1 and Kotlin 2.4.21: the Compose compiler plugin at 2.4.21 raises AGP's built-in Kotlin to the same version, so the two still match. |
| Library versions | Compose BOM 2026.09.00 (Compose 1.12.1), core-ktx 1.19.1 and lifecycle 2.11.0, since the compileSdk 37 PR. From PR 1 until then: Compose BOM 2026.06.01 (Compose 1.11.4), core-ktx 1.18.0 and lifecycle 2.10.0, because the newer ones need compileSdk 37 (their AAR metadata says `minCompileSdk=37`) and the project rules fixed compileSdk at 36. |
| Ripple | material-ripple has no `ripple()` function (1.11.4, and 1.12.1 has none either), so `BeaconTheme` builds its white press ripple with the public `createRippleModifierNode`. |
| Unit tests | On JDK 21, Robolectric needs the JVM flag `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`, which the app's test tasks set. |
| SDK | minSdk 29, compileSdk 37 (36 until the owner raised it on 11 November 2026), targetSdk 36, applicationId `dev.beacon`, versionName `0.1.0` |

### Data flow

```text
message arrives
  → BeaconListener.onNotificationPosted
  → NotificationFacts.from(notification)        structure only, never text
  → MessageClassifier.decide(facts, settings, seen)
  → Alert(source)  or  Ignore(reason)
  → AlertController.start(request)               one at a time
  → BlinkClock fixes the start time and length
  ├→ AlertActivity draws BlinkScreen from the clock
  └→ TorchBlinker flashes the light from the same clock
  → ends: timer, Stop, Back, Home, screen off, or Beacon opened
```

## 5. Settings

### Model

```kotlin
data class BeaconSettings(
    val pause: Pause,                          // None, Until(epochMillis), Indefinite
    val apps: Map<MessageSource, AppSetting>,  // AppSetting(enabled, colour)
    val colourMode: ColourMode,                // SAME_FOR_ALL, PER_APP
    val globalColour: FlashColour,
    val duration: DurationChoice,              // ONE_MINUTE, FIVE_MINUTES, CUSTOM, UNTIL_STOPPED
    val customLength: CustomLength,            // kept even when duration isn't CUSTOM
    val speed: BlinkSpeed,
)
```

- **`BlinkSpeed`** is an enum: `SLOW(30)`, `MEDIUM(60)` and `FAST(90)`
  blinks a minute. Half a period is `30_000 / bpm` ms. Its constructor
  refuses anything above 180 a minute, which is 3 flashes a second. Code that
  blinks takes a `BlinkSpeed`, never a number.
- **`FlashColour`** is an enum:
  - Red `#FF3B30`
  - Yellow `#FFD60A`
  - Blue `#0A84FF`
  - Green `#30D158`
  - White `#FFFFFF`

  Each colour has its name in `strings.xml`.
- **`CustomLength`** holds one of the fixed steps: 1, 2, 3, 5, 10, 15, 20,
  30, 45 or 60 minutes. Shorter and Longer move one step. An unknown stored
  value becomes 10 minutes, as in the design.
- An app's effective colour is its own colour in `PER_APP` mode, and the
  global colour otherwise.

### Defaults

| Setting | Default |
|---|---|
| Alerts | ON (`Pause.None`) |
| Texts (SMS) | ON, Yellow |
| WhatsApp messages | ON, Green |
| Telegram | OFF, Blue |
| Screen flash colour | Same colour for all apps, Yellow |
| How long to flash | 1 min (custom length 10 min) |
| Blink speed | Medium |

### Storage

- **Where:** one DataStore Preferences file. Enums are stored by name.
- **Unreadable data:** an unknown name or a missing key falls back to that
  setting's default. A corrupt file is replaced with defaults through
  DataStore's corruption handler.
- **Access:** all reads and writes go through `SettingsRepository`. It
  exposes `settings: Flow<BeaconSettings>` and one function per change.
- **Backup:** Android backup is turned off (`allowBackup="false"`, and data
  extraction rules exclude everything), so nothing leaves the phone. On
  Android 10 and 11, `allowBackup="false"` alone stops every backup and
  transfer, so there are no `fullBackupContent` rules for them. Lint's
  `DataExtractionRules` warning, which asks for them, is ignored on the
  `<application>` element for that reason.

### Pause

| Action | New pause |
|---|---|
| Home: "Pause all alerts" | `Indefinite` |
| Home: "Turn alerts back ON" | `None` |
| Widget tap while ON | `Until(now + 1 hour)` |
| Widget tap while PAUSED, either kind | `None` |

- **Alert check:** an alert checks `isPausedAt(now)`. `Until(t)` counts as
  paused only while `now < t`. So alerts come back on time even if nothing
  else runs. A widget tap checks the same way: a tap on an `Until` that
  has already passed pauses for another hour.
- **One place for changes:** every pause change goes through
  `PauseCoordinator`: Home's buttons, the widget's tap, the resume alarm,
  Beacon coming to the front, the screen turning on, and a time, time-zone
  or language change. Each change ends the same way: save the pause, set
  the resume alarm for a timed pause or cancel it for any other, then
  redraw the widget if the saved pause differs from the one stored before.
  The alarm is set again even when nothing changed, which also re-arms one
  lost to a restart. A catch-up with nothing to clean up redraws nothing,
  because each redraw starts a Glance drawing session of about 45 seconds
  under a wake lock (ruling F3). The one exception is the catch-up after a
  time, time-zone or language change (follow-up R1): it also redraws while
  a timed pause is running, the only state that shows a time, and in every
  state after a language change, because every state shows words; one
  redraw at most. A time change alone doesn't redraw ON or an open-ended
  pause, because automatic time sends TIME_SET whenever it corrects the
  clock. A lock keeps the alarm in the order of the saves.
- **A started change finishes:** once a change has begun, it runs to the
  end even if the screen that asked for it closes at once. Leaving Beacon
  right after "Pause all alerts" or "Turn alerts back ON" can't leave the
  alarm or the widget behind the saved pause.
- **Atomic update:** `SettingsRepository.updatePause(transform)` reads the
  stored pause and writes the new one in a single DataStore edit, so a
  widget tap is decided against the very pause it replaces. Two quick taps
  give a 1-hour pause and then ON, never 2 hours (checked on the emulator
  with taps 41 ms apart).
- **Clean-up:** an expired `Until` is written back as `None` by the
  coordinator's catch-up, which runs when the resume alarm fires, when
  Beacon comes to the front, when the screen turns on (while the
  listener is bound) and after a time, time-zone or language change.
- **Display:** the resume time is shown as HH:MM in the phone's 12- or
  24-hour format ("5:29 PM", or "17:50" with the 24-hour setting, on the
  emulator). Home reads the format each time it draws. The widget follows
  a change of the 12/24-hour setting, the time zone or the language by
  itself, through `TimeChangeReceiver` (follow-up R1): Android sends
  TIME_SET (also for the 12/24-hour switch), TIMEZONE_CHANGED and
  LOCALE_CHANGED, the receiver stores the widget's key, so a drawing
  session that is still running draws again, and then runs the catch-up
  above. On the emulator the widget showed the 24-hour time about 3
  seconds after the switch in Settings, with no screen off and on and
  Beacon not opened, and moved its time two hours back when the zone
  changed from Israel to London, each time in a new drawing session. A
  zone change inside a running session wasn't tried.

## 6. Message detection

### Sources

| Source | Matches notifications from | Status |
|---|---|---|
| Texts (SMS) | The phone's default SMS app, read with `Telephony.Sms.getDefaultSmsPackage` each time, so changing the default app is followed | Tested on Redmi 15C 5G, Android 16 (Google Messages) |
| WhatsApp messages | `com.whatsapp` | Tested on Redmi 15C 5G, Android 16 |
| WhatsApp messages | `com.whatsapp.w4b` (WhatsApp Business) | Not tested |
| Telegram | `org.telegram.messenger`, `org.telegram.messenger.web` | Not tested |

### Facts

`NotificationFacts` holds only what the rules need, taken from the
notification. It never holds titles, names or message text:

- package
- key
- ongoing
- group summary
- category
- whether it has a MessagingStyle layout
- the newest message's time
- whether that newest message was sent by the phone's owner
- the notification's `when`

### Rules

`MessageClassifier` is plain Kotlin with no Android types, so it is fully
unit-tested. It returns `Alert(source)` only when every check passes, and
otherwise the first reason that fails:

1. The package belongs to a source in the table above. Otherwise `NOT_A_SOURCE`.
2. The notification isn't ongoing. Otherwise `ONGOING`. This rules out
   WhatsApp's backup progress.
3. It isn't a group summary. Otherwise `GROUP_SUMMARY`.
4. It is a message: it has a MessagingStyle layout, or category `msg`.
   Otherwise `NOT_A_MESSAGE`.
5. Its newest message time is later than the last one already handled for
   this notification key. A key with no record passes. The newest message
   time is the MessagingStyle time, else `when`. Otherwise `ALREADY_HANDLED`.
6. With MessagingStyle, the newest message wasn't sent by the owner, for
   example a reply typed into the notification. Otherwise `OWN_MESSAGE`.
   A message is the owner's when it has no sender (MessagingStyle's
   convention), or when its sender's key is the owner's key (an empty key
   is no key). For WhatsApp only, when neither the sender nor the owner has
   a key, it is the owner's when the sender's name is the owner's non-blank
   name in the notification: WhatsApp marks the owner's reply that way (the
   phone checks' P3). Other apps keep the key rule alone, because a wrong
   match silently drops a real alert.
7. The phone isn't in use: its screen is off, or it is on and showing the
   lock screen. Otherwise `IN_USE`. The owner decided this on 11 November
   2026: while the person is using the phone, the message app's own banner
   is the cue, and a flash only disrupts.
8. Alerts aren't paused at this moment. Otherwise `PAUSED`.
9. That source's switch is ON. Otherwise `APP_OFF`.

### The "already handled" record

- **What it holds:** the newest message time for each notification key, kept
  in memory.
- **When it's updated:** for every notification that passes rules 1 to 4,
  whatever the final result. So a message that arrives while alerts are
  paused, while its app is OFF, or during another alert never flashes later
  when the same notification is posted again.
- **When it's filled:** when the listener connects, it records every
  notification already showing. That way, a re-post of an old notification
  after a restart doesn't flash.
- **When an entry goes:** when its notification is removed.
- **Busy alerts:** a message that arrives during an alert is recorded as
  handled but doesn't start another alert. This is Step 1 requirement 3.

### Listener

`BeaconListener` (`NotificationListenerService`):

- Builds the facts, waits for the saved settings, asks the classifier, and
  hands an `Alert` to the controller. A message that arrives before the
  saved settings have loaded waits for them; it is never decided with the
  defaults, which would flash while the person has paused alerts.
- While connected, it also runs the pause catch-up when the screen turns
  on. The widget is redrawn only when that catch-up changed the saved
  pause (section 10).
- **Reconnect:** when Beacon is opened, `MainActivity` checks whether access
  is granted but the listener isn't connected. If so, it turns the listener
  component off and on again. This is the Step 1 fix for HyperOS refusing to
  rebind with Autostart off, and Step 1 requirement 5. The check runs at
  once each time Beacon comes to the front, with no delay, so just after a
  cold start it may switch the listener off and on once for nothing.

## 7. The alert

A real alert starts only for a message that passed every rule in section 6,
so never while the phone is in use (rule 7). The phone is in use when
`PowerManager.isInteractive` is true and `KeyguardManager.isKeyguardLocked`
is false: an always-on display counts as off, and a phone with no lock
screen is in use whenever its screen is on. It is read when a message's
decision reaches rule 7, and if either service can't be read, the phone
counts as not in use, so the alert flashes. The test alert starts from its
button and isn't affected.

### AlertController

There is one per process. Its state is a `StateFlow<AlertState>`: `Idle` or
`Running(request, clock)`. Screens draw from this state and never keep their
own timer, so rebuilding a screen can't restart or lengthen an alert (Step 1
requirement 4).

`start(request)`:

1. **Busy check.** If an alert is running, a real alert replaces a running
   *test*: the test is ended first, exactly as Stop would end it, and the
   real alert starts at once, so the wake lock stays held throughout.
   Otherwise the new request is dropped.
2. **Clock.** Create a `BlinkClock`: the start time (`SystemClock.uptimeMillis`),
   the half period from `BlinkSpeed`, and the end time. "Until I stop it"
   has no end time.
3. **Wake lock.** Hold a partial wake lock for the alert's length plus
   10 seconds. "Until I stop it" holds it for 60 minutes plus 10 seconds and
   renews it every 60 minutes while the alert runs. The 10-second margin
   makes sure the renewal runs before the old lock lapses; if it is wrong,
   it costs at most 10 seconds of extra CPU time.
4. **Camera light.** Start `TorchBlinker` on the same clock, unless the
   thermal status is already Severe or worse.
5. **Screen.** For a real alert, start `AlertActivity` directly, but only if
   "Display over other apps" is granted (Step 1 requirement 6). Without it,
   Android would block the screen, so the alert is light-only.
6. **Thermal.** Listen to thermal status. At Severe or worse, stop the light
   and keep the screen blinking. The running state's `lightFlashing` then
   turns false, so the test overlay's camera line changes to "Off while the
   phone is hot" (section 8). If the system refuses the thermal listener,
   the controller checks the thermal status itself every 30 seconds while
   the light flashes, so at worst the light runs 30 seconds after the phone
   turns Severe.
7. **A refused light.** When the light refuses a command mid-alert (for
   example another app takes the camera), `TorchBlinker` ends its run,
   makes one attempt to leave the light off, then reports the refusal. The
   controller handles it like the heat stop: `lightFlashing` turns false,
   the 30-second heat poll stops, and the screen blink and the wake lock
   carry on. The running state's `lightRefused` turns true, so the test
   overlay's camera line says "Off while another app uses the camera"
   instead of the heat line (section 8). The heat listener stays
   registered until the alert ends, as after the heat stop. A report that
   arrives after its alert ended or was replaced changes nothing, the same
   guard as the heat callback's (PR 4).

An alert ends at the first of these:

- its end time;
- Stop or Back on the alert screen;
- the Home button on the alert screen (`onUserLeaveHint`), but only while
  the screen is on. The alert screen is started with
  `FLAG_ACTIVITY_NO_USER_ACTION`, because a message, not the person, opens
  it. Without that flag, Android paused a window that opened while the
  display was asleep as "user leaving" (`userLeaving=true,sleep`), which
  reached `onUserLeaveHint` and ended every alert that arrived with the
  screen off (PR 2's D2). A person can only press Home on a screen that is
  on, so a call while it is off is never Home;
- the screen turning off (the power button) while the alert screen shows.
  `AlertActivity` listens for the screen turning off; that screen keeps the
  display on, so the screen going off means the power button. A light-only
  real alert has no alert screen, and the screen going off doesn't end it:
  if it did, an ordinary screen timeout would end it too. It ends on its
  timer or when Beacon is opened;
- for a real alert, Beacon's main screen (`MainActivity`) coming to the
  front. This is how a light-only alert can be stopped;
- for a test, Beacon's main screen stopping (Home, another app, the screen
  turning off), but not when it is only recreated for a rotation or a font
  change, so the countdown carries on.

Ending always turns the light off, releases the wake lock and closes the
alert screen:

- **Light, then wake lock.** The light is switched on its own thread, and
  ending only queues its off there. The wake lock is released after that
  off has run, so the phone can't fall asleep with the light on. If a new
  alert has started in the meantime, the late release does nothing.
- **The alert screen closes itself** whenever no real alert is running. It
  watches the controller's state even while it is in the background, so it
  also closes after an ending that happened elsewhere (the timer, Beacon
  opened), and a stale start finds nothing to show and closes at once. Its
  own endings (Stop, Back, Home, screen off) act only while a real alert
  is running, so a window left behind can never end a later test.

All these endings are built in PR 2.

### The two halves of the blink

- **BlinkClock:** works out the phase from the clock. The colour shows in
  even half periods and black in odd ones. The screen and the light both use
  it, so they stay in step.
- **TorchBlinker:** carried over from the spike.
  - It runs on its own `HandlerThread`, so a stalled camera call can't delay
    the screen.
  - It schedules each flip at `start + step × half period`, so delays don't
    add up.
  - It never replays missed flips. After a stall, a late start or a restart
    it jumps to the clock's current phase, so it never goes past 3 flashes a
    second.
  - A finite alert always ends with the light off.
  - It prefers the back camera's light.
  - If the light is unavailable or in use, it stops quietly for that alert.
    A refusal mid-alert is reported once, after the blinker's last attempt
    to leave the light off; the end of an alert and Stop never report one
    (PR 4).
- **BlinkScreen** (Compose):
  - It reads the clock once per frame.
  - It gives its content the frame time (`() -> Long`), so the test
    overlay's countdown and the background share one clock.
  - It flips at the first frame after each half-period boundary, so it is
    never faster than the preset.
  - The colour fills the window except for black strips behind the system
    bars: the status bar, the navigation bar, and the navigation bar at the
    side in landscape (the system-bar insets). The bars stay visible with
    white icons, 21:1 on black (section 11). The cards keep clear of the
    bars and of any camera cutout.

### Safety

Fast 90 is 1.5 flashes a second, half of WCAG 2.3.1's limit. A unit test
checks that every `BlinkSpeed` stays at or under 3 flashes a second.

## 8. Screens

The layouts follow the design, with section 3's changes. Sizes are
dp and sp, 1:1 with the design's px.

### Shared bottom area

- **Test alert button:**
  - Label "Test alert" at 26 sp, weight 800, plus "(3 sec)" at 20 sp.
  - White background, `#0B0B0D` text, a bolt icon, minimum height 76 dp.
  - Fixed above the tabs, except in the case in section 3, row 9.
- **Tab bar:**
  - Background `#141418`, with exactly 2 tabs: "Home" and "Alert settings".
  - Each tab is at least 92 dp tall, with an icon and a 20 sp label.
  - The selected tab has a 6 dp yellow top bar, a yellow pill behind a dark
    icon, and weight 800.
  - Each tab opens at its top.
  - Back on Alert settings goes to Home. Back on Home leaves the app.
  - It keeps clear of the navigation bar and a side camera cutout, but not
    of the keyboard, so a text field can never push it up. The Test alert
    strip and the content use the same insets.

### Home

From top to bottom:

1. **H1 "Beacon".** 36 sp, weight 800.
2. **"Finish setup" card**, only when something is missing (section 9).
3. **Alerts card**, in one of two states:
   - **ON:**
     - Yellow, 24 dp radius.
     - The heading "Visual alerts" at 28 sp, then a bolt icon and "ON" at 56 sp.
     - The text "Your screen and camera light will flash when a message
       arrives and you aren’t using the phone." (The design's ends at
       "arrives."; the rest follows the owner's decision, section 6, rule 7.)
     - A black "Pause all alerts" button: white text, pause icon, at least 80 dp.
   - **PAUSED:**
     - `#1B1B20` with a 4 dp white border.
     - The heading "Visual alerts" at 28 sp, as on the ON card.
     - A pause icon with "PAUSED".
     - The text "You will NOT see any flashes until you turn alerts back on."
     - When the pause is timed, the extra line "Back ON by itself at HH:MM."
     - A yellow "Turn alerts back ON" button.
4. **"Flash for these apps".**
   - The heading, with the line "Tap an app to turn it ON or OFF."
   - While paused, a banner with a 3 dp white border: "All alerts are paused
     right now."
   - Then one row per app: Texts (SMS), WhatsApp messages, Telegram.
   - **Each row is a single switch:**
     - At least 100 dp tall, `#1B1B20`, 2 dp `#A1A1AA` border, 20 dp radius.
     - On the start side, the app's icon on a 52 dp `#2A2A31` tile.
     - The name at 24 sp, weight 800.
     - Under the name, "<Colour> flash" with a swatch, or "No flash" when
       the app is OFF.
     - On the end side, a 100 × 60 dp pill with the text ON or OFF and a
       42 dp knob. At the largest font, when a word of the text beside it
       would otherwise break, the pill wraps under the text.
     - ON pill: yellow with dark text. OFF pill: black, with a white border
       and white text, laid out in reverse.
5. **"How alerts look".** A card with three label and value rows:
   - "Flashes for": the duration summary.
   - "Blink speed": the speed name.
   - "Screen color": a swatch and the colour name, or "Different per app".

   Below the rows, a "Change settings" button with a chevron opens the Alert
   settings tab.

### Alert settings

1. **H1 "Alert settings".**
2. **"How long to flash"**, with the line "After a message arrives."
   - Four options, each at least 120 dp tall:
     - "1 min"
     - "5 min"
     - "Custom", with the line "N min"
     - "Until I stop it"
   - **When Custom is chosen, a "Custom length" panel opens:**
     - The value at 40 sp, read aloud when it changes.
     - "Shorter" and "Longer" buttons, at least 76 dp, with − and + icons.
     - At either end, that button is disabled and drawn in `#AEAEB6`, with
       a dashed border, so the state isn't shown by colour alone.
     - The line "From 1 to 60 minutes."
3. **"Blink speed"**, with the line "All three speeds are safe limits that
   protect your camera light and battery."
   - Slow, "30 blinks a minute".
   - Medium, "60 blinks a minute".
   - Fast, "90 blinks a minute", with the note "Highest safe speed".
   - Each option is at least 96 dp tall.
4. **"Screen flash color"**, with the line "Your whole screen blinks this
   color. Each color has its name written on it."
   - Two mode options, each at least 76 dp: "Same color for all apps" and
     "Different color for each app".
   - **Same colour mode:** a swatch grid of the five colours, each at least
     96 dp, with White spanning the full row.
   - **Per-app mode:** one card per app showing its name and current colour,
     and a "Change" button (at least 120 × 64 dp).
     - The button sits beside the name. When the available width divided by
       the font scale is under 250 dp, it sits under the name, full width.
     - The button opens that app's swatch grid, with swatches at least 88 dp.
     - When open, the button reads "Done".
     - Only one app's grid is open at a time.
   - **Every swatch:**
     - The colour name is printed white on a black label.
     - The selected swatch has a dark and white ring, and a check in its
       label.

**Option styling.** A selected option has a yellow fill, dark text and a
check icon (a black circle with a yellow tick). An unselected option is
`#1B1B20` with a `#A1A1AA` border and an empty circle. The state never
depends on colour alone.

### Test alert overlay

- **Start.** It opens over the whole app from the Test alert button and runs
  through `AlertController` as a 3-second test request. The test also runs
  while alerts are paused.
- **Blink.** The background blinks between the flash colour and black at the
  chosen speed. The camera light flashes too.
- **Top card**, black with a white border:
  - "Test alert" (28 sp, weight 800)
  - "This is how your alerts will look" (34 sp, weight 800), announced when
    the overlay opens
- **Countdown:** 3, 2, 1 at 96 sp, with "seconds left" or "second left"
  (plurals).
- **Details card:**

  | Row | Shows |
  |---|---|
  | Color | The colour name. In per-app mode, "Name (App)". |
  | Speed | For example "Medium, 60 a minute" |
  | Camera light | "Flashing too"; "Off while the phone is hot" when the phone has a light but the heat check stopped it (section 7); "Off while another app uses the camera" when the light refused during the test (section 7, PR 4); or "Not on this phone" when there's no light |
  | Real alerts last | The duration summary |

- **Stop button.** White, with a 4 dp black border, at least 96 dp tall, and
  "Stop" at 32 sp.
- **Test colour in per-app mode:** the app whose colour card is open. If no
  card is open, the first app that is ON. If no app is ON, Texts (SMS).
- **Over the app.** Taps on the overlay never reach the screen underneath.
  TalkBack reaches only the overlay, which it knows as the pane "Test
  alert".
- **Keyboard focus.** When the overlay opens, keyboard focus moves to its
  Stop button, even when it was on the Test alert button that started the
  test. Focus can't move into the screen underneath.
- **Back** during a test ends the test and stays on the current tab.
- **Stop and Back end only a test.** If a real message has just replaced
  the test (section 7), they leave the real alert running; it has its own
  screen.
- **One clock.** The countdown reads the same frame time as the blinking
  background (see `BlinkScreen` in section 7).

### The real alert screen

This screen is not in the design. It reuses the test overlay's layout and
`BlinkScreen`.

- **Window behaviour.** `AlertActivity` shows over the lock screen and turns
  the screen on. It keeps the screen on, stays out of recent apps, and runs
  in its own task. The phone stays locked behind it. It closes itself
  whenever no real alert is running (section 7), and a rotation redraws it
  from the same clock.
- **Top card:**
  - "New message" (28 sp, weight 800)
  - The app's name, such as "WhatsApp messages" (34 sp, weight 800),
    announced when the screen opens
- **Window title.** "New message: <app name>", which TalkBack reads when the
  window opens.
- **No message content.** No sender, title or text is ever shown or read
  aloud.
- **Stop button.** The same Stop button as the test overlay. It is the first
  thing TalkBack focuses.
- **No countdown.** A real alert lasts a minute or more, so the countdown
  card is left out.

### Launcher icon

The design has no app icon or logo (version read: `1794248815-83ec`). The
owner chose this one from three ideas on 9 November 2026: the widget's ON
card, as an icon.

- **Look:** the design's bolt (`M13 2L3 14h9l-1 8 10-12h-9l1-8z`, filled
  and stroked 2, round joins) in `#0B0B0D` on `#FFD60A`, 13.93:1.
- **Adaptive icon:** `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`
  in `res/mipmap-anydpi/`. Both use the same three vector layers in
  `res/drawable/`: `ic_launcher_background` (a full 108 dp yellow square),
  `ic_launcher_foreground` (the bolt) and `ic_launcher_monochrome` (the same
  bolt, opaque white). minSdk is 29, so there is no `-v26` folder and no
  bitmap fallback. The manifest sets `android:icon` and `android:roundIcon`.
- **Size:** the 24-unit bolt is scaled by 2 and moved 30 dp, so it is 44 dp
  tall and centred on the 108 dp layer. Its farthest corner, stroke
  included, is 22 dp from the centre: inside the 33 dp radius of the 66 dp
  safe zone that no launcher mask clips.
- **Themed icon:** on Android 13 and later, when themed icons are on, the
  launcher tints the monochrome layer with the wallpaper's colours.
- **Widget picker:** the widget sets no preview image, so its picker entry
  shows this icon.

## 9. Finish setup

### The card

- **Where:** a card at the top of Home, shown only while at least one check
  is OFF.
- **Look:** `#1B1B20` with a 4 dp white border, so it stands out like the
  paused card.
- **Heading:** "Finish setup", with the line "Beacon needs these to flash
  for your messages."
- **Refresh:** the checks run again whenever Home comes back into view, for
  example after the person returns from Settings.

### Each row

- Its name and a plain-words reason.
- A status pill with the text ON or OFF, styled like the app-row pills.
- An "Open settings" button, at least 64 dp, whose spoken label names the
  row.

| Row | Reason shown | Checked with | Button opens |
|---|---|---|---|
| Notification access | So Beacon knows when a message arrives. | `NotificationManagerCompat.getEnabledListenerPackages` | Android 11+: Beacon's own notification-access page. Android 10: the notification-access list. |
| Display over other apps | So the screen can flash over other apps and the lock screen. | `Settings.canDrawOverlays` | Beacon asks for its own "Display over other apps" page. Android 11 and later ignore the app's address for this page and open the list of apps, where the person taps Beacon (seen on the Android 16 emulator). |
| Battery: No restrictions | So alerts still arrive while the phone sleeps. | `PowerManager.isIgnoringBatteryOptimizations` (Step 1 showed HyperOS reports "No restrictions" this way) | Beacon's App info page, with steps in the row, above the button. On Xiaomi phones: "Tap Power (or Battery), then No restrictions." On other phones the steps follow the Android version (see "The battery steps" below). |
| Background autostart (Xiaomi only) | So Beacon works again by itself after the phone restarts. | Xiaomi's private app-op 10008, read with the hidden `AppOpsManager.checkOpNoThrow` by reflection: allowed is ON, ignored is OFF, anything else or a failed read is unknown. An unknown row shows no pill and doesn't keep the card open, so on such a phone nothing prompts for Autostart once the other three are on: a known limit. The call is on Android's "unsupported" list, allowed on Android 16 (the test phone logged it), and a later Android may block it. A phone made by Xiaomi without HyperOS's app-op gets the unknown row, and its button falls back to App info. | HyperOS's "Background autostart" list (action `miui.intent.action.OP_AUTO_START`), with the steps "Find Beacon in the list and turn its switch on." Without it, HyperOS keeps Beacon's listener unbound after a restart until Beacon is opened (the phone checks' P5). |

### The battery steps

Beacon can only open its App info page for the battery setting (ruling 3
of PR 4's plan), and stock Android names the way from there to
"Unrestricted" differently in different versions. So phones other than
Xiaomi get the steps for their version. `batteryStepsRes(manufacturer,
sdkInt)` in `SetupCard.kt`, a pure function, chooses them from
`Build.MANUFACTURER` and `Build.VERSION.SDK_INT`, which Home reads once.
Each names the labels in the order the person taps them.

| Android (API) | Steps shown | Where the words come from | Label |
|---|---|---|---|
| 14 and later (34 and up) | "Tap App battery usage. Tap the words Allow background usage, not its switch. If the next page’s switch is off, turn it on. Then tap Unrestricted." | Read on the emulator's screens with `uiautomator dump`, then followed word for word from "Optimized" and from "Restricted" (PR 4's checks). Android 14 and 15 from Android's Settings source. | Tested on emulator beacon-api36, Android 16 (Android 14 and 15: Not tested) |
| 13 (33) | "Tap App battery usage, then Unrestricted." | Android's Settings source | Not tested |
| 12 and 12L (31 and 32) | "Tap Battery, then Unrestricted." (the steps PR 2 showed on every other phone) | Android's Settings source | Not tested |
| 10 and 11 (29 and 30) | "Tap Advanced, then Battery, then Battery optimization. Choose All apps, tap Beacon, then Don’t optimize, then Done." | Android's Settings source | Not tested |
| Xiaomi, any version | "Tap Power (or Battery), then No restrictions." | HyperOS's Security app 12.8.6, copied off the test phone: the App info row is "Power" there ("חשמל", as the phone showed), and the app carries an older title for it, "Battery"; the option is "No restrictions" (the phone checks' P2) | Not tested |

- **Android's Settings source:** the App info and battery-page layouts and
  their strings in AOSP's `platform/packages/apps/Settings`, at each
  version's tagged releases.
- **Versions that changed in an update:** Android 13 renamed the App info
  row from "Battery" to "App battery usage" in an early update, and
  Android 14 moved "Unrestricted" behind "Allow background usage" in a
  2024 update. Beacon can't tell these builds apart by `SDK_INT`, so it
  shows each version's last wording. On an Android 13 or 14 phone that
  hasn't had the update, one label won't match the screen.
- **Android 10 and 11:** App info shows only its first six rows and puts
  the rest under "Advanced"; whether Battery is among them depends on the phone. The
  Battery page has no "Unrestricted" choice. Its "Battery optimization"
  row opens the list of apps that are "Not optimized", so the person
  switches the list to "All apps" before Beacon shows.
- **"Allow background usage" is two targets** on Android 14 and later: its
  words open the page with "Unrestricted", while the switch at its end
  turns background use off and makes Beacon "Restricted" (PR 4's checks,
  O2). So the steps name the words and say "not its switch". When
  background use is already off, the words still open the next page, but
  its own "Allow background usage" switch is off and "Optimized" and
  "Unrestricted" are greyed out until it is turned on; the steps say to
  turn it on. Following them word for word, from "Optimized" and from
  "Restricted", the Battery row ended ON both times (PR 4's fix round 0).
- **Android 12 and 13 have no such switch.** Their battery page lists
  "Unrestricted", "Optimized" and "Restricted" as three radio buttons
  (Android's Settings source), so tapping "Unrestricted" works from any of
  them, and their steps stay as they are. Not tested.
- **Android 10 and 11 have no switch either.** Their "Background
  restriction" row, just above "Battery optimization", asks before it
  restricts anything. Not tested.
- **Other makers** may name all of these differently; only Xiaomi has its
  own steps.
- At font 2.0 the longest steps (Android 14 and later's, 13 lines on the
  emulator, taller than the screen, so the page scrolls) wrap with nothing
  cut off, on the emulator and in Robolectric; so do Android 10's in
  Robolectric.

### Not used

- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Google Play restricts it).
- `POST_NOTIFICATIONS` (Beacon posts no notifications of its own).
- `USE_FULL_SCREEN_INTENT`, foreground services, and the SMS, call-log or
  camera permissions.
- `RECEIVE_BOOT_COMPLETED` and the exact-alarm permissions
  (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`): the resume alarm is inexact
  and isn't set again after a restart (section 13).

Beacon itself asks only for `SYSTEM_ALERT_WINDOW` and `WAKE_LOCK`. The
listener service is protected by `BIND_NOTIFICATION_LISTENER_SERVICE`. The
merged manifest also has `dev.beacon.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`:
AndroidX core 1.18's own manifest declares and uses it, so that
`ContextCompat.registerReceiver` can keep a receiver private on Android
versions before 13. Its protection level is signature, so it grants nothing
to other apps and the person is never asked for it.

Glance draws the widget through WorkManager, whose own manifest adds
`FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED` and `ACCESS_NETWORK_STATE` and
declares its foreground service, `androidx.work.impl.foreground.SystemForegroundService`.
Glance only runs short drawing sessions, never a foreground worker or a
network constraint, so Beacon's manifest removes all four with
`tools:node="remove"`. The service's removal entry also tells lint to ignore
`MissingClass`, because the class is declared only in WorkManager's own
manifest. The merged manifest's `<uses-permission>` set is then
exactly `WAKE_LOCK`, `SYSTEM_ALERT_WINDOW` and the AndroidX signature
permission above, with no `INTERNET`. PR 3 adds three receivers: the
widget's `BeaconWidgetReceiver`, exported so the launcher can reach it;
`ResumeReceiver`, not exported, which only the resume alarm's explicit
pending intent reaches; and `TimeChangeReceiver`, not exported, for the
system's TIME_SET, TIMEZONE_CHANGED and LOCALE_CHANGED (follow-up R1).
Those three are on Android's list of implicit broadcasts that a manifest
receiver still gets on Android 8 and later, a receiver that isn't
exported still gets system broadcasts, and none of them needs a
permission. Building the unit tests prints four "tagged to
remove … but no other declaration present" warnings, because that merge
runs without the library manifests; the app's own merge prints none.

## 10. The widget

### Look

The widget is built with Glance, at a default size of 4 × 2 cells, and can
be resized.

| | ON | PAUSED |
|---|---|---|
| Background | Yellow `#FFD60A`, 28 dp radius | `#0B0B0D`, 4 dp white border, 28 dp radius |
| Lines | "Visual alerts" (28 sp), bolt icon + "ON" (48 sp; the design's 64, by the owner's choice), "Tap to pause for 1 hour" (22 sp) | "Visual alerts" (28 sp), pause icon + "PAUSED" (40 sp; the design's 52, by the owner's choice), "Back ON by itself at HH:MM" (22 sp, timed pause only), "Tap to turn ON now" (22 sp) |
| Tap | Pause for 1 hour | Turn ON now |
| Spoken label | "Visual alerts are ON. Tap to pause for 1 hour." | "Visual alerts are PAUSED until HH:MM. Tap to turn them back on now." For an open-ended pause from the app: "Visual alerts are PAUSED. Tap to turn them back on now." |

The text is `#0B0B0D` on yellow and `#FFFFFF` on black, in the system font.
The icons are decorative. The whole widget is one tap target.

### Size and the picker entry

- **Size:** 4 × 2 cells by default (`targetCellWidth` 4, `targetCellHeight`
  2). `minWidth` and `minResizeWidth` are 250 dp, `minHeight` and
  `minResizeHeight` 110 dp. `updatePeriodMillis` is 0: there are no
  periodic updates. Only Android 12 and later read the target cells;
  Android 10 and 11 size the widget from `minWidth` and `minHeight`.
  Lint's `UnusedAttribute` warning about this is ignored in
  `beacon_widget_info.xml`.
- **The minimum is in dp, not cells.** Android takes a widget's minimum
  size in dp, and each launcher turns it into cells with its own cell
  size. So 250 × 110 dp can't promise 4 × 2: a launcher whose columns are
  wide enough allows 3 × 2 (PR 3's D2), and one whose rows are at least
  110 dp tall would allow a single row. A larger minimum would force 5
  cells on a 5-column grid such as HyperOS's. Instead, the widget fits
  itself to the size it gets (below), so nothing is clipped at the smaller
  sizes. This replaces the plan's ruling 6, "never below 4 × 2".
- **Android 10 and 11:** launchers read the target cells only from
  Android 12. Before that they work out the default size from `minWidth`
  and `minHeight`, so the widget may open at 3 × 2. Not tested.
- **Seen on the emulator** (Pixel Launcher, Pixel 7 profile, from
  `dumpsys appwidget --proto`): 4 × 2 is 360 × 224 dp in portrait and
  624 × 135 dp in landscape; 4 × 3 is 360 × 344 dp; 3 × 2 is 266 × 224 dp.
  That launcher stops at 3 × 2: dragging an edge further gives neither two
  columns nor one row. A single row on HyperOS's launcher is Not tested.
  Real cell sizes differ per launcher and phone.
- **Picker entry:** the widget receiver's label, "Beacon", and the
  description "Shows whether visual alerts are ON. One tap pauses them for
  1 hour." There is no custom preview image, so the picker shows Beacon's
  app icon.

### When lines don't fit

A widget can't scroll. When the widget's size and the font scale leave too
little room, lines are left out in this order: the tap hint first, then the
"Visual alerts" title, then the decorative state icon, then the resume
time. The state word is never left out, and is kept even if it alone
doesn't fit. A resume time that is left out is only not drawn: the spoken
label always carries every line, so TalkBack still says the time. This
order replaces the plan's ruling 7 and this section's earlier "the state
word and the resume time are always kept": at the smallest sizes a clear
"PAUSED" is better than both lines clipped.

**Measured before drawing.** Glance can't measure text, so the fit
measures each line itself, the way the launcher's TextView will lay it
out. It uses Android's `StaticLayout` with:

- the line's text size in sp, scaled by the user's font size (on Android 14
  and later this includes the system's non-linear font scaling);
- bold for the title and the state word, as drawn;
- the font padding a TextView adds, and high-quality line breaks;
- the width the line really gets: the widget's width less 12 dp of padding
  on each side, and for the state line less the icon (as wide as the text
  is tall) and its 8 dp gap. A word too wide for its line counts as
  wrapped, never as fitting.

Each measured line gets 2 dp of margin, and lines have 4 dp between them.
The fit and the drawing share the padding and the gaps. The fixed estimate
PR 3 first used (0.55 em per character, a 1.2 line height) was too small
for the bold capitals of "PAUSED" (about 0.62 em), which broke the word and
cut the resume line at 3 × 2 (PR 3's D3). The measure gives the heights the
emulator's launcher drew, to the pixel. The fit is worked out each time
Glance draws the widget.

On the emulator after the fix (Tested on emulator beacon-api36,
Android 16):

| Size | Font | ON | Timed PAUSED |
|---|---|---|---|
| 4 × 2 (360 × 224 dp) | 1.0 | Title, bolt with "ON", tap hint | Title, pause icon with "PAUSED", resume line, tap hint |
| 4 × 2 | 2.0 | Title, bolt with "ON" | Pause icon with "PAUSED", resume line on two lines |
| 3 × 2 (266 × 224 dp) | 1.0 | Title, bolt with "ON", tap hint | "PAUSED", resume line on two lines |
| 3 × 2 | 2.0 | Title, bolt with "ON" | "PAUSED" |

Nothing is clipped and no word is broken in any of them. Real fit on the
phone is not tested yet (section 15).

**After a font-size change.** The fit depends on the font size, the screen
density and the language, so Beacon holds those three as state that the
widget's drawing reads. When one of them changes, the widget is fitted and
drawn again, also inside a Glance drawing session that is still running,
and Beacon asks for a redraw, which starts a session when none is running.
Before this, a font-size change alone left the old lines at the new size,
clipped: the launcher redraws the widget's last views with the new text
size, and a refresh that reached a running session redrew nothing, because
nothing it read had changed (PR 3's D1). A rotation, or any change that
leaves those three alone, starts nothing. The same state also holds the
12/24-hour setting and the time zone, which the resume time is shown in;
those changes reach Beacon through `TimeChangeReceiver` (follow-up R1, in
"Keeping it current" below). If the font size or the display size changes
while Beacon's process isn't running at all, nothing tells it: the widget
keeps its old lines until its next redraw (section 13).

### Keeping it current

- **After a change:** the `PauseCoordinator` redraws the widget through
  `WidgetRefresher` after every pause change, from the app or from the
  widget (section 5). A catch-up that finds nothing to clean up redraws
  nothing, except after a time, time-zone or language change (below).
- **After a time, time-zone or language change:** `TimeChangeReceiver`
  stores the widget's key, then runs the coordinator's catch-up, which
  redraws the widget once while a timed pause is running and in every
  state after a language change (section 5, follow-up R1). It asks even
  when the key didn't change: if Beacon's process wasn't running, the
  broadcast started it and Beacon read the key fresh, so "unchanged" says
  nothing about what the widget last drew. On the emulator, a 12/24-hour
  switch with Beacon's process not running started it and redrew the
  widget; with alerts ON, a switch started no drawing session.
- **At the resume time:** an inexact alarm (`setWindow`, `RTC_WAKEUP`) is
  set for the end of a timed pause, with a 10-minute window: the smallest
  inexact window Android allows without the exact-alarm permission. Its
  receiver runs the coordinator's catch-up, which writes `None` and
  redraws the widget. With the phone awake the widget turns ON at most
  10 minutes late (`window=+10m0s0ms` on the emulator). Before, Android
  chose the window itself, 75% of the delay: 45 minutes for a 1-hour
  pause. The alarm doesn't fire in Doze; it waits for Doze's next
  maintenance window, and the screen turning on catches up anyway. This
  replaces the plan's ruling 3 (`setAndAllowWhileIdle`).
- **Screen on and Beacon in front:** while the listener is bound, it runs
  the same catch-up when the screen turns on, and the widget is only seen
  while the screen is on. `MainActivity` runs it each time Beacon comes to
  the front. Either one redraws the widget only when it cleaned up an
  ended pause; otherwise it only sets the alarm again. On the emulator the
  screen-on catch-up cleared an ended pause 0.15 seconds after the screen
  came on, before the alarm had fired. With nothing to clean up, a screen
  off and on and opening Beacon started no drawing session. On the test
  phone, locked on battery in deep idle, an alert woke the screen 43
  seconds after a 1-hour pause's end; the screen-on catch-up cleared the
  pause, cancelled the alarm, which hadn't fired, and redrew the widget
  less than a second later
  ([`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md)).
- **Alerts don't wait for the widget.** They check the clock themselves
  (section 5), so they come back on time even if the widget lags.

## 11. Accessibility

### Colours

Every text colour pair, checked against WCAG's contrast formula. A unit test
recomputes these from the theme tokens and fails below 7:1.

| Text | On | Ratio | Used for |
|---|---|---|---|
| `#FFFFFF` | `#0B0B0D` page | 19.66:1 | Text on the page, Pause button, OFF pill, widget PAUSED |
| `#D9D9DF` | `#0B0B0D` page | 13.99:1 | Secondary text on the page |
| `#FFFFFF` | `#1B1B20` card | 17.15:1 | Text on cards |
| `#D9D9DF` | `#1B1B20` card | 12.20:1 | Secondary text on cards |
| `#AEAEB6` | `#1B1B20` card | 7.78:1 | Disabled Shorter/Longer |
| `#0B0B0D` | `#FFD60A` yellow | 13.93:1 | ON card, selected options, ON pill, yellow buttons, widget ON |
| `#FFFFFF` | `#000000` | 21.00:1 | Overlay cards, swatch labels |
| `#D9D9DF` | `#000000` | 14.94:1 | Overlay labels |
| `#FFFFFF` | `#141418` tab bar | 18.37:1 | Tab labels |
| `#0B0B0D` | `#FFFFFF` | 19.66:1 | Test alert button |
| `#000000` | `#FFFFFF` | 21.00:1 | Stop button |

No text is ever drawn directly on a flash colour. Names sit on black labels,
because red and blue can't carry dark text at 7:1 (5.54:1 and 5.39:1).

The launcher icon (section 8) uses the `#0B0B0D` on `#FFD60A` pair, 13.93:1.
It isn't text, so WCAG asks only 3:1 of it (1.4.11), but Beacon holds it to
7:1 too. `LauncherIconTest` checks it, so the table and its 11-pair test
stay text only.

### Type

All sizes are in sp, using Atkinson Hyperlegible Next at 400, 700 and 800.

| Style | Size | Weight | Used for |
|---|---|---|---|
| Screen title | 36 | 800 | H1 |
| State | 56 | 800 | ON / PAUSED on Home |
| Heading | 28 | 800 | Section headings, option labels, "Test alert" |
| Row title, button | 24 | 800 | App names, buttons |
| Small button, pill | 22 | 800 | Change/Done, Shorter/Longer, ON/OFF pills, swatch labels |
| Body strong | 22 | 700 | Card copy, values |
| Body | 20 | 400 | Explanations |
| Sub | 20 | 700 | Option sub-lines |
| Special | 26, 32, 34, 40, 96 | 800 | Test button, Stop, overlay title, custom value, countdown |

Body text is never under 20 sp, and headings are never under 28 sp bold.
Headings are marked as headings for TalkBack.

### Layout

- **Size:** every touch target is at least 64 × 64 dp. Most are larger, as
  in the design.
- **Largest font:** screens scroll, and nothing has a fixed text height.
  Text wraps and is never ellipsized. Section 3 rows 9 and 10 cover the
  fixed area and the grids.
- **Orientation:** portrait and landscape both work. State survives
  rotation.
- **System bars:** always dark, whatever the phone's theme. The status bar
  and navigation bar are transparent with light icons and buttons, and the
  system draws no contrast scrim, so the page (`#0B0B0D`) and the tab bar
  (`#141418`) show behind them. White on those is 19.66:1 and 18.37:1.
  The bars are always visible and dark, and never hidden, also while a
  flash fills the window. During a flash, in the test overlay and on the
  real alert screen, the strips behind the bars stay black, so their white
  icons are 21:1; the blink fills the rest (section 7). A blink behind the
  bars would put those icons on the flash colour (about 1.4:1 on Yellow).
  Hiding the bars instead was tried in PR 2 and made Android show its
  one-time "Viewing full screen" panel over the first alert and the first
  test, covering the title and dimming the blink (PR 2's D1).

### TalkBack

| Control | Role | State read aloud | Label |
|---|---|---|---|
| App row | Switch | "On" / "Off" | App name and its flash line, such as "Texts (SMS), Yellow flash" |
| Duration, speed, mode and swatch options | RadioButton, in a group read as a one-column list ("In list, 4 items", then "2 of 4") | "Selected" / "Not selected" | The visible text, such as "Fast, 90 blinks a minute, Highest safe speed" |
| Tabs | Tab, in a selectable group | "Selected" | "Home" / "Alert settings" |
| Per-app Change/Done | Button | "Expanded" / "Collapsed" | "Change color for Telegram" / "Done choosing color for Telegram" |
| Shorter/Longer | Button | "Disabled" at the ends | "Shorter" / "Longer" |
| Setup status | Text | "On" / "Off" | The row name |
| Custom value, countdown | Live region (polite) | — | "10 min", "2 seconds left" |
| Icons | None | — | Decorative icons are hidden. No control relies on an icon alone. |
| Home-screen widget | None (see below) | In the label | The widget's spoken label from section 10, such as "Visual alerts are ON. Tap to pause for 1 hour." |

Spoken labels always contain the visible text, so voice control matches what
is on screen.

The app rows' "On" and "Off" are Compose's own words (its private
`state_on` and `state_off`, with its translations). The setup status has
strings of its own, `setup_state_on` and `setup_state_off`, so changing
its words can't change what the switches say (section 12). Like the rest
of Beacon's text, they are English until Beacon is translated.

**The widget has no role.** Glance 1.2 can't give a widget's views a role,
so TalkBack doesn't say "Button" for it; on the emulator it says
"Double-tap to activate" after the label. The label starts with the state
and says what a tap does ("Tap to pause for 1 hour", "Tap to turn them back
on now"), and the whole widget is one tap target and one focus target, so
nothing depends on the role.

**List positions.** Every radio group is read as a one-column list in
reading order, whatever its visual columns: "1 of 4" to "4 of 4" for the
durations, "1 of 5" to "5 of 5" for the swatches.

- The one-column groups (blink speed, colour mode) are a `selectableGroup`;
  Compose counts their options itself.
- The grids (durations and both swatch grids) state the list's size, with
  one column, and each option states its row. Compose 1.11 counts a
  `selectableGroup`'s options by their place in their own parent layout,
  and every grid option sits alone in its grid cell, so all of them were
  read as "1 of 4". It also writes its own count over an option's stated
  row whenever the parent is a `selectableGroup`, so the grids don't use
  one.

**Live regions.** TalkBack reads a live region when it gets a content-change
event from it. The live region holds its text, and a change inside it
reaches TalkBack as a subtree change of the live region:

- The countdown's number and words are new nodes for each second, so every
  second is such a change, whatever the glyph widths. Before PR 2 it relied
  on the text changing width.
- The Custom value's text is a new node for each length, so every step is
  such a change, even when the new value is as wide as the old one. Compose
  1.11 can drop a text change made in place from its events (it depends on
  hash order), and on the Android 16 emulator it always did.

### Rules kept

- Every switch shows ON or OFF in words.
- There are no sliders, hamburger menus or icon-only buttons.
- Navigation is a flat bottom bar with exactly 2 tabs.

## 12. Strings and right-to-left

- **Strings:** all user-facing text is in `res/values/strings.xml`, with
  plurals for minutes, seconds and the blink speeds' line ("30 blinks a
  minute"). This covers spoken labels, widget text and the app name.
  Kotlin code holds no visible text.
- **Names:** no Beacon string takes the name of a library's resource.
  A string with the same name would replace the library's English text,
  and nothing at run time would show it; lint's `PrivateResource` flags it
  only when the library marks its resource private. The lint clean-up
  checked all of Beacon's names against the libraries' and found none
  shared.
- **Direction:** the manifest sets `supportsRtl="true"`. Layouts use start
  and end only.
- **Text direction:** every text is drawn by `BeaconText`, which takes the
  direction from the text's own words (`TextDirection.Content`), so English
  lines read left to right in a right-to-left layout and a Hebrew
  translation reads right to left. Compose's own default follows the
  layout instead, which on a phone set to Hebrew turned "1 minute" into
  "minute 1" (the phone checks' P1). `BeaconText` also keeps text at the
  layout's start edge: Compose's `TextAlign.Start` follows the text's
  direction, so Start and End become the layout's left or right there, the
  one place that uses left and right. A test fails if any other file draws
  text with `BasicText`.
- **Icons:** icons that point, such as the chevron, are mirrored for
  right-to-left.
- **Short added text:** the Test alert button's "(3 sec)" takes its
  direction from its own text, as every text does through `BeaconText`, so
  an English string in a right-to-left layout stays on one line.
- **Translation:** none in Step 2. A Hebrew translation can be added later
  without layout changes.

## 13. Errors and edge cases

| Case | What Beacon does |
|---|---|
| No camera light (or no camera service), or the camera is in use | Only the screen blinks. The test overlay says "Not on this phone" for a missing light. A light that refuses during an alert is marked off and its heat poll stops; the test overlay then says "Off while another app uses the camera" (section 7, PR 4). Not tested on a device: the emulator has no camera light to take away from Beacon. |
| "Display over other apps" off | Only the light flashes. The setup card shows it OFF. |
| Beacon open in front with "Display over other apps" off | Only the light flashes, as everywhere else without that permission; no blink screen opens over Beacon. It ends on its timer or when Beacon is opened again. |
| Notification access off | Nothing is detected. The setup card shows it OFF. |
| Listener dropped by the system | Reconnected when Beacon is opened (section 6). |
| Phone too hot (Severe or worse) | The light stops and the screen keeps blinking. The test overlay says "Off while the phone is hot". |
| The system refuses the thermal listener | The controller checks the thermal status itself every 30 seconds while the light flashes (section 7). |
| Messages arrive during an alert | Recorded, no second alert. |
| A real message during a test | The test ends and the real alert starts. The overlay's Stop and Back can't end that real alert. |
| Settings unreadable | Defaults are used, and the app stays on them until it is restarted. |
| Settings can't be saved | The change isn't kept, and the screen keeps showing the saved value. No message in PR 1. |
| An app's icon can't be drawn | Beacon shows its own glyph for that app. |
| A timed pause ends while nothing runs | Alerts resume on time (clock check). The widget catches up when the resume alarm fires, at most 10 minutes late while the phone is awake and at Doze's next maintenance window while it sleeps (section 10), or on the next screen-on, or when Beacon comes to the front, or after a time, time-zone or language change. The alarm's catch-up and the screen-on catch-up were both seen clearing an ended pause on the emulator, with its clock moved forward. On the test phone, locked on battery in deep idle, a message 40 seconds after a 1-hour pause's end flashed while the pause was still stored, and the screen-on catch-up then cleared it and cancelled the alarm, which hadn't fired ([`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md)). |
| The phone restarts | Settings and pause are kept. Android rebinds the listener once the phone is first unlocked. With HyperOS Autostart off it stays unbound until Beacon is opened (the phone checks' P5), so the Finish setup card asks for Autostart on Xiaomi phones (section 9). In one restart on the test phone with Autostart on, the listener was bound by itself once the phone was first unlocked, and an SMS flashed. |
| The phone restarts during a timed pause | The resume alarm is gone and isn't set again, because there is no `RECEIVE_BOOT_COMPLETED` (PR 3's ruling 4). Alerts still resume on time (clock check), and Home and the widget draw from the clock, so nothing shows the pause wrongly once it has ended. The stored pause is cleaned up, and the widget redrawn, the next time Beacon comes to the front, the screen turns on, the time, time zone or language changes, or the widget updates. The catch-up run by the first three also sets the alarm again while the pause is still running. Not tested. |
| The phone's clock is set back, by hand or by automatic time, while the widget is updating | On the emulator the widget showed ON while a 1-hour pause was saved (PR 3's checks, O1). This followed moving the emulator's clock forward and setting it back by hand: one drawing session then ran for 16 minutes with a wake lock, and it was ended by hand with a debug command (`cmd jobscheduler timeout`). WorkManager queued it again about 45 minutes out, and until then the widget didn't redraw; removing the widget and adding it again cleared it. Likely cause, not tried: Glance and WorkManager time their sessions by the wall clock. Alerts and Home read the saved pause, not the widget. On a phone the clock can also be set back by automatic time correcting it. Not tested on the phone, either way. Whether this is a defect is open for the owner. |
| The font size or the display size changes while Beacon's process isn't running | Nothing tells Beacon, so the launcher shows the widget's old lines at the new size, which can clip, until the widget's next redraw: a pause change, the resume alarm, a widget tap, a language change, or a time or time-zone change while a timed pause is running (the last two through `TimeChangeReceiver`). Opening Beacon doesn't repair it. While notification access is on, the bound listener normally keeps Beacon's process running. Not tested. |
| The language changes while Beacon's process isn't running | LOCALE_CHANGED starts Beacon for `TimeChangeReceiver`, which redraws the widget (follow-up R1). The same was seen on the emulator for a 12/24-hour change; a language change wasn't tried, because Beacon has only English strings. Whether HyperOS lets the broadcast start Beacon with Autostart off isn't known. Not tested. |
| Another app's notification has a one-time code | Android 15+ may hide the content. Beacon doesn't need content, so no effect is expected. Not tested. |

## 14. Privacy

- **No internet:** Beacon has no `INTERNET` permission. The merged manifest
  is checked for it.
- **No message content:** content is never stored, logged or shown.
- **Names:** for WhatsApp, the owner check (section 6, rule 6) may compare
  the newest sender's name with the owner's, when neither has a key. The
  names are read only for that comparison, in memory; only the yes/no
  answer is kept, and nothing is logged.
- **Logs:** release builds write no logs. Debug builds log only the
  classifier's decision and reason, never titles, names or text.
- **What's saved:** settings are the only saved data, and they never leave
  the phone (section 5, backup off).

## 15. Testing

### JVM unit tests (no device)

- `MessageClassifier`: every rule and reason, including:
  - the WhatsApp backup pattern;
  - group summaries;
  - repeated updates;
  - own replies;
  - "silent" channel messages, which do alert;
  - SMS without MessagingStyle but with category `msg`;
  - pause edges at exactly `t`.
- `BlinkSpeed`: rates, half periods, and at most 3 flashes a second.
- `CustomLength`: steps, ends, and an unknown value becoming 10.
- `BlinkClock`: the phase at boundaries, and "Until I stop it".
- Settings mapping: defaults, round trip, unknown and missing values.
- Contrast: every pair in section 11 is at least 7:1.
- The battery steps (`batteryStepsRes`): which string each Android version
  from 29 to 37 gets, for Xiaomi in any letter case and for four other
  makers' names, with each boundary checked on both sides.

### Robolectric Compose tests

- Roles, states and labels from section 11 on Home, Alert settings, the test
  overlay and the alert screen.
- The list positions TalkBack reads, taken from the node information
  Compose builds for TalkBack, not only from the semantics.
- The system bars' style in the light and the dark system theme.
- Tab switching.
- Option selection.
- Custom length at both ends.
- Per-app colour editing, with one grid open at a time.
- The content-change event TalkBack gets from the Custom value's live
  region, after both Longer and Shorter.
- The overflow check that the largest-font tests use: it fails on
  ellipsized and clipped text, and passes on text that fits.
- No camera service means no light, without a crash.
- A refused light: `TorchBlinker` reports it once, after its final off, and
  never for an alert's end or a Stop; the controller then stops the heat
  poll and keeps the screen and the wake lock, and a late report from an
  ended or replaced alert changes nothing. The test overlay's camera line
  for a refusal, also at font 2.0.
- The setup card's battery steps at Robolectric's default Android version
  (16), and Android 10's steps drawn by the card at font 2.0. No test starts
  another Android version, so the tests need only the default version's
  Android jar.
- The widget's text measure, with Robolectric's native graphics at the
  emulator's density: the lines kept at 266 × 224 and 360 × 224 dp, fonts
  1.0 and 2.0, ON and timed PAUSED, with nothing taller than the widget and
  "PAUSED" on one row.
- The widget's drawing fitted again when the font size changes under it.
- The pause coordinator: a change finishes when its caller is cancelled,
  and an unchanged pause redraws nothing, except that after a time
  change a running timed pause is redrawn once, and after a language
  change every state is.
- The time-change receiver: each of its three broadcasts during a timed
  pause, TIME_SET and LOCALE_CHANGED with alerts ON, an unrelated action,
  the key stored before the redraw, and the manifest entry. The widget's
  drawing shows the 24-hour time when the key changes under it, and the
  key follows the 12/24-hour setting and the time zone.
- The launcher icon: both icons inflate as adaptive icons with a monochrome
  layer, the manifest names both, the colours are the theme's tokens, the
  path is the design's bolt, and every corner of the stroked bolt stays
  inside the 66 dp safe zone.

### Emulator (Android 16, API 36)

- Screenshots of every screen and state at the default and the largest font
  size, in portrait.
- Landscape at the largest font size.
- A TalkBack pass on every screen.
- The widget in both states at both font sizes.
- The launcher icon on the home screen, in App info, in the widget picker
  and as a themed icon.
- The setup card's battery path on Android 16, read with `uiautomator dump`
  and then followed by the card's own steps word for word, from "Optimized"
  and from "Restricted" (PR 4).
- Installing the emulator and system image needs the owner's OK for any new
  licence.

### Real phone (Redmi 15C 5G, Android 16), when it is connected again

- The setup card, end to end.
- SMS and WhatsApp alerts with the phone locked, asleep and on battery.
- No flash for the backup pattern or for own replies.
- Each way an alert ends.
- Widget pause and auto-resume.
- Largest font and TalkBack on the phone.

The phone checks on 11 November 2026
([`docs/checks/2026-11-phone-checks.md`](../checks/2026-11-phone-checks.md))
and the owner's two cable-out checks that day
([`docs/checks/2026-11-alerts-on-battery.md`](../checks/2026-11-alerts-on-battery.md)
and [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md))
ran most of these; what is left is in their "Not tested" lists.

Anything not run on the phone is reported as "Not tested". "Should work"
counts as not tested.

## 16. Delivery

Work happens on `feature/` branches, one PR at a time. Each PR comes with
README, CHANGELOG and spec updates and a full PR draft. Each implementation
PR has its own plan, written just before it starts, so later plans can use
what earlier PRs found.

| PR | Contents |
|---|---|
| 0 | This spec and the plan for PR 1 (docs only) |
| 1 | Project setup, theme, fonts, settings, Home, Alert settings, test overlay (with `BlinkClock`, `TorchBlinker` and the test path of `AlertController`) |
| 2 | Listener, classifier, the full alert controller, `AlertActivity`, the setup card |
| 3 | Widget, timed pause and its Home line, widget refresh |
| 4 | PR 2's last two carry-overs: a refused camera light reported to the alert controller, and the battery steps for each Android version |

PR 0 merged as yanivil/beacon#5, PR 1 as #6 and PR 2 as #7, all on
9 November 2026.

PR 1: built on `feature/step2-screens`. Its emulator checks are in
[`docs/checks/2026-11-step2-pr1.md`](../checks/2026-11-step2-pr1.md).

PR 2: built on `feature/step2-alert-engine`, from the plan
[`docs/plans/2026-11-09-step2-pr2-alert-engine.md`](../plans/2026-11-09-step2-pr2-alert-engine.md).
Its emulator checks are in
[`docs/checks/2026-11-step2-pr2.md`](../checks/2026-11-step2-pr2.md). They
found two defects, both fixed and re-checked on the emulator: Android's
one-time "Viewing full screen" panel covered the first alert and the first
test (D1), and an alert that started while the screen was off ended at
once (D2).

PR 3 merged as yanivil/beacon#8 and the app icon as #9, both on
9 November 2026.

PR 3: built on `feature/step2-widget`, from the plan
[`docs/plans/2026-11-09-step2-pr3-widget.md`](../plans/2026-11-09-step2-pr3-widget.md).
Its emulator checks are in
[`docs/checks/2026-11-step2-pr3.md`](../checks/2026-11-step2-pr3.md). They
found three defects, all fixed in PR 3 and re-checked on the emulator: after
only a font-size change the widget stayed clipped until Glance drew it
afresh (D1); the launcher let the widget shrink to 3 × 2 (D2), which the
widget now fits instead of forbidding; and at 3 × 2 "PAUSED" broke and the
resume time was cut off (D3). PR 3's final review found that, after ruling
F3, the widget no longer followed a 12/24-hour or time-zone change during
a timed pause; follow-up R1 fixed it with `TimeChangeReceiver` and
re-checked it on the emulator.

App icon: built on `feature/app-icon`, a PR of its own beside the three
above. Its emulator checks are in
[`docs/checks/2026-11-app-icon.md`](../checks/2026-11-app-icon.md).

PR 4: built on `feature/step2-carryovers`, from the plan
[`docs/plans/2026-11-09-step2-pr4-carryovers.md`](../plans/2026-11-09-step2-pr4-carryovers.md),
and merged as yanivil/beacon#10 on 9 November 2026. Its emulator checks are in
[`docs/checks/2026-11-step2-pr4.md`](../checks/2026-11-step2-pr4.md): the
battery path on stock Android 16, and the new steps followed word for word
from "Optimized" and from "Restricted" (fix round 0, after the first
wording's "Allow background usage" step could lead to the switch that
restricts Beacon), with the row at font 2.0. The refusal has unit tests
only (the plan's ruling 4).

Lint clean-up: built on `fix/lint-warnings` and merged as yanivil/beacon#11
on 10 November 2026, a PR of its own after PR 4,
with no plan of its own: its rulings are in its PR and its checks file,
[`docs/checks/2026-11-lint-cleanup.md`](../checks/2026-11-lint-cleanup.md).
It deals with lint's ten warnings about Beacon's own code and resources:
- the speed line becomes a plural (section 12);
- the setup status gets strings of its own (sections 11 and 12);
- four shared composables take `modifier` as their first optional
  parameter;
- three warnings are ignored where they stand, each with its reason
  (sections 5 and 10).

Also, `compileSdk` and `targetSdk` stay at 36, as `CLAUDE.md` says. So
lint's two reminders that 37 exists are silenced on those two lines.

Lint then reports 0 errors and 8 warnings, all about newer versions of
Gradle and the libraries, for the owner to decide on.

Dependency versions: built on `fix/dependency-versions` and merged as
yanivil/beacon#12 on 11 November 2026, a PR of its own after the lint
clean-up, when the owner asked for the 8 warnings to be dealt with. Its
rulings and checks are in
[`docs/checks/2026-11-dependency-versions.md`](../checks/2026-11-dependency-versions.md).
- The Gradle wrapper goes from 9.8.2 to 9.9.1, and Kotlin from 2.2.10 to
  2.4.21 (section 4). Nothing anyone sees or hears changes.
- Compose, Lifecycle and Core KTX stay, since their newer releases need
  compileSdk 37 (section 4).
- One test line, a bare `Unit` that Kotlin 2.4.21 reports as unused, is
  removed.

Lint then reports 0 errors and 6 warnings, all about those three
libraries, until the owner decides about compileSdk 37.

compileSdk 37: merged as yanivil/beacon#13 on 11 November 2026, after the owner raised
compileSdk from 36 to 37 in `CLAUDE.md` on 11 November 2026. targetSdk
stays at 36. Its rulings and checks are in
[`docs/checks/2026-11-compile-sdk-37.md`](../checks/2026-11-compile-sdk-37.md).
- Compose goes to 1.12.1 (BOM 2026.09.00), Lifecycle to 2.11.0 and Core
  KTX to 1.19.1 (section 4). Beacon's code needs no change.
- Compose UI 1.12 no longer needs `androidx.transition`, so it and six old
  support libraries leave the app.
- On the emulator, the screens are pixel-identical to the build before,
  at font size 1.0 and 2.0.

Lint then reports no issues.

Phone checks: on 11 November 2026 Step 2's app ran on the test phone for
the first time, with the owner at the phone. The results, defects and the
owner's decisions are in
[`docs/checks/2026-11-phone-checks.md`](../checks/2026-11-phone-checks.md).
- Five defects: P1, English lines laid out right to left on a Hebrew phone
  (section 12); P2, the Xiaomi battery step names a row HyperOS in Hebrew
  calls "חשמל" (section 9); P3, a flash for the owner's own reply from a
  WhatsApp notification, which carries the owner's name but no key
  (section 6); P4, the widget hiding the resume time at text size 1.45
  (section 10); P5, no alerts after a restart with HyperOS Autostart off
  until Beacon is opened (section 13).
- The owner's decisions: a real alert flashes only when the screen is off
  or the lock screen is showing, and Beacon does nothing while the phone
  is unlocked and in use (section 7); the widget's state word is 40 sp
  (PAUSED) and 48 sp (ON), with 12 dp of padding (section 10).
- Each defect and decision is built in a PR of its own, which also changes
  the sections named here.
- P1 is merged as yanivil/beacon#15 (section 12):
  [`docs/checks/2026-11-rtl-text-direction.md`](../checks/2026-11-rtl-text-direction.md).
- P3 is merged as yanivil/beacon#16 (sections 6 and 14):
  [`docs/checks/2026-11-own-whatsapp-reply.md`](../checks/2026-11-own-whatsapp-reply.md).
- P2 is merged as yanivil/beacon#17 (section 9):
  [`docs/checks/2026-11-xiaomi-battery-step.md`](../checks/2026-11-xiaomi-battery-step.md).
- P4, with the owner's widget sizes, is merged as yanivil/beacon#18
  (sections 3 and 10):
  [`docs/checks/2026-11-widget-text-size.md`](../checks/2026-11-widget-text-size.md).
- The owner's decision 1, a flash only with the screen off or locked, is
  merged as yanivil/beacon#19 (sections 6 and 7):
  [`docs/checks/2026-11-flash-when-not-in-use.md`](../checks/2026-11-flash-when-not-in-use.md).
- P5, a Background autostart row on Xiaomi phones, is merged as
  yanivil/beacon#20 (sections 9 and 13):
  [`docs/checks/2026-11-xiaomi-autostart.md`](../checks/2026-11-xiaomi-autostart.md).
- Home's ON card line ends "…and you aren’t using the phone.", as the owner
  asked (section 8), merged as yanivil/beacon#21.
- The owner's cable-out check: SMS and WhatsApp alerts on battery, in Doze,
  on the test phone (section 1, "Done means" 2):
  [`docs/checks/2026-11-alerts-on-battery.md`](../checks/2026-11-alerts-on-battery.md).
- The owner's second cable-out check: the widget's 1-hour pause on battery,
  in Doze (section 1, "Done means" 4). Alerts came back on time by the
  clock, and the screen-on catch-up set the widget back to ON; the resume
  alarm never fired, so it is still not seen in Doze:
  [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md).

## 17. Not tested at the end of Step 2 (expected)

- Telegram and WhatsApp Business detection.
- Any phone other than the Redmi 15C 5G, and Android 10 to 15.
- Swiping Beacon away, and the system killing it. A restart was checked on
  the phone on 11 November 2026: with HyperOS Autostart off, Beacon hears
  nothing until it's opened (the phone checks' defect P5); with Autostart
  on, it came back by itself.
- Real launcher cell sizes other than the test phone's.
- Installing from Google Play.
