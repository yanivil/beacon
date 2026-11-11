package dev.beacon.alert

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/*
 * The one place that touches a Notification. It copies out the few facts the
 * rules need and nothing else: it never reads the title, the text or a
 * message's text, and it keeps no person's name.
 *
 * Why the owner check may compare names, for WhatsApp only: the newest
 * message is the owner's when it has no sender (the MessagingStyle
 * convention), or when its sender's key is the owner's. WhatsApp sets no keys
 * at all, and marks the owner's own reply, typed into its notification, with
 * a sender named as the owner. On the test phone that reply flashed (the
 * phone checks' P3). So for WhatsApp, when neither side has a key, the two
 * names are compared. They are read only for that comparison, in memory;
 * only the yes/no answer is kept, and nothing is logged.
 *
 * Why only WhatsApp: a wrong "it's the owner's" silently drops a real alert,
 * which is worse for a deaf person than a flash for their own reply. Names
 * were seen to work only in WhatsApp, and other apps, such as SMS apps using
 * the older notification fields, also give their senders no keys. Cost: a
 * WhatsApp message whose sender's name is exactly the owner's label in
 * WhatsApp's notification (a namesake contact or group member) won't flash.
 */

/**
 * Reads the facts the classifier needs from a notification.
 *
 * @param packageName the app that posted the notification.
 * @param key the notification's key.
 * @param notification the notification to read.
 * @return the facts; they say "no messaging layout" if the layout cannot be read.
 */
fun factsOf(packageName: String, key: String, notification: Notification): NotificationFacts {
    val style = try {
        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
    } catch (_: RuntimeException) {
        // Another app's malformed extras must not crash the listener: treat as no MessagingStyle.
        null
    }
    // Largest timestamp wins; on a tie the later message in the list does (the index breaks the tie).
    val newest = style?.messages?.withIndex()
        ?.maxWithOrNull(compareBy({ it.value.timestamp }, { it.index }))?.value
    // A newest message means the style was read, so the compiler knows it isn't null here.
    val fromOwner = newest?.let { sentByOwner(it.person, style.user, packageName) }
    return NotificationFacts(
        packageName = packageName,
        key = key,
        ongoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
        groupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        category = notification.category,
        hasMessagingStyle = style != null,
        newestMessageTime = newest?.timestamp,
        newestFromOwner = fromOwner,
        whenMillis = notification.`when`,
    )
}

/**
 * Decides whether a message was sent by the phone's owner.
 *
 * @param sender the message's sender; null is MessagingStyle's convention for the owner.
 * @param user the owner, as the notification names them.
 * @param packageName the app that posted the notification; names are compared for WhatsApp only.
 * @return true for the owner's own message: no sender, or the owner's key, or, in WhatsApp
 *   with no keys on either side, the owner's non-blank name.
 */
internal fun sentByOwner(sender: Person?, user: Person?, packageName: String): Boolean {
    if (sender == null) return true
    // An empty key is no key.
    val userKey = user?.key?.takeIf { it.isNotEmpty() }
    val senderKey = sender.key?.takeIf { it.isNotEmpty() }
    // Keys decide whenever either side has one.
    if (userKey != null || senderKey != null) return userKey != null && senderKey == userKey
    if (packageName !in MessageSource.WHATSAPP.fixedPackages) return false
    // Two blank names would match anyone with no name, so a blank owner name never matches.
    val userName = user?.name?.toString()?.takeIf { it.isNotBlank() } ?: return false
    return sender.name?.toString() == userName
}
