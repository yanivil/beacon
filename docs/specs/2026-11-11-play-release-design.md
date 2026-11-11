# Play release design: Beacon 1.0.0 on Google Play

| | |
|---|---|
| Status | Design approved by the owner in chat, 11 November 2026. This written spec is waiting for the owner's review. |
| Date | 11 November 2026 |
| Builds on | Step 2 ([`2026-11-08-step2-design.md`](2026-11-08-step2-design.md)), whose section 2 left Google Play, release signing and the store listing out of Step 2 |
| Play account | The owner's personal developer account, "Bernhard Software" |

This spec describes how Beacon reaches Google Play. It is the input for the
implementation plan. It changes no app behaviour.

## 1. Goal

Put Beacon 1.0.0 on Google Play's **internal testing** track for the
owner's few testers, and prepare in the same pass everything a public
release needs, so that going public later is mostly moving the app between
tracks.

### Done means

1. A signed release bundle (`.aab`) of Beacon 1.0.0 builds from `main`,
   with the upload key kept outside the repository.
2. The release build passes a smoke test on the emulator.
3. The app exists in the owner's Play Console with its store listing,
   graphics, privacy policy and every App content form filled in, each
   answer approved by the owner before it was submitted.
4. The 1.0.0 bundle is rolled out to an internal testing track, and the
   owner has the testers' opt-in link.
5. Beacon, installed from Play on the test phone, flashes for a message
   with the phone locked.
6. A checklist for the later steps (closed test, production access,
   production release) is in the repository.

## 2. The owner's decisions (11 November 2026)

- **Testers:** only a few people for now. So this round uses internal
  testing. A new personal Play account must run a closed test with at
  least 12 testers, opted in for 14 days in a row, before it can apply for
  production; that comes later.
- **Privacy policy:** a page in this repository, `PRIVACY.md`, now that the
  repository is public.
- **Upload key:** created by Claude in the owner's home folder, with its
  password in the owner's Gradle properties, never shown in chat. The
  owner backs up both.
- **The design, approved as presented, including three assumptions:**
  - The listing names SMS and WhatsApp only. Beacon supports Telegram, but
    Telegram has never been tested on a phone, so the listing doesn't
    claim it until it has.
  - The listing's public contact email is asked for when that form is
    reached.
  - The listing is in English only, like the app.

## 3. Rules this follows

- `CLAUDE.md` in full. In particular: work on `feature/` or `fix/`
  branches with a PR draft for each change; docs with every change; no
  keychain entries; never print, log or commit a secret.
- `main` is protected by the "Protect main" ruleset: every change reaches
  it through a pull request.
- Results on a device are labelled "Tested on <phone model>, Android
  <version>" or "Not tested".
- No personal data from the test phone or from testers is committed:
  testers' email addresses go into Play Console only.

## 4. Release build

### Version

`versionName` 1.0.0 and `versionCode` 1 in `app/build.gradle.kts`. Every
later upload to Play needs a higher `versionCode`.

### Upload key

- Created once with the JDK's `keytool`:
  - keystore `~/.android-keys/beacon-upload.jks`, PKCS12;
  - alias `beacon-upload`, RSA 4096, valid for 10,000 days;
  - subject `CN=Beacon upload key`, with no personal details;
  - a random password, generated on the machine and written straight into
    the properties below, so it never appears in chat or in a log.
- Its details live in `~/.gradle/gradle.properties`, which only the owner
  can read (`chmod 600`), as four properties:
  `BEACON_UPLOAD_STORE_FILE`, `BEACON_UPLOAD_STORE_PASSWORD`,
  `BEACON_UPLOAD_KEY_ALIAS` and `BEACON_UPLOAD_KEY_PASSWORD`.
- **Play App Signing:** Google keeps the key that signs what users
  install. The upload key only proves an upload comes from the owner. If
  the upload key is lost, the owner can ask Google to reset it.
- **Backup:** the owner copies the keystore file and the four properties
  into a password manager or another safe place. The plan's last step
  reminds them.

### Signing in Gradle

- A `release` signing config reads the four properties. If any is
  missing, a release build stops with a message naming what is missing.
  Debug builds and unit tests don't need them.
- The `release` build type uses that config.

### Shrinking

Code shrinking and resource shrinking stay **off** for 1.0.0. Beacon calls
a hidden Android method by reflection to read Xiaomi's Autostart (the
phone checks' P5), which shrinking could break. The app is small, so
there's little to gain. It can be revisited later with keep rules and a
test.

### Building and the smoke test

- `./gradlew :app:bundleRelease` builds
  `app/build/outputs/bundle/release/app-release.aab`. `.gitignore` already
  keeps `.aab` and `.apk` files out of the repository.
- **Smoke test on the emulator,** with a release APK
  (`./gradlew :app:assembleRelease`, the same code and key as the bundle):
  1. It installs, and opens on Home with the Finish setup card.
  2. The test alert runs and ends.
  3. The widget can be placed, and a tap pauses alerts.
  4. With setup finished and the screen off, an emulator SMS
     (`adb emu sms send`) flashes.
  5. The release build logs no Beacon decisions (`D/Beacon` is silent).
- The existing unit tests and lint pass, unchanged.

## 5. Store listing

The texts live in `docs/store/listing.md`, so they are reviewed like any
other change and can be pasted into Play Console.

