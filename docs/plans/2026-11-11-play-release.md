# Play Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Beacon 1.0.0, signed with an upload key kept outside the repository, rolled out to Google Play's internal testing track, with the listing, graphics, privacy policy and Play forms ready for the public release.

**Architecture:** A `release` signing config in `app/build.gradle.kts` reads four Gradle properties from the owner's `~/.gradle/gradle.properties`, and a `checkReleaseSigning` task stops a release build that lacks them. Store texts, the privacy policy, the graphics (rendered from HTML with headless Chrome) and the Play Console answer sheet live in the repository. Play Console is filled in the desktop app's browser pane, where the owner is signed in, only after the owner approves the answer sheet.

**Tech Stack:** Gradle Kotlin DSL with AGP 9.5.1, the JDK's `keytool`, the Android SDK's `apksigner` and `aapt2` (build-tools 36.1.0), `adb` with the emulator `emulator-5554`, headless Google Chrome, and macOS `sips`.

**Spec:** [`docs/specs/2026-11-11-play-release-design.md`](../specs/2026-11-11-play-release-design.md)

## Global Constraints

- Follow `CLAUDE.md` in full. Work on the branch `feature/play-release`; never commit to `main`; every change has a PR draft (Title, Context/Why, Summary of Changes, Test Steps, Doc Impact); update README, CHANGELOG `[Unreleased]` and affected specs with every change.
- `main` is protected by the "Protect main" ruleset: changes reach it only through a pull request.
- GitHub access only with the repo-scoped keychain token, as `CLAUDE.md` shows. Never create, change or delete keychain entries.
- **Secrets:** never print, log, echo or commit the upload key's password, or write it anywhere except `~/.gradle/gradle.properties`. Never show the contents of that file.
- Testers' email addresses go into Play Console only, never into the repository or a commit.
- Results on a device are labelled "Tested on emulator beacon-api36, Android 16", "Tested on Redmi 15C 5G, Android 16" or "Not tested".
- `versionName` "1.0.0", `versionCode` 1. Application id stays `dev.beacon`.
- Gradle property names, exactly: `BEACON_UPLOAD_STORE_FILE`, `BEACON_UPLOAD_STORE_PASSWORD`, `BEACON_UPLOAD_KEY_ALIAS`, `BEACON_UPLOAD_KEY_PASSWORD`.
- Upload key: `~/.android-keys/beacon-upload.jks`, PKCS12, alias `beacon-upload`, RSA 4096, validity 10000 days, subject `CN=Beacon upload key`.
- Shrinking (`isMinifyEnabled`, `isShrinkResources`) stays off.
- Store texts: app name "Beacon: Visual Message Alerts" (29 characters); short description at most 80 characters; full description at most 4,000; English only; name SMS and WhatsApp, never claim Telegram.
- Colours for graphics: yellow `#FFD60A`, near-black `#0B0B0D` (13.93:1). The bolt path is `M13 2L3 14h9l-1 8 10-12h-9l1-8z`, filled and stroked 2 with round joins, as in `app/src/main/res/drawable/ic_launcher_foreground.xml`.
- Asset formats: icon 512 × 512 PNG with an alpha channel; feature graphic 1024 × 500 and screenshots 1080 × 2160, without alpha (JPEG).
- Nothing in Play Console is submitted, saved or rolled out until the owner has approved `docs/store/play-console-answers.md`. The two legal declarations at app creation, and the final "Start rollout", each need the owner's yes in chat at that moment.

## Review Focus

1. **A machine without the four properties** (a fresh clone, a contributor): the debug build and unit tests must work unchanged, and a release build must stop early with a message naming the missing properties. Pinned in Task 1, Steps 6 and 8.
2. **The password leaking** into the repository, the build log or the chat. Pinned in Task 1, Steps 2, 3 and 9.
3. **Play refusing an asset** because of its size, transparency, aspect ratio or text length. Pinned in Task 3, Step 4, and Task 4, Step 5.
4. **The listing or the privacy policy saying something the app doesn't do** (Telegram, wrong speeds, a network feature). Pinned in Task 3, Step 3.
5. **Release-only behaviour:** the only `BuildConfig.DEBUG` branch is the decision log in `BeaconApp.kt:132`. Pinned in Task 2, Steps 1 and 6.

## File Map

