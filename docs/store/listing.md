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
Beacon is for people who are deaf or hard of hearing. When a text message (SMS) or a WhatsApp message arrives, your whole screen flashes a bright colour and your camera light blinks, so you see the message even when you can't hear it.

WHEN IT FLASHES
• When the screen is off or the lock screen is showing, even while the phone sleeps on battery.
• Not while you're using the phone: then the message app's own notification is your cue.
• Only for new messages. Never for calls, backups, progress bars or your own replies.

MAKE IT YOURS
• Screen colour: red, yellow, blue, green or white. Each colour has its name written on it. Use one colour for every app, or a different colour for each.
• Blink speed: Slow (30 a minute), Medium (60 a minute) or Fast (90 a minute).
• How long to flash: 1 minute, 5 minutes, a length you choose, or until you stop it.
• Turn each app ON or OFF.
• Try it any time with the 3-second Test alert.

PAUSE WHEN YOU NEED QUIET
• "Pause all alerts" on the Home screen stops every flash until you turn alerts back on.
• Or add the Beacon widget to your home screen: one tap pauses alerts for 1 hour, and they come back on by themselves. Tap again to turn them on sooner.

EASY TO READ AND USE
• Big, high-contrast text and large buttons.
• Every switch says ON or OFF in words, not only in colour.
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

Beacon is free and its code is open: github.com/yanivil/beacon
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

| Claim | Source |
|---|---|
| Flashes the whole screen and blinks the camera light for an SMS or a WhatsApp message | Spec section 1, "Done means" 2; [`docs/checks/2026-11-alerts-on-battery.md`](../checks/2026-11-alerts-on-battery.md) (both on battery, in Doze) |
| Flashes with the screen off or the lock screen showing; not while the phone is in use | `CLAUDE.md`, "When to flash"; [`docs/checks/2026-11-flash-when-not-in-use.md`](../checks/2026-11-flash-when-not-in-use.md); `IN_USE` in [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md) |
| Only new messages; never calls, backups, progress bars or your own replies | `MessageClassifier.kt` rules; [`docs/checks/2026-11-own-whatsapp-reply.md`](../checks/2026-11-own-whatsapp-reply.md); `CLAUDE.md`, "Scope" |
| Red, yellow, blue, green or white, each with its name; one colour or one per app | `strings.xml` (`colour_*`, `colour_mode_*`, `colour_hint`) |
| Slow 30, Medium 60, Fast 90 a minute | `CLAUDE.md`, "Blink safety"; `strings.xml` (`speed_*`) |
| 1 minute, 5 minutes, a chosen length, or until stopped | `strings.xml` (`duration_*`) |
| Each app ON or OFF; a 3-second Test alert | `strings.xml` (`apps_hint`, `test_button_length`) |
| "Pause all alerts" until turned back on | `strings.xml` (`alerts_pause_button`, `alerts_paused_body`) |
| Widget: one tap pauses for 1 hour, alerts come back by themselves, tap again for ON | Spec section 1, "Done means" 4; [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md); [`docs/checks/2026-11-play-release.md`](../checks/2026-11-play-release.md), check 3 |
| Big, high-contrast text; switches say ON or OFF; two tabs | `CLAUDE.md`, "Accessibility" (7:1 contrast, 20 sp body, 64 dp targets, ON/OFF text, two tabs) |
| Works with TalkBack and with the largest text size | Step 2 checks: TalkBack and font scale 2.0 on the emulator ([`docs/checks/2026-11-step2-pr1.md`](../checks/2026-11-step2-pr1.md), [`docs/checks/2026-11-step2-pr3.md`](../checks/2026-11-step2-pr3.md)); the owner judged Robolectric enough for the phone (11 November 2026) |
| At most 1.5 flashes a second, under the limit of 3 | `CLAUDE.md`, "Blink safety" (Fast 90 = 1.5 a second; WCAG 2.3.1) |
| The camera light stops if the phone gets hot; the screen keeps flashing | `strings.xml` (`test_camera_hot`); `README.md`, PR 2 and PR 4 |
| Every alert has Stop, and stops by itself after its length unless "Until I stop it" was chosen | `strings.xml` (`stop`); [`docs/checks/2026-11-pause-on-battery.md`](../checks/2026-11-pause-on-battery.md) (an alert ended by itself after 1 minute; another by a tap) |
| The setup steps and their reasons | `strings.xml` (`setup_*`), word for word |
| No internet, collects and shares nothing, no ads, ignores message text, alert screen shows only "New message" and the app | [`PRIVACY.md`](../../PRIVACY.md), [`SECURITY.md`](../../SECURITY.md); the release manifest ([`docs/checks/2026-11-public-repo-audit.md`](../checks/2026-11-public-repo-audit.md)) |
| Free, open code | This repository is public |

Telegram: Beacon supports Telegram messages too, but that has never been
tested on a phone, so the listing doesn't mention it (spec section 2).
