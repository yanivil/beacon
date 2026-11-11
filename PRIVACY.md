# Beacon privacy policy

Last updated: 11 November 2026

Beacon is an Android app that flashes the screen and the camera light when
a message arrives, for people who are deaf or hard of hearing. This page
explains what Beacon does with information on your phone.

## The short version

Beacon collects no personal information and shares none. It has no
internet permission, so it can't send anything off your phone. It has no
ads, no analytics and no crash reporting, and it has no account to sign in
to.

## What Beacon uses, and why

### Notification access

Beacon needs notification access to know when a message arrives. Android
then gives Beacon every notification on the phone, from every app. Beacon
uses only:

- which app posted the notification;
- its key, so the same message doesn't flash twice;
- its kind, so it flashes only for new messages and not for things like
  backups or progress bars;
- its times.

Android also hands over the rest of each notification, such as titles and
message text. Beacon ignores them. It doesn't store them, show them or
write them to a log.

One exception: WhatsApp marks a reply you type into its notification only
by your name. So, for WhatsApp alone, Beacon compares the name of the
newest message's sender with yours, in memory, so that your own reply
doesn't flash. Only the yes-or-no answer is kept, and nothing is logged.

The alert screen shows "New message" and the app's name. It never shows
who sent the message or what it says.

### Display over other apps

This lets the flashing screen appear over other apps and over the lock
screen. Beacon draws nothing else over other apps.

### Battery: No restrictions

This lets alerts arrive while the phone sleeps. On Xiaomi phones,
"Background autostart" does the same after a restart. Neither gives Beacon
access to any information.

### Camera light

Beacon turns the camera's light on and off to flash it. It never takes
pictures and never uses the camera to see anything.

## What Beacon keeps

Only your settings: the flash colour, the blink speed, how long to flash,
the pause, and each app's settings. They stay on your phone. Android
backup and device-to-device transfer are turned off for Beacon, so they
aren't copied anywhere either. Uninstalling Beacon deletes them.

## Children

Beacon is meant for adults and isn't directed at children.

## Changes

If this policy changes, the new version is published at this same address
with a new date. The history of every change is kept in this repository.

## Contact

To report a privacy or security problem privately, use "Report a
vulnerability" on this repository's
[Security tab](https://github.com/yanivil/beacon/security). For other
questions, open an [issue](https://github.com/yanivil/beacon/issues).