| File | Task | Responsibility |
|---|---|---|
| `~/.android-keys/beacon-upload.jks` (outside the repo) | 1 | The upload key |
| `~/.gradle/gradle.properties` (outside the repo) | 1 | The four signing properties |
| `app/build.gradle.kts` | 1 | Version, `release` signing config, `checkReleaseSigning` |
| `docs/checks/2026-11-play-release.md` | 2, 9, 10 | Smoke test, Play setup and phone check record |
| `PRIVACY.md` | 3 | Privacy policy linked from Play |
| `docs/store/listing.md` | 3 | Store texts, release notes |
| `docs/store/release-checklist.md` | 3 | The later steps |
| `docs/store/graphics/src/icon.html`, `feature.html` | 4 | Sources of the icon and the feature graphic |
| `docs/store/graphics/render.sh` | 4 | Renders both with headless Chrome |
| `docs/store/graphics/icon-512.png`, `feature-1024x500.jpg`, `screenshots/0N-*.jpg` | 4 | The assets |
| `docs/store/play-console-answers.md` | 5 | Every Play Console answer, for the owner's approval |
| `README.md`, `CHANGELOG.md`, `SECURITY.md`, the spec | 1, 3, 10 | Docs |

---

### Task 1: Upload key, release signing and version 1.0.0

**Files:**
- Outside the repo: create `~/.android-keys/beacon-upload.jks`; append to `~/.gradle/gradle.properties`
- Modify: `app/build.gradle.kts`, `README.md` (the "Building" section), `CHANGELOG.md`

**Interfaces:**
- Produces: the Gradle task `:app:checkReleaseSigning`; `./gradlew :app:bundleRelease` →
  `app/build/outputs/bundle/release/app-release.aab`; `./gradlew :app:assembleRelease` →
  `app/build/outputs/apk/release/app-release.apk`. Both are signed with the upload key.

- [ ] **Step 1: Check that nothing exists yet**

Run: `test -e ~/.android-keys/beacon-upload.jks && echo EXISTS; grep -c '^BEACON_UPLOAD_' ~/.gradle/gradle.properties 2>/dev/null`
Expected: no `EXISTS`, and a count of 0 (or no file). If either exists, stop and ask the owner: never overwrite a key.

- [ ] **Step 2: Create the key and the properties without showing the password**

Run as one command, exactly:

```bash
umask 077 && mkdir -p ~/.android-keys && \
P="$(openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-40)" && \
BEACON_KS_PASS="$P" keytool -genkeypair -storetype PKCS12 \
  -keystore ~/.android-keys/beacon-upload.jks -alias beacon-upload \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Beacon upload key" \
  -storepass:env BEACON_KS_PASS -keypass:env BEACON_KS_PASS >/dev/null 2>&1 && \
{ printf '\n# Beacon Play upload key (docs/specs/2026-11-11-play-release-design.md, section 4)\n'; \
  printf 'BEACON_UPLOAD_STORE_FILE=%s\n' "$HOME/.android-keys/beacon-upload.jks"; \
  printf 'BEACON_UPLOAD_STORE_PASSWORD=%s\n' "$P"; \
  printf 'BEACON_UPLOAD_KEY_ALIAS=beacon-upload\n'; \
  printf 'BEACON_UPLOAD_KEY_PASSWORD=%s\n' "$P"; } >> ~/.gradle/gradle.properties && \
chmod 600 ~/.gradle/gradle.properties ~/.android-keys/beacon-upload.jks; unset P; echo done
```

Expected: `done` and nothing else.

- [ ] **Step 3: Verify the key and the permissions**

Run: `BEACON_KS_PASS="$(sed -n 's/^BEACON_UPLOAD_STORE_PASSWORD=//p' ~/.gradle/gradle.properties)" keytool -list -keystore ~/.android-keys/beacon-upload.jks -storepass:env BEACON_KS_PASS | grep -E 'beacon-upload|Keystore type'; stat -f '%Sp %N' ~/.gradle/gradle.properties ~/.android-keys/beacon-upload.jks`
Expected: `Keystore type: PKCS12`, a `beacon-upload, …, PrivateKeyEntry` line, and `-rw-------` for both files.

- [ ] **Step 4: Show that the check doesn't exist yet**

Run: `./gradlew :app:checkReleaseSigning`
Expected: FAIL with "Cannot locate tasks that match ':app:checkReleaseSigning'" (or similar).

- [ ] **Step 5: Implement signing and the version in `app/build.gradle.kts`**

