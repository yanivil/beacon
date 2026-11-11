# The owner's own WhatsApp reply: checks

This page records the fix for the phone checks' defect P3, on
`fix/own-whatsapp-reply`: its ruling, with what it costs if wrong, and its
checks.

Results are labelled **"Tested on Redmi 15C 5G, Android 16"** or **"Not
tested"**.

## The defect

On the test phone the owner replied to a WhatsApp message from inside its
notification, and Beacon flashed for that reply
([`2026-11-phone-checks.md`](2026-11-phone-checks.md), P3). Beacon is meant
to ignore the owner's own messages (spec section 6, rule 6).

A diagnostic build on the phone logged only yes/no facts and counts about
each WhatsApp notification, never a name or text. It showed why (Tested on
Redmi 15C 5G, Android 16, 11 November 2026):

| Notification | Messages | Newest sender has a key | Newest sender's name equals the owner's | Owner has a key | Android's reply history | Beacon decided |
|---|---|---|---|---|---|---|
| Incoming message, 11:42:38 | 1 | No | No | No | No | Alert (correct) |
| After the owner's reply, 11:43:01 | 2 | No | **Yes** | No | No | Alert (wrong) |

Beacon counted a message as the owner's only if it had no sender, or if
its sender's key equalled the owner's key. WhatsApp sets no keys at all, so
neither held. WhatsApp marks the reply with a sender named as the owner
instead. It doesn't fill in Android's reply-history field either, so that
field can't be used.

## Ruling

- **R1. For WhatsApp only, when neither the newest sender nor the owner has
  a key, their names are compared.**
  - Keys still decide whenever either side has one. A sender with no key
    and the owner's name doesn't count as the owner when the owner has a
    key, and a sender with a key never counts by name.
  - An empty key counts as no key. A blank name never matches, so a
    blank-named sender can't pass for a blank-named owner.
  - **Why WhatsApp only:** a wrong "this is the owner's" silently drops a
    real alert, which is worse for a deaf person than a flash for their own
    reply. Names were seen to work only in WhatsApp. Other apps also give
    their senders no keys, such as SMS apps that use the older notification
    fields, where the owner's label may be their real name. So other apps
    keep the rule as it was.
  - The names are read only for this comparison, in memory. Only the yes/no
    answer is kept, and nothing is logged. `NotificationFacts` has no text
    field except the package, the key and the category, which a test
    checks. The reader's promise changes from "never reads names" to "keeps
    no names"; the spec's privacy section says so.
  - Cost if wrong, two ways:
    - A WhatsApp message whose sender's name is exactly the owner's label in
      WhatsApp's notification won't flash. That could be a contact or group
      member with the same name, if WhatsApp uses the owner's profile name
      as the label. The label's text wasn't logged on the phone.
    - If Google Messages marks its own replies the same way, they still
      flash; that stays Not tested, and the same rule can be extended to
      it.

## Build machine

| Check | Result |
|---|---|
| New tests in `NotificationReaderTest` | 8 tests, for the new `sentByOwner` and for `factsOf`. Against the code before the fix, the one built like WhatsApp's reply failed: no keys, and the newest sender named as the owner, which must count as the owner (in `com.whatsapp` and `com.whatsapp.w4b`). The others are guards: a keyless sender with another name isn't the owner; the same keyless reply from an SMS app isn't the owner; blank names, or an owner with no name, never match; a sender with a key never counts by name, nor does a keyless sender when the owner has a key; an empty key counts as no key; and `NotificationFacts` has no text field for a name. All 8 pass with the fix. |
| `./gradlew :app:testDebugUnitTest` | 341 tests, 0 failures: the 333 before, and the 8 new ones. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:lintDebug --rerun-tasks --warning-mode all` | No Kotlin compiler warnings (`Problem found` lines), and lint reports "No issues found". |

## Not tested

- **The fix on the test phone.** The diagnostic build's yes/no log gave the
  structure the tests copy, but no build with the fix has run there yet.
  The emulator has no WhatsApp, and Android doesn't let one app post
  another app's notifications, so the phone is the only place to check it.
- **Replies in WhatsApp groups**, and from **WhatsApp Business**.
- **What WhatsApp uses as the owner's label** in its notification: the
  diagnostic build didn't log names.
- **Google Messages' own replies** from its notification. On the phone its
  incoming messages have a sender with no key. Names aren't compared for
  it, so if it marks replies the way WhatsApp does, they still flash.
- **Telegram.**
