# Google Play store listing

The texts for Beacon's Play Console "Main store listing", in English
(United States). Paste each field as it stands between its markers. The
rules come from the spec
([`2026-11-11-play-release-design.md`](../specs/2026-11-11-play-release-design.md),
section 5): the name at most 30 characters, the short description at most
80, the full description at most 4,000. The listing names SMS and WhatsApp
only.

## App name

<!-- name -->
Beacon: Visual Message Alerts
<!-- /name -->

## Short description

<!-- short -->
Flashes your screen and camera light when an SMS or WhatsApp message arrives.
<!-- /short -->

## Full description

<!-- full -->
Beacon is for people who are deaf or hard of hearing. When a text message (SMS) or a WhatsApp message arrives, your whole screen flashes a bright color and your camera light blinks, so you see the message even when you can't hear it.

WHEN IT FLASHES
• When the screen is off or the lock screen is showing, even while the phone sleeps on battery.
• Not while you're using the phone: then the message app's own notification is your cue.
• Only for new messages. Never for calls, backups or progress bars.

MAKE IT YOURS
• Screen color: red, yellow, blue, green or white. Each color has its name written on it. Use one color for every app, or a different color for each.
• Blink speed: Slow (30 a minute), Medium (60 a minute) or Fast (90 a minute).
• How long to flash: 1 minute, 5 minutes, a length you choose, or until you stop it.
• Turn each app ON or OFF.
• Try it any time with the 3-second Test alert.

PAUSE WHEN YOU NEED QUIET
• "Pause all alerts" on the Home screen stops every flash until you turn alerts back on.
• Or add the Beacon widget to your home screen: one tap pauses alerts for 1 hour, and they come back on by themselves. Tap again to turn them on sooner.

EASY TO READ AND USE
• Big, high-contrast text and large buttons.
• Every switch says ON or OFF in words, not only in color.
• Works with TalkBack and with the largest text size.
• Just two tabs: Home and Alert settings.

SAFE BY DESIGN
• At most 1.5 flashes a second, well under the safety limit of 3 a second.
• The camera light stops if the phone gets hot. The screen keeps flashing.
• Every alert has a big Stop button, and stops by itself after the length you chose (unless you chose "until I stop it").

SETUP
Beacon shows a "Finish setup" card that explains each step in plain words and opens the right settings page:
• Notification access, so Beacon knows when a message arrives.
• Display over other apps, so the screen can flash over other apps and the lock screen.
• Battery: No restrictions, so alerts still arrive while the phone sleeps.
• On Xiaomi phones, Background autostart, so Beacon works again by itself after the phone restarts.

PRIVATE
Beacon has no internet access. It collects nothing, shares nothing and shows no ads. It ignores the text of your messages, and the alert screen shows only "New message" and the app's name, never who sent it or what it says.

PLEASE NOTE
Beacon is an aid, not a safety device. It can miss a message or flash late, for example if a permission is turned off or the phone's battery settings hold it back, so don't rely on it alone where a missed message could cause harm. It flashes the screen and the camera light: if you have photosensitive epilepsy, check with your doctor first.

Beacon is free. Its code is public on GitHub under the PolyForm Strict License: github.com/yanivil/beacon
<!-- /full -->

## Release notes for 1.0.0

<!-- notes -->
First test version.
<!-- /notes -->

## Other fields

| Field | Value |
|---|---|
| Category | Communication |
| Price | Free |
| Website | https://github.com/yanivil/beacon |
| Privacy policy | https://github.com/yanivil/beacon/blob/main/PRIVACY.md |
| Contact email | Asked of the owner at the form. Shown publicly. |

## Claims and their sources

Every claim in the full description, with what shows it.

The "Shown on" column says how strongly each claim is shown, never more. **Phone** means a record in `docs/checks/` shows it on the test phone (Redmi 15C 5G, Android 16). **Emulator** means only the emulator showed it. **Unit tests** means only automated tests, code or strings. **Rule** means a project rule or an owner's decision, which no device check shows.