- `versionCode = 1`, `versionName = "1.0.0"`.
- A list of the four property names; a helper that reads each with `providers.gradleProperty(name)` and treats a blank value as missing.
- `signingConfigs { create("release") { … } }` that sets `storeFile`, `storePassword`, `keyAlias` and `keyPassword` only when all four are present.
- `buildTypes { release { signingConfig = signingConfigs.getByName("release") } }`; shrinking stays off.
- `val checkReleaseSigning by tasks.registering` that fails with `GradleException` when any property is missing. The message is exactly: `Release signing needs these Gradle properties in ~/.gradle/gradle.properties: <missing names, comma-separated>. See docs/specs/2026-11-11-play-release-design.md, section 4.` It never prints a value.
- Every `preReleaseBuild` task depends on `checkReleaseSigning` (`tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseSigning) }`).
- Comments, as `CLAUDE.md` asks: a block comment explaining why the key lives outside the repository and why shrinking stays off, and a short comment on the helper and the task.

- [ ] **Step 6: Verify that a missing property is named**

Run: `./gradlew :app:checkReleaseSigning -PBEACON_UPLOAD_KEY_ALIAS= 2>&1 | grep 'Release signing needs'`
Expected: one line naming `BEACON_UPLOAD_KEY_ALIAS` and no other property.

Run: `./gradlew :app:assembleRelease -PBEACON_UPLOAD_STORE_PASSWORD= -PBEACON_UPLOAD_KEY_PASSWORD= 2>&1 | grep -E 'Release signing needs|BUILD'`
Expected: the message naming exactly those two properties, and `BUILD FAILED`.

- [ ] **Step 7: Verify the signed outputs**

Run: `./gradlew :app:checkReleaseSigning :app:bundleRelease :app:assembleRelease`
Expected: `BUILD SUCCESSFUL`.

Run: `~/Library/Android/sdk/build-tools/36.1.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk | grep 'certificate DN'; keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep Owner; ~/Library/Android/sdk/build-tools/36.1.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk | head -1`
Expected: `CN=Beacon upload key` twice; then `package: name='dev.beacon' versionCode='1' versionName='1.0.0'`.

- [ ] **Step 8: Verify that debug and the tests didn't change**

Run: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, 374 tests with 0 failures, and lint "No issues found".

- [ ] **Step 9: Verify that no secret is in the repository**

Run: `git status --porcelain; git diff | grep -c -F "$(sed -n 's/^BEACON_UPLOAD_STORE_PASSWORD=//p' ~/.gradle/gradle.properties)"`
Expected: only `app/build.gradle.kts` (and the docs from Step 10) changed, and a count of `0`.

- [ ] **Step 10: Docs**

- README "Building": how to build a release bundle; where the key and properties live; that a release build stops without them; a reminder to back both up; that debug needs nothing new.
- CHANGELOG `[Unreleased]` Added: release signing, the check, and the version 1.0.0.

- [ ] **Step 11: Commit**

```bash
git add app/build.gradle.kts README.md CHANGELOG.md
git commit -m "Release signing from the owner's Gradle properties, and version 1.0.0"
```

---

### Task 2: Release smoke test on the emulator

**Files:**
- Create: `docs/checks/2026-11-play-release.md` (sections "Release build on the emulator" and "Not tested")

**Interfaces:**
- Consumes: `app/build/outputs/apk/release/app-release.apk` from Task 1.
- Produces: the emulator running the release build, set up, for Task 4's screenshots.

- [ ] **Step 1: Confirm that the decision log is the only debug-only code**

Run: `grep -rn 'BuildConfig.DEBUG' app/src/main`
Expected: only `BeaconApp.kt:132`. If there are others, list them in the record and add a smoke check for each.

- [ ] **Step 2: Install the release build**

Run: `adb -s emulator-5554 uninstall dev.beacon; adb -s emulator-5554 install app/build/outputs/apk/release/app-release.apk`
Expected: `Success`. The debug build's signature differs, so the uninstall is needed.

- [ ] **Step 3: Checks 1 and 2: Home, setup card, test alert**

Open Beacon (`adb -s emulator-5554 shell monkey -p dev.beacon 1`). Take a screenshot and look at it.
Expected: Home with the Finish setup card. Then run the test alert from Alert settings and take screenshots during it and after it.
Expected: it flashes and then ends.

- [ ] **Step 4: Finish setup over adb**

Run:

```bash
adb -s emulator-5554 shell cmd notification allow_listener dev.beacon/dev.beacon.alert.BeaconListener
adb -s emulator-5554 shell appops set dev.beacon SYSTEM_ALERT_WINDOW allow
adb -s emulator-5554 shell dumpsys deviceidle whitelist +dev.beacon
```

