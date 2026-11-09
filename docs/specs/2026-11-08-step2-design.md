# Step 2 design: the Beacon app

| | |
|---|---|
| Status | Approved by the owner, 8 November 2026. PR 1 built (screens and test alert), with the emulator checks' defects D1 to D5 fixed. PR 2 built (the alert engine and the "Finish setup" card), with its emulator checks' defects D1 and D2 fixed. PR 3 built (the widget and the timed pause); its emulator checks found defects D1 to D3, not fixed yet. |
| Date | 8 November 2026 |
| Approved | The technical design in chat, with the package name `dev.beacon`, then this written spec, both on 8 November 2026 |
| Plan for PR 1 | [`docs/plans/2026-11-08-step2-pr1-screens.md`](../plans/2026-11-08-step2-pr1-screens.md) |
| Plan for PR 2 | [`docs/plans/2026-11-09-step2-pr2-alert-engine.md`](../plans/2026-11-09-step2-pr2-alert-engine.md) |
| Plan for PR 3 | [`docs/plans/2026-11-09-step2-pr3-widget.md`](../plans/2026-11-09-step2-pr3-widget.md) |
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
| Build | Gradle 9.8.2 wrapper, AGP 9.5.1 with its built-in Kotlin, the Compose compiler plugin matching that Kotlin, and a version catalog. Library versions are pinned to exact releases looked up on Google Maven when the plan is written. |
| Library versions (PR 1) | Compose BOM 2026.06.01 (Compose 1.11.4), core-ktx 1.18.0 and lifecycle 2.10.0. Compose 1.12, core 1.19 and lifecycle 2.11 need compileSdk 37 (their AAR metadata says `minCompileSdk=37`), and the project rules fix compileSdk at 36. |
| Ripple | material-ripple 1.11.4 has no `ripple()` function, so `BeaconTheme` builds its white press ripple with the public `createRippleModifierNode`. |
| Unit tests | On JDK 21, Robolectric needs the JVM flag `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`, which the app's test tasks set. |
| SDK | minSdk 29, compileSdk 36, targetSdk 36, applicationId `dev.beacon`, versionName `0.1.0` |

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
  extraction rules exclude everything), so nothing leaves the phone.

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
  Beacon coming to the front and the screen turning on. Each change ends
  the same way: save the pause, set the resume alarm for a timed pause or
  cancel it for any other, then redraw the widget. A lock keeps the alarm
  in the order of the saves.
- **Atomic update:** `SettingsRepository.updatePause(transform)` reads the
  stored pause and writes the new one in a single DataStore edit, so a
  widget tap is decided against the very pause it replaces. Two quick taps
  give a 1-hour pause and then ON, never 2 hours (checked on the emulator
  with taps 41 ms apart).
- **Clean-up:** an expired `Until` is written back as `None` by the
  coordinator's catch-up, which runs when the resume alarm fires, when
  Beacon comes to the front and when the screen turns on (while the
  listener is bound).
- **Display:** the resume time is shown as HH:MM in the phone's 12- or
  24-hour format ("5:29 PM", or "17:50" with the 24-hour setting, on the
  emulator).

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
7. Alerts aren't paused at this moment. Otherwise `PAUSED`.
8. That source's switch is ON. Otherwise `APP_OFF`.

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
- While connected, it also refreshes the widget when the screen turns on
  (section 10).
- **Reconnect:** when Beacon is opened, `MainActivity` checks whether access
  is granted but the listener isn't connected. If so, it turns the listener
  component off and on again. This is the Step 1 fix for HyperOS refusing to
  rebind with Autostart off, and Step 1 requirement 5. The check runs at
  once each time Beacon comes to the front, with no delay, so just after a
  cold start it may switch the listener off and on once for nothing.

## 7. The alert

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
     - The text "Your screen and camera light will flash when a message arrives."
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
  | Camera light | "Flashing too"; "Off while the phone is hot" when the phone has a light but the heat check stopped it (section 7); or "Not on this phone" when there's no light |
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
| Battery: No restrictions | So alerts still arrive while the phone sleeps. | `PowerManager.isIgnoringBatteryOptimizations` (Step 1 showed HyperOS reports "No restrictions" this way) | Beacon's App info page, with steps in the row, above the button. On Xiaomi phones: "Tap Battery saver, then No restrictions." On other phones: "Tap Battery, then Unrestricted." Other phones are not tested; stock Android 16 on the emulator names the row "App battery usage". |

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
`tools:node="remove"`. The merged manifest's `<uses-permission>` set is then
exactly `WAKE_LOCK`, `SYSTEM_ALERT_WINDOW` and the AndroidX signature
permission above, with no `INTERNET`. PR 3 adds two receivers: the widget's
`BeaconWidgetReceiver`, exported so the launcher can reach it, and
`ResumeReceiver`, not exported, which only the resume alarm's explicit
pending intent reaches. Building the unit tests prints four "tagged to
remove … but no other declaration present" warnings, because that merge
runs without the library manifests; the app's own merge prints none.