| Claim | Source | Shown on |
|---|---|---|
| Flashes the whole screen and blinks the camera light for an SMS or a WhatsApp message | [Step 2 spec](../specs/2026-11-08-step2-design.md) section 1, "Done means" 2; [`docs/checks/2026-11-alerts-on-battery.md`](../checks/2026-11-alerts-on-battery.md) (both on battery, in Doze) | Phone |
| Flashes with the screen off or the lock screen showing; not while the phone is in use | `CLAUDE.md`, "When to flash"; [`docs/checks/2026-11-flash-when-not-in-use.md`](../checks/2026-11-flash-when-not-in-use.md); `IN_USE` in [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md) | Phone (screen off, and a message ignored while in use); Emulator (lock screen with the screen on) |
| Only new messages; never calls, backups or progress bars | `MessageClassifier.kt` rules (ongoing notifications, group summaries, non-message kinds); `CLAUDE.md`, "Scope". "Your own replies" is left out until it is tested on a phone: [`docs/checks/2026-11-own-whatsapp-reply.md`](../checks/2026-11-own-whatsapp-reply.md) has it as Not tested there, and only WhatsApp's are recognised by name. | Phone (WhatsApp backup, group summaries and Google Messages' ongoing notification ignored); Unit tests (calls and the other kinds) |
| Red, yellow, blue, green or white, each with its name; one colour or one per app | `strings.xml` (`colour_*`, `colour_mode_*`, `colour_hint`) | Phone (Alert settings seen; yellow flashed); Emulator (the other colors, one color per app) |
| Slow 30, Medium 60, Fast 90 a minute | `CLAUDE.md`, "Blink safety"; `strings.xml` (`speed_*`) | Phone (Medium; Step 1 ran the camera light at Fast); Rule (the three presets); Slow not run on a device |
| 1 minute, 5 minutes, a chosen length, or until stopped | `strings.xml` (`duration_*`) | Phone (1 minute); Emulator (the options and a Custom 10 min set in Alert settings); no real alert at 5 minutes, a custom length or "Until I stop it" |
| Each app ON or OFF; a 3-second Test alert | `strings.xml` (`apps_hint`, `test_button_length`) | Phone (Test alert); Emulator (the app switches) |
| "Pause all alerts" until turned back on | `strings.xml` (`alerts_pause_button`, `alerts_paused_body`) | Emulator |
| Widget: one tap pauses for 1 hour, alerts come back by themselves, tap again for ON | [Step 2 spec](../specs/2026-11-08-step2-design.md) section 1, "Done means" 4; [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md); [`docs/checks/2026-11-play-release.md`](../checks/2026-11-play-release.md), check 3 | Phone (the resume alarm itself was never seen firing; alerts came back on at the first message after the pause) |
| Big, high-contrast text; switches say ON or OFF; two tabs | `CLAUDE.md`, "Accessibility" (7:1 contrast, 20 sp body, 64 dp targets, ON/OFF text, two tabs) | Phone (screens and contrast seen at font 1.45); Emulator (64 dp targets); Rule (the 7:1, 20 sp and 64 dp limits) |
| Works with TalkBack and with the largest text size | TalkBack on the phone, by the owner's report only ([`docs/checks/2026-11-phone-checks.md`](../checks/2026-11-phone-checks.md)); TalkBack and font scale 2.0 on the emulator ([`docs/checks/2026-11-step2-pr1.md`](../checks/2026-11-step2-pr1.md), [`docs/checks/2026-11-step2-pr3.md`](../checks/2026-11-step2-pr3.md)). The largest text size was never checked on the phone: the owner was satisfied at 1.45 (phone checks, decision 4). | Phone (TalkBack by the owner's report, on Home, the test alert and the widget; font 1.45); Emulator (TalkBack on the other screens; font 2.0) |
| At most 1.5 flashes a second, under the limit of 3 | `CLAUDE.md`, "Blink safety" (Fast 90 = 1.5 a second; WCAG 2.3.1) | Rule (Fast 90 = 1.5 a second); Phone (Step 1: camera light at Fast for 60 minutes) |
| The camera light stops if the phone gets hot; the screen keeps flashing | `strings.xml` (`test_camera_hot`); `README.md`, PR 2 and PR 4 | Unit tests (no phone record shows the light stopping; Step 1's 60-minute run had no thermal warning) |
| Every alert has Stop, and stops by itself after its length unless "Until I stop it" was chosen | `strings.xml` (`stop`); [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md) (an alert ended by itself after 1 minute; another by a tap) | Phone (Stop; the 1-minute length); no real alert with "Until I stop it" or other lengths |
| The setup steps and their reasons | `strings.xml` (`setup_*`), word for word | Phone (the three setup steps and the Xiaomi Autostart row); Emulator (the battery steps for other phones) |
| No internet, collects and shares nothing, no ads, ignores message text, alert screen shows only "New message" and the app | [`PRIVACY.md`](../../PRIVACY.md), [`SECURITY.md`](../../SECURITY.md); the release manifest ([`docs/checks/2026-11-public-repo-audit.md`](../checks/2026-11-public-repo-audit.md)) | Phone (no `INTERNET` permission in the copy from Play; the alert screen's "New message" and app name); Unit tests (ignores message text, collects and shares nothing: code and tests only) |
| Free; code public on GitHub under the PolyForm Strict License | This repository is public; [`LICENSE.md`](../../LICENSE.md). PolyForm Strict isn't an open-source licence, so the listing says "public", not "open source". | Rule |
| Please note: an aid, not a safety device; can miss a message or flash late; photosensitive epilepsy | The owner asked for this protection (11 November 2026); `README.md`, "Disclaimer". The reasons named are real: setup permissions, battery settings ([`docs/feasibility.md`](../feasibility.md)). | Rule |

Telegram: Beacon supports Telegram messages too, but that has never been
tested on a phone, so the listing doesn't mention it (spec section 2).
