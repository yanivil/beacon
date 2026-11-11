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
each WhatsApp notification, never a name or text. It showed why:

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

- **R1. When neither the newest sender nor the owner has a key, their names
  are compared.**
  - Keys still decide whenever either side has one. A sender with no key
    and the owner's name doesn't count as the owner when the owner has a
    key.
  - The names are read only for this comparison, in memory. Only the yes/no
    answer is kept, and nothing is logged; `NotificationFacts` still holds
    no names, which a test checks.
  - This narrows the reader's earlier promise of "never names" to "never
    keeps names". The spec's privacy section says so.
  - Cost if wrong: in an app that sets no keys, a message from a contact
    whose name is exactly the owner's name in the notification won't
    flash. In WhatsApp the owner's name there is the owner's own label
    (for example "You"), which another contact rarely has.

## Build machine

| Check | Result |
|---|---|
| New tests in `NotificationReaderTest` | 5 tests. Against the code before the fix, the one built like WhatsApp's reply failed: no keys, and the newest sender named as the owner, which must count as the owner. The other 4 passed and stay as guards: a keyless sender with another name isn't the owner; a sender with a key that isn't the owner's isn't the owner, even with the owner's name; a keyless sender with the owner's name isn't the owner when the owner has a key; and the facts hold no names. All 5 pass with the fix. |
| `./gradlew :app:testDebugUnitTest` | 338 tests, 0 failures: the 333 before, and the 5 new ones. |
| `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:lintDebug --rerun-tasks --warning-mode all` | No Kotlin compiler warnings (`Problem found` lines), and lint reports "No issues found". |

## Not tested

- **The fix on the test phone.** The diagnostic build's yes/no log gave the
  structure the tests copy, but no build with the fix has run there yet.
  The emulator has no WhatsApp, and Android doesn't let one app post
  another app's notifications, so the phone is the only place to check it.
- **Replies in WhatsApp groups**, and from **WhatsApp Business**.
- **Google Messages' own replies** from its notification. On the phone its
  incoming messages have a sender with no key, and the owner's name wasn't
  checked; if it marks replies the way WhatsApp does, this fix covers it.
- **Telegram.**