Reopen Beacon. Expected: no setup card, and "Visual alerts ON".

- [ ] **Step 5: Check 3: the widget**

Place Beacon's widget on the home screen (long-press → Widgets → Beacon), then tap it.
Expected: it shows PAUSED with a resume time. Tap it again: it shows ON.

- [ ] **Step 6: Checks 4 and 5: an SMS with the screen off, and no decision log**

Run: `adb -s emulator-5554 logcat -c; adb -s emulator-5554 shell input keyevent KEYCODE_SLEEP; sleep 3; adb -s emulator-5554 emu sms send 5551234 "Release check"; sleep 5; adb -s emulator-5554 shell dumpsys activity activities | grep -c 'dev.beacon/.alert.AlertActivity'; adb -s emulator-5554 logcat -d -s Beacon:D | grep -c 'D/Beacon'`
Expected: an AlertActivity count of 1 or more, and a `D/Beacon` count of 0.

- [ ] **Step 7: Record**

Write `docs/checks/2026-11-play-release.md` with a setup table (emulator, build, commit), the five checks labelled "Tested on emulator beacon-api36, Android 16", and "Not tested" for: the camera light (the emulator has none), the release build on a phone before Task 9, and Telegram.

- [ ] **Step 8: Commit**

```bash
git add docs/checks/2026-11-play-release.md
git commit -m "Release build smoke test on the emulator"
```

---

### Task 3: Privacy policy, store texts and the later-steps checklist

**Files:**
- Create: `PRIVACY.md`, `docs/store/listing.md`, `docs/store/release-checklist.md`
- Modify: `SECURITY.md` (link to `PRIVACY.md`), `README.md` (a new "Google Play" section), `CHANGELOG.md`

**Interfaces:**
- Produces: the privacy URL `https://github.com/yanivil/beacon/blob/main/PRIVACY.md`; `listing.md` with headed fields that Task 7 pastes into Play Console.

- [ ] **Step 1: Write `PRIVACY.md`**

Content from spec section 7, consistent with `SECURITY.md`'s "What the Beacon app reads and keeps", in plain words, with the date "11 November 2026". For contact, use the repository's private "Report a vulnerability" and its issues.

- [ ] **Step 2: Write `docs/store/listing.md`**

- **App name:** `Beacon: Visual Message Alerts`.
- **Short description**, exactly: `Flashes your screen and camera light when an SMS or WhatsApp message arrives.` (77 characters).
- **Full description:** the content list in spec section 5.
- **Category:** Communication. **Price:** Free. **Website:** `https://github.com/yanivil/beacon`. **Contact email:** "asked at the form".
- **Release notes for 1.0.0:** `First test version.`

Take every fact from the code and the checks records, not from memory: the speed presets (Slow 30, Medium 60, Fast 90 blinks a minute), the colours and lengths offered (`app/src/main/res/values/strings.xml`), when Beacon flashes (screen off or locked) and when it doesn't, the widget's 1-hour pause, the heat stop, and the setup steps.

- [ ] **Step 3: Check claims against the app**

Add a table "Claims and their sources" at the end of `listing.md`. List each claim in the full description, with the file or checks record that shows it. Then run: `grep -n -i telegram docs/store/listing.md`
Expected: Telegram appears only in the sources table's note that the listing doesn't claim it.

- [ ] **Step 4: Check the lengths**

Run a Python one-liner that reads the three fields from `listing.md` and prints each one's length.
Expected: the name is 29, the short description is 77, and the full description is at most 4000.

- [ ] **Step 5: Write `docs/store/release-checklist.md`**

The four parts of spec section 10, in order, each with what to do in Play Console and what to record.

- [ ] **Step 6: Docs and commit**

- `SECURITY.md`: a line linking `PRIVACY.md`.
- README: a "Google Play" section with the state (internal testing being prepared) and links to the listing, privacy policy and checklist.
- CHANGELOG: Added entries.

```bash
git add PRIVACY.md docs/store/listing.md docs/store/release-checklist.md SECURITY.md README.md CHANGELOG.md
git commit -m "Privacy policy, store texts and the later-steps checklist"
```

---

### Task 4: Store graphics

**Files:**
- Create: `docs/store/graphics/src/icon.html`, `docs/store/graphics/src/feature.html`, `docs/store/graphics/render.sh`, `docs/store/graphics/icon-512.png`, `docs/store/graphics/feature-1024x500.jpg`, `docs/store/graphics/screenshots/01-home.jpg`, `02-alert-settings.jpg`, `03-test-alert.jpg`, `04-finish-setup.jpg`, `05-widget.jpg`

