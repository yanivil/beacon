# Security

Beacon is an Android app that turns incoming messages into visual alerts
for deaf and hard-of-hearing people. It has no internet permission, so
nothing it reads can leave the phone.

## Reporting a problem

Please report a security or privacy problem privately, not in a public
issue: on this repository's **Security** tab, choose **Report a
vulnerability**. Include the phone model, the Android version, Beacon's
version and the steps that show the problem.

Only the latest version of Beacon gets fixes.

## What Beacon reads and keeps

- **Notifications:** Beacon's notification listener reads only what it
  needs to decide whether to flash: which app a notification came from,
  its key, its kind (a new message or not) and its times. It never reads a
  notification's title or text, or a message's text.
- **Your own WhatsApp replies:** WhatsApp marks a reply typed into its
  notification only by the sender's name. So for WhatsApp alone, Beacon
  compares the newest sender's name with yours, in memory, to avoid
  flashing for your own reply. Only the yes/no answer is kept, and nothing
  is logged.
- **The alert screen** shows "New message" and the app's name, never who
  sent the message or what it says.
- **Settings** (colour, speed, length, the pause and each app's settings)
  stay on the phone. Android backup is turned off for Beacon.
- **Logs:** release builds write no logs.

## Keeping secrets out of this repository

The repository is public. Signing keys, passwords, tokens and service
credentials are never committed: [`.gitignore`](.gitignore) blocks the
usual files (`*.jks`, `*.keystore`, `keystore.properties`, `.env`,
`google-services.json` and others), and the Google Play upload key lives
outside the repository.