| Field | Value or rule |
|---|---|
| App name (30 characters at most) | "Beacon: Visual Message Alerts" |
| Short description (80 at most) | One sentence: Beacon flashes the screen and the camera light when an SMS or WhatsApp message arrives. |
| Full description (4,000 at most) | Plain words for older deaf and hard-of-hearing people and their families: what Beacon does, when it flashes (screen off or locked) and when it doesn't (phone in use, calls, backups), the settings (colour, speed, length, per-app switches, pause), the widget, the three setup permissions and why each is needed, safety (at most 1.5 flashes a second, a light that stops when hot), and privacy (no internet, nothing collected). It names SMS and WhatsApp only. |
| Category | Communication |
| Price | Free |
| Contact email | The owner gives it at the form. Shown publicly. |
| Website | The repository on GitHub |
| Release notes for 1.0.0 | "First test version." |

Every claim in the listing must match the app and its checks records.

## 6. Graphics

All made on this computer with tools it already has (SVG drawn by hand,
turned into PNG with macOS's own tools); no new software. They live in
`docs/store/graphics/`.

| Asset | Size and format | Content |
|---|---|---|
| App icon | 512 × 512, 32-bit PNG | The launcher icon: the black bolt on the design's yellow. |
| Feature graphic | 1024 × 500, 24-bit PNG, no transparency | "Beacon" and the bolt on the yellow, with one short line such as "See every message", in the app's colours. |
| Phone screenshots | 4 or 5, 24-bit PNG, no transparency; each side 320 to 3,840 pixels, the long side at most twice the short one | From the emulator at the default text size, in English: Home with alerts ON, Alert settings, the test alert, the Finish setup card, and the widget. The emulator's 1080 × 2400 screens are cropped to 1080 × 2160 to fit the 2:1 limit. |

The screenshots come from the emulator, so they contain no personal data.
Contrast and text sizes are the app's own, which already meet the project's
rules.

## 7. Privacy policy

`PRIVACY.md` in the repository root, linked from Play as its GitHub page
(`https://github.com/yanivil/beacon/blob/main/PRIVACY.md`). It says, in
plain words and consistent with `SECURITY.md`:

- Beacon collects no personal data and shares none. It has no internet
  permission, no ads, no analytics and no crash reporting.
- Why it needs notification access, what it uses from a notification, and
  that it ignores and keeps no titles or message text (with the WhatsApp
  own-reply exception explained).
- Why it needs "Display over other apps" and the battery setting.
- Settings stay on the phone; backup is off.
- How to contact the developer, and the policy's date.

## 8. Play Console

Claude fills Play Console in the desktop app's browser pane, where the
owner is signed in. Nothing is submitted without the owner's approval:
the answers are written first in `docs/store/play-console-answers.md`, and
the owner approves that page once before Claude enters them.

### Creating the app

App name, default language English (United States), App (not game), Free.
The two declarations at creation, Google Play's Developer Program Policies
and US export laws, are the owner's: Claude asks before ticking them.

### App content answers (to be approved)

| Form | Planned answer | Why |
|---|---|---|
| Privacy policy | The `PRIVACY.md` link | Section 7 |
| App access | All functionality is available without special access | No login or account |
| Ads | No ads | None in the app |
| Content rating | Questionnaire answered "No" throughout | No violence, sexual content, gambling, user-generated content or sharing of location. Expected rating: Everyone (3+). |
| Target audience | 18 and over | The app is for adults; this keeps it out of the Families policy's extra rules. |
| Data safety | No data collected or shared | Everything stays on the phone. Data handled only on the device isn't "collected" in Play's sense. |
| Advertising ID | Not used | No ads library. |
| Government app, financial features, health, news | No | None applies. |

If Play asks for anything this table doesn't cover, Claude stops and asks.

### Store settings

Category and contact details as in section 5. Countries for the internal
test don't matter; for production they are decided later.

### Internal testing

1. Create an email list of testers. The owner gives the addresses in chat
   or types them in; they never enter the repository.
2. Create a release on the internal testing track, accept Play App Signing,
   upload `app-release.aab`, and add the release notes.
3. Review and roll out. Internal testing has no Google review, so it is
   available within minutes.
4. Give the owner the opt-in link for the testers.

## 9. The test phone

The debug build on the test phone is signed with a different key, so Play
can't update it. The owner uninstalls it, opts in with the link, installs
Beacon from Play, and grants its permissions again (notification access,
display over other apps, battery No restrictions, and on Xiaomi,
Background autostart). Then one check: a message with the phone locked
flashes. As in the cable-out checks, the result is read from the phone's
battery history and settings, since the release build writes no decision
log.

## 10. Later, not in this round

`docs/store/release-checklist.md` lists, in order:

1. A closed testing track with at least 12 testers who stay opted in for
   14 days in a row.
2. Applying for production access on the Play Console dashboard, and
   Google's review (often about a week).
3. The production release: countries, and a staged rollout.
4. For each later version: a higher `versionCode`, release notes, and the
   CHANGELOG section.

## 11. Risks

| Risk | What happens |
|---|---|
| `dev.beacon` is already taken on Play | Play refuses the upload. Changing the application id is the owner's decision; the plan stops there and asks. |
| The release build behaves differently from debug | The smoke test (section 4) and the check from Play (section 9) catch it before testers do. Shrinking is off. |
| Play's review questions notification access | Beacon's use is its core function and is explained in the app's setup card and in `PRIVACY.md`. If Play asks for more, Claude stops and asks. |
| The Data safety answer is wrong | It rests on the release manifest having no internet permission (checked) and on `PRIVACY.md`. A future network feature must update both. |
| The upload key is lost | Google can reset the upload key; the app keeps its Play signing key. |

## 12. Docs

- `README.md`: a "Google Play" section with the release state and where
  the store texts and checklist live.
- `CHANGELOG.md`: `[Unreleased]` becomes `[1.0.0] - <date>` when the
  bundle is uploaded.
- `docs/checks/`: a record of the smoke test, the Play Console setup and
  the check from Play.
- This spec's status line is updated as the round moves on.
