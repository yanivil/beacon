# Google Play: the steps after internal testing

Beacon 1.0.0 starts on Play's internal testing track
([spec](../specs/2026-11-11-play-release-design.md), section 10). Because
the developer account is a new personal account, Google lets it publish to
everyone only after a closed test. These are the steps from there, in
order. Each one is done on a `feature/` or `fix/` branch with a PR, like
any other change, and recorded in `docs/checks/`.

## 1. Closed test: at least 12 testers for 14 days in a row

Before the closed test, which Google reviews:

- **A privacy policy link inside the app.** Play's User Data policy asks
  for the policy both in Play Console and within the app itself. Add a
  "Privacy policy" text button (a string in `strings.xml`, at least 64 dp
  tall, with a TalkBack label) that opens
  `https://github.com/yanivil/beacon/blob/main/PRIVACY.md` with
  `ACTION_VIEW`. That needs no internet permission: the browser opens it.
  It adds UI, so it goes on its own `feature/` branch with the owner's say,
  and its version gets a higher `versionCode`. The 1.0.0 internal test
  doesn't have it, since internal testing isn't reviewed. (Found by the
  final review of the Play release branch, I1.)
- **A contact that needs no GitHub account** in `PRIVACY.md`: the public
  contact email from the store listing.

- In Play Console: Test and release → Testing → Closed testing → create a
  track (for example "Beacon closed test").
- Add at least 12 testers, as an email list or a Google Group. They must
  opt in through the track's link and **stay opted in for 14 days in a
  row**. If the count drops below 12, the 14 days start again.
- Promote the release that passed internal testing to the track, or upload
  a newer one with a higher `versionCode`.
- Closed testing is reviewed by Google before testers can install it.
- Ask the testers to use Beacon and to send feedback. Google's production
  application asks about the test, so keep notes: how many testers, what
  they reported, and what changed.
- **Record:** the start date, the tester count each few days, and the end
  date.

## 2. Apply for production access

- When the dashboard shows the requirement as met, choose "Apply for
  production" on the Play Console dashboard and answer its questions about
  the test and the app.
- Google reviews the application, often within about a week.
- **Record:** the date applied, the answers given, and Google's decision.

## 3. The production release

- Countries: decide which ones (all, or a few to start).
- Release: promote the tested release to production, or upload a newer one.
  Use a staged rollout (for example 20% first) and watch Play Console's
  pre-launch report and Android vitals before going to 100%.
- Update `README.md`'s "Google Play" section with the store link.
- **Record:** the date, the countries, the rollout steps.

## 4. Every later version

1. Raise `versionCode` (and `versionName`) in `app/build.gradle.kts`.
2. Move `[Unreleased]` in `CHANGELOG.md` into a section for the new
   version, with its date.
3. Write short release notes in `docs/store/listing.md`.
4. Build with `./gradlew :app:bundleRelease`: the upload key's properties
   must be in `~/.gradle/gradle.properties` (see `README.md`, "Release
   builds for Google Play").
5. Test the release build (see
   [`docs/checks/2026-11-play-release.md`](../checks/2026-11-play-release.md)
   for the smoke test), upload it to a testing track first, then promote it.
6. If the app ever gains a network feature, ads or analytics, update
   [`PRIVACY.md`](../../PRIVACY.md), the Data safety form and
   [`docs/store/play-console-answers.md`](play-console-answers.md) before
   that release.
