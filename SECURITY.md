# Security

Beacon is an Android app that turns incoming messages into visual alerts
for deaf and hard-of-hearing people. It has no internet permission, so it
can't open a network connection itself: the release build's merged
manifest asks only for the wake lock and "Display over other apps", plus a
permission AndroidX adds for Beacon's own internal broadcasts.

## Reporting a problem

Please report a security or privacy problem privately, not in a public
issue: on this repository's **Security** tab, choose **Report a
vulnerability**. Include the phone model, the Android version, Beacon's
version and the steps that show the problem.

Only the latest version of Beacon gets fixes.

## What the Beacon app reads and keeps

The privacy policy for Google Play says the same in plainer words:
[`PRIVACY.md`](PRIVACY.md).

This section is about the app in [`app/`](app). The test app in
[`spikes/`](spikes) was used only for the feasibility tests on a test phone
and isn't part of Beacon.

- **Notifications:** Android gives a notification listener every
  notification on the phone, from every app, not only the message apps
  Beacon flashes for. Beacon uses only which app posted it, its key, its
  kind (a new message or not) and its times. Android hands over the rest of
  the notification too, titles and message text included; Beacon ignores
  them, and doesn't keep or log them.
- **Your own WhatsApp replies:** WhatsApp marks a reply typed into its
  notification only by the sender's name. So for WhatsApp alone, Beacon
  compares the newest sender's name with yours, in memory, to avoid
  flashing for your own reply. Only the yes/no answer is kept, and nothing
  is logged.
- **The alert screen** shows "New message" and the app's name, never who
  sent the message or what it says.
- **Settings** (colour, speed, length, the pause and each app's settings)
  stay on the phone. Android backup and device-to-device transfer are
  turned off for Beacon.
- **Logs:** Beacon's own code logs nothing in release builds. Debug builds
  log only each notification's decision: to flash, with its source (SMS,
  WhatsApp or Telegram), or not to, with the reason. Android and the libraries Beacon uses
  keep their own logs.

## Keeping secrets out of this repository

The repository is public. Signing keys, passwords, tokens and service
credentials are never committed: [`.gitignore`](.gitignore) blocks the
usual files (`*.jks`, `*.keystore`, `*.pepk`, `keystore.properties`,
`.env`, `google-services.json` and others). Any signing key is kept
outside the repository.