**Interfaces:**
- Consumes: the emulator with the release build set up (Task 2).
- Produces: the seven asset files Task 7 uploads.

- [ ] **Step 1: The two HTML sources**

- `icon.html`: a 512 × 512 page filled with `#FFD60A`, and the bolt as inline SVG in `#0B0B0D`. Size and place it as the launcher foreground does: a 24-unit bolt drawn 44/108 of the side tall, centred, so about 209 px.
- `feature.html`: a 1024 × 500 page in `#FFD60A`. On the left, the bolt about 240 px tall; beside it, "Beacon" in the system font, weight 800, about 120 px; under that, "See every message" at about 48 px. All text is `#0B0B0D`.

- [ ] **Step 2: `render.sh`**

It renders both with `"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1`:
- the icon with `--window-size=512,512 --default-background-color=00000000`, written to `icon-512.png`, which keeps an alpha channel;
- the feature graphic with `--window-size=1024,500` to a PNG, then `sips -s format jpeg -s formatOptions 92` to `feature-1024x500.jpg`.

It deletes its intermediate PNGs, and runs from any folder.

- [ ] **Step 3: Screenshots on the emulator**

Prepare:

```bash
adb -s emulator-5554 shell wm size 1080x2160
adb -s emulator-5554 shell settings put system font_scale 1.0
adb -s emulator-5554 shell settings put global sysui_demo_allowed 1
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command enter
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb -s emulator-5554 shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
```

Capture each screen with `adb -s emulator-5554 exec-out screencap -p > /tmp/x.png`, then convert with `sips -s format jpeg -s formatOptions 92` into the screenshot files, in the order of the file list:
1. Home, alerts ON, with the app rows.
2. Alert settings, top of the screen.
3. The test alert, in its colour phase.
4. The Finish setup card: revoke notification access first with `cmd notification disallow_listener`, then grant it again afterwards.
5. The home screen with the widget showing ON.

Afterwards: `wm size reset`, the demo `exit` command, and `font_scale` back to its old value.

- [ ] **Step 4: Run the render script**

Run: `bash docs/store/graphics/render.sh`
Expected: `icon-512.png` and `feature-1024x500.jpg` are written.

- [ ] **Step 5: Check sizes and formats**

Run: `for f in docs/store/graphics/icon-512.png docs/store/graphics/feature-1024x500.jpg docs/store/graphics/screenshots/*.jpg; do sips -g pixelWidth -g pixelHeight -g hasAlpha -g format "$f" | tr '\n' ' '; echo; done; ls -l docs/store/graphics/icon-512.png`
Expected:
- the icon: 512 × 512, png, `hasAlpha: yes`, under 1 MB;
- the feature graphic: 1024 × 500, jpeg, `hasAlpha: no`;
- each of the five screenshots: 1080 × 2160, jpeg, `hasAlpha: no`.

- [ ] **Step 6: Look at every image**

Open each one. Check that it is right and readable, and that it shows no personal data and nothing that isn't Beacon's.

- [ ] **Step 7: Commit**

```bash
git add docs/store/graphics
git commit -m "Store graphics: icon, feature graphic and five screenshots"
```

---

### Task 5: The Play Console answer sheet, and the owner's approval

**Files:**
- Create: `docs/store/play-console-answers.md`

- [ ] **Step 1: Write the sheet**

For each Play Console page, give every field and the answer to enter:
- **Create app:** the fields, plus the two declarations marked "owner ticks";
- **Main store listing:** pointing to `listing.md` and the graphics files;
- **Store settings:** category and contact;
- **App content:** privacy policy, app access, ads, content rating (each questionnaire question and its answer: category "Utility, Productivity, Communication, or Other", then No to violence, sexuality, language, controlled substances, gambling, user interaction, sharing location, and digital purchases), target audience (18 and over), data safety (no data collected or shared, with the reasons from spec section 8), advertising ID (no), and government app, financial features, health and news (no);
- **Internal testing:** an email list named "Beacon testers", the release name `1.0.0 (1)`, and the release notes.

- [ ] **Step 2: Commit, then ask the owner to approve the sheet. STOP until they do.**

```bash
git add docs/store/play-console-answers.md
git commit -m "Play Console answer sheet for the owner's approval"
```

Expected: the owner says the sheet is approved, or asks for changes, which are made and committed before going on.

---

### Task 6: Review and merge, then build from `main`

