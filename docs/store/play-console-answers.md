# Play Console answers

Every field and answer Claude will enter in Play Console for Beacon 1.0.0,
following the spec
([`2026-11-11-play-release-design.md`](../specs/2026-11-11-play-release-design.md),
section 8). Nothing is entered until the owner approves this page. If Play
asks something this page doesn't cover, Claude stops and asks.

| | |
|---|---|
| Status | Approved by the owner in chat, 11 November 2026 |
| Developer account | "Bernhard Software" (personal). Its account ID stays in Play Console. |

## 1. Create app

| Field | Answer |
|---|---|
| App name | Beacon: Visual Message Alerts |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free. (A free app can't later be changed to paid.) |
| Developer Program Policies declaration | **The owner ticks it**, after saying so in chat. |
| US export laws declaration | **The owner ticks it**, after saying so in chat. |

## 2. Main store listing

| Field | Answer |
|---|---|
| App name, short description, full description | From [`listing.md`](listing.md), each exactly as written between its markers |
| App icon | `docs/store/graphics/icon-512.png` |
| Feature graphic | `docs/store/graphics/feature-1024x500.jpg` |
| Phone screenshots, in this order | `docs/store/graphics/screenshots/01-home.jpg`, `02-alert-settings.jpg`, `03-test-alert.jpg`, `04-finish-setup.jpg`, `05-widget.jpg` |
| Tablet and Chromebook screenshots, video | None |

Uploads open the system's file dialog, which Claude can't operate: at each
one Claude names the file, and the owner picks it.

## 3. Store settings

| Field | Answer |
|---|---|
| App category | Communication |
| Tags | None |
| Contact email | Given by the owner in chat at this step. Shown publicly on the listing. |
| Contact phone | None |
| Website | https://github.com/yanivil/beacon |

## 4. App content

| Form | Answer | Why |
|---|---|---|
| Privacy policy | https://github.com/yanivil/beacon/blob/main/PRIVACY.md | It is on `main` once this branch is merged (Task 6). |
| App access | All functionality is available without any access restrictions | No login or account. The setup permissions are granted on the phone, not by an account. |
| Ads | No, the app doesn't contain ads | No ads library, no internet permission. |
| Content rating | See section 4.1 | |
| Target audience and content | Age groups: **18 and over** only. The store listing doesn't unintentionally appeal to children. | Beacon is for adults; this keeps it out of the Families policy's extra rules. |
| News app | No | |
| COVID-19 contact tracing and status | Not such an app | |
| Data safety | See section 4.2 | |
| Advertising ID | No, the app doesn't use the advertising ID | No ads or analytics library. |
| Government app | No | |
| Financial features | The app provides no financial features | |
| Health | The app has no health features | Beacon is an accessibility tool, not a health app. |

### 4.1 Content rating questionnaire

| Question | Answer |
|---|---|
| Email for the rating authority (IARC) | The contact email from section 3 |
| Category | All other app types (utility, productivity, communication or other) |
| Violence, blood or gore | No |
| Sexual content or nudity | No |
| Bad language | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Crude humour | No |
| Gambling or simulated gambling | No |
| Users can interact or exchange content with other users | No. Beacon shows no messages and sends nothing; the message apps do that. |
| Shares the user's location with other users | No |
| Users can buy digital goods | No |
| Unrestricted internet access (a web browser or search engine) | No |

Expected result: Everyone (IARC 3+, PEGI 3, ESRB Everyone).

### 4.2 Data safety

| Question | Answer | Why |
|---|---|---|
| Does the app collect or share any of the required user data types? | **No** | Play counts data as collected only when it leaves the phone. Beacon has no internet permission (the release manifest has none), handles notifications only on the phone and keeps nothing from them, and its settings stay on the phone with backup off ([`PRIVACY.md`](../../PRIVACY.md)). |

With "No", Play doesn't ask about encryption in transit or data deletion.
If it asks anyway, Claude stops and asks.

## 5. Internal testing

| Field | Answer |
|---|---|
| Testers | An email list named "Beacon testers". The owner gives the addresses in chat or types them in; they never go into the repository. |
| Feedback URL or email | The contact email from section 3 |
| App signing | Play App Signing with a Google-generated key (the default) |
| Automatic protection | **On** (asked on the first release form; the owner chose to keep it on, 12 November 2026). Google adds an installer check to the copy Play delivers; [`PRIVACY.md`](../../PRIVACY.md) says so. |
| App bundle | `app/build/outputs/bundle/release/app-release.aab`, built from `main` after the merge (Task 6) |
| Release name | 1.0.0 (1); then 2 (1.0.1), Play's suggestion |
| Release notes (en-US) | First test version. For 1.0.1: "Adds a Privacy section at the end of Alert settings, with a button that opens the privacy policy." |
| Rollout | Only after the owner says yes in chat |

## 6. Not in this round

Countries and regions for production, closed testing, pre-registration,
and the production application. They are in
[`release-checklist.md`](release-checklist.md).