## 10. The widget

### Look

The widget is built with Glance, at a default size of 4 × 2 cells, and can
be resized.

| | ON | PAUSED |
|---|---|---|
| Background | Yellow `#FFD60A`, 28 dp radius | `#0B0B0D`, 4 dp white border, 28 dp radius |
| Lines | "Visual alerts" (28 sp), bolt icon + "ON" (64 sp), "Tap to pause for 1 hour" (22 sp) | "Visual alerts" (28 sp), pause icon + "PAUSED" (52 sp), "Back ON by itself at HH:MM" (22 sp, timed pause only), "Tap to turn ON now" (22 sp) |
| Tap | Pause for 1 hour | Turn ON now |
| Spoken label | "Visual alerts are ON. Tap to pause for 1 hour." | "Visual alerts are PAUSED until HH:MM. Tap to turn them back on now." For an open-ended pause from the app: "Visual alerts are PAUSED. Tap to turn them back on now." |

The text is `#0B0B0D` on yellow and `#FFFFFF` on black, in the system font.
The icons are decorative. The whole widget is one tap target.

### Size and the picker entry

- **Size:** 4 × 2 cells by default (`targetCellWidth` 4, `targetCellHeight`
  2). `minWidth` and `minResizeWidth` are 250 dp, `minHeight` and
  `minResizeHeight` 110 dp, so it is meant to grow but never shrink below
  4 × 2: at 4 × 2 the 52 sp "PAUSED", its icon and the resume line already
  need all the room. `updatePeriodMillis` is 0: there are no periodic
  updates.