- [ ] **Step 1: Whole-branch review** (the execution method's final review). Fix and commit its findings.
- [ ] **Step 2: Open the PR and merge it**, with `gh` and the keychain token, as `CLAUDE.md` shows. The PR draft names no design/rules conflict, unless one was found.
- [ ] **Step 3: Rebuild from `main`**

Run: `git checkout main && git pull --ff-only && ./gradlew :app:bundleRelease && git rev-parse --short HEAD`
Expected: `BUILD SUCCESSFUL`. Note the commit, which Task 10 records.

---

### Task 7: Create the app in Play Console

Done in the browser pane, following the approved sheet. Uploads open the system's file dialog, which Claude can't operate. At each upload, Claude names the file and its full path, and the owner picks it.

- [ ] **Step 1: Ask the owner for the public contact email,** and whether to tick the two declarations. Wait for their yes.
- [ ] **Step 2: Create the app** (All apps → Create app) with the sheet's fields. If Play reports that the package name is taken, this shows only at upload (Task 8, Step 3).
- [ ] **Step 3: Main store listing:** paste the texts from `listing.md`. The owner uploads the icon, the feature graphic and the five screenshots. Save.
- [ ] **Step 4: Store settings and App content:** enter each form as the sheet says, and save each one. If a question isn't on the sheet, stop and ask.
- [ ] **Step 5: Check the dashboard:** every setup task for internal testing shows as done. Screenshot the dashboard for the record.

---

### Task 8: Internal testing release

- [ ] **Step 1: Testers:** Testing → Internal testing → Testers → create the list "Beacon testers". The owner types or pastes the addresses. They are never written to a file.
- [ ] **Step 2: Create a release.** Accept Play App Signing (Google-generated key).
- [ ] **Step 3: Upload the bundle.** The owner picks `app/build/outputs/bundle/release/app-release.aab` (built from `main` in Task 6). Expected: Play accepts it as `dev.beacon`, version 1.0.0 (1). If Play says the package name is used by another app, stop and ask the owner: spec section 11.
- [ ] **Step 4: Release name and notes** as the sheet says. Review the release; note any warnings.
- [ ] **Step 5: Ask the owner before "Start rollout to Internal testing",** then roll out.
- [ ] **Step 6: Copy the testers' opt-in link** from the Testers tab and give it to the owner in chat. The link holds no secret.

---

### Task 9: Beacon from Play on the test phone

- [ ] **Step 1: The owner uninstalls the debug build** (Settings → Apps → Beacon → Uninstall), or says yes to Claude running `adb -s USMVLVUKM7SWRW7D uninstall dev.beacon`. Either way, the phone's Beacon settings are lost.
- [ ] **Step 2: The owner opens the opt-in link on the phone,** accepts, and installs Beacon from Play.
- [ ] **Step 3: Read the installed version.**
Run: `adb -s USMVLVUKM7SWRW7D shell dumpsys package dev.beacon | grep -E 'versionName|installerPackageName|installInitiatingPackageName'`
Expected: `versionName=1.0.0`, installed by `com.android.vending`.
- [ ] **Step 4: The owner finishes setup:**
  - notification access;
  - display over other apps;
  - the battery's No restrictions;
  - Background autostart on Xiaomi.

  Check that the Finish setup card is gone.
- [ ] **Step 5: One check:** the phone locked, the screen off, a WhatsApp or SMS message from another phone. Read the result from `dumpsys batterystats --history` (`+flashlight` with the app's new uid, and `+top` for `dev.beacon`), since the release build logs no decisions.
- [ ] **Step 6: Record the result** in `docs/checks/2026-11-play-release.md`, labelled "Tested on Redmi 15C 5G, Android 16" or "Not tested".

---

### Task 10: Docs, PR and merge

**Files:**
- Modify: `docs/checks/2026-11-play-release.md`, `CHANGELOG.md`, `README.md`, `docs/specs/2026-11-11-play-release-design.md`

- [ ] **Step 1: Docs**

- **The record:** the Play setup (dates, the dashboard's state, the release's version and warnings) and Task 9's result.
- **CHANGELOG:** move `[Unreleased]` into `[1.0.0] - 2026-11-<day>`, and start a new empty `[Unreleased]`.
- **README:** the "Google Play" section says 1.0.0 is on internal testing, and that the next step is the checklist.
- **The spec's status line:** internal testing done.
- **Backups:** remind the owner, in the PR and in chat, to back up the keystore and the four properties.

- [ ] **Step 2: Commit on a new branch, open the PR and merge it** with the keychain token.