- **Seen on the emulator** (Pixel Launcher, Pixel 7 profile, from
  `dumpsys appwidget --proto`): 4 × 2 is 360 × 224 dp in portrait and
  624 × 135 dp in landscape; 4 × 3 is 360 × 344 dp. That launcher also
  accepts 3 × 2, 266 × 224 dp, because three of its columns are wider than
  250 dp (PR 3's D2, open). Real cell sizes differ per launcher and phone.
- **Picker entry:** the widget receiver's label, "Beacon", and the
  description "Shows whether visual alerts are ON. One tap pauses them for
  1 hour." There is no custom preview image, so the picker shows Beacon's
  app icon.

### When lines don't fit

A widget can't scroll. When the widget's size and the font scale leave too
little room, lines are left out in this order: the tap hint first, then the
"Visual alerts" title, then the decorative state icon, before "PAUSED" would
wrap. The state word and the resume time are always kept, even when they
don't fit on their own; the spoken label always carries every line.

Fit is estimated before drawing, because Glance can't measure text. It uses
the widget's size, each line's scaled text size and fixed constants:

| Constant | Value |
|---|---|
| Glyph width | 0.55 em per character |
| Line height | 1.2 times the text size |
| Padding | 16 dp on each side |
| Gap between lines | 4 dp |
| State icon | as wide as its text size, plus 8 dp before the word |

A line wraps onto as many rows as its estimated width needs. On Android 14
and later the scaled text size includes the system's non-linear font
scaling. On the emulator at 4 × 2 and font 2.0, ON keeps the title and the
bolt with "ON", and the timed PAUSED state keeps the pause icon with
"PAUSED" and the resume line on two lines; nothing is cut. The 0.55 em
estimate is too small for the bold capitals of "PAUSED" (about 0.62 em
measured), which shows at 3 × 2: "PAUSED" breaks and the resume line is cut
off (PR 3's D3, open). Real fit on the phone is not tested yet (section 15).

The fit is worked out each time Glance draws the widget. A change of the
system font size alone doesn't make Glance draw it, so until the next
drawing the launcher shows the old lines at the new size, which can clip.
On the emulator a refresh that reached a drawing session still running
(Glance keeps one open for about 45 seconds after an update) didn't
re-fit it either; the first refresh after that session ended did (PR 3's
D1, open).

### Keeping it current

- **After a change:** the `PauseCoordinator` redraws the widget through
  `WidgetRefresher` after every pause change, from the app or from the
  widget (section 5).
- **At the resume time:** an inexact alarm (`setAndAllowWhileIdle`,
  `RTC_WAKEUP`) is set for the end of a timed pause, so no exact-alarm
  permission is needed. Its receiver runs the coordinator's catch-up, which
  writes `None` and redraws the widget. An inexact alarm can fire late:
  Android allows up to 75% of its delay, so 45 minutes for a 1-hour pause
  (`window=+44m59s995ms` on the emulator).
- **Screen on and Beacon in front:** while the listener is bound, it runs
  the same catch-up when the screen turns on, and the widget is only seen
  while the screen is on. `MainActivity` runs it each time Beacon comes to
  the front. On the emulator the screen-on catch-up cleared an ended pause
  0.15 seconds after the screen came on, before the alarm had fired.
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

Spoken labels always contain the visible text, so voice control matches what
is on screen.

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
  plurals for minutes and seconds. This covers spoken labels, widget text
  and the app name. Kotlin code holds no visible text.
- **Direction:** the manifest sets `supportsRtl="true"`. Layouts use start
  and end only.
- **Icons:** icons that point, such as the chevron, are mirrored for
  right-to-left.
- **Short added text:** the Test alert button's "(3 sec)" takes its
  direction from its own text (content direction), so an English string in
  a right-to-left layout stays on one line.
- **Translation:** none in Step 2. A Hebrew translation can be added later
  without layout changes.

## 13. Errors and edge cases

| Case | What Beacon does |
|---|---|
| No camera light (or no camera service), or the camera is in use | Only the screen blinks. The test overlay says "Not on this phone" for a missing light. |
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
| A timed pause ends while nothing runs | Alerts resume on time (clock check). The widget catches up when the resume alarm fires, which may be up to 45 minutes late for a 1-hour pause (section 10), or on the next screen-on, or when Beacon comes to the front. Both catch-ups were seen on the emulator with its clock moved forward. |
| The phone restarts | Settings and pause are kept. Android rebinds the listener. With HyperOS Autostart off, it may stay unbound until Beacon is opened. This is Step 1's open risk, and not tested. |
| The phone restarts during a timed pause | The resume alarm is gone and isn't set again, because there is no `RECEIVE_BOOT_COMPLETED` (PR 3's ruling 4). Alerts still resume on time (clock check), and Home and the widget draw from the clock, so nothing shows the pause wrongly once it has ended. The stored pause is cleaned up, and the widget redrawn, the next time Beacon comes to the front, the screen turns on, or the widget updates. Not tested. |
| The phone's clock is set back by hand while the widget is updating | On the emulator the widget showed ON while a 1-hour pause was saved (PR 3's checks, O1). This followed moving the emulator's clock forward and setting it back by hand: one drawing session then ran for 16 minutes with a wake lock, and it was ended by hand with a debug command (`cmd jobscheduler timeout`). WorkManager queued it again about 45 minutes out, and until then the widget didn't redraw; removing the widget and adding it again cleared it. Likely cause, not tried: Glance and WorkManager time their sessions by the wall clock. Alerts and Home read the saved pause, not the widget. Not tested on the phone. Whether this is a defect is open for the owner. |
| Another app's notification has a one-time code | Android 15+ may hide the content. Beacon doesn't need content, so no effect is expected. Not tested. |

## 14. Privacy

- **No internet:** Beacon has no `INTERNET` permission. The merged manifest
  is checked for it.
- **No message content:** content is never stored, logged or shown.
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

### Emulator (Android 16, API 36)

- Screenshots of every screen and state at the default and the largest font
  size, in portrait.
- Landscape at the largest font size.
- A TalkBack pass on every screen.
- The widget in both states at both font sizes.
- Installing the emulator and system image needs the owner's OK for any new
  licence.

### Real phone (Redmi 15C 5G, Android 16), when it is connected again

- The setup card, end to end.
- SMS and WhatsApp alerts with the phone locked, asleep and on battery.
- No flash for the backup pattern or for own replies.
- Each way an alert ends.
- Widget pause and auto-resume.
- Largest font and TalkBack on the phone.

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

PR 3: built on `feature/step2-widget`, from the plan
[`docs/plans/2026-11-09-step2-pr3-widget.md`](../plans/2026-11-09-step2-pr3-widget.md),
not merged yet. Its emulator checks are in
[`docs/checks/2026-11-step2-pr3.md`](../checks/2026-11-step2-pr3.md). They
found three defects, not fixed yet: after only a font-size change the widget
can stay clipped until Glance draws it afresh (D1); the launcher lets the
widget shrink to 3 × 2 (D2); and at 3 × 2 "PAUSED" breaks and the resume
time is cut off (D3).

## 17. Not tested at the end of Step 2 (expected)

- Telegram and WhatsApp Business detection.
- Any phone other than the Redmi 15C 5G, and Android 10 to 15.
- Restarting the phone, swiping Beacon away, and the system killing it.
- Real launcher cell sizes other than the test phone's.
- Installing from Google Play.
