package dev.beacon.alert

import android.app.Notification
import androidx.core.app.NotificationCompat

/*
 * The one place that touches a Notification. It copies out the few facts the
 * rules need and nothing else: it never reads the title, the text or a
 * message's text, and it keeps no person's name.
 *
 * Why the owner check may compare names: the newest message is the owner's
 * when it has no sender (the MessagingStyle convention), or when its sender's
 * key is the owner's. WhatsApp sets no keys at all, and marks the owner's own
 * reply, typed into its notification, with a sender named as the owner. On
 * the test phone that reply flashed (the phone checks' P3). So when neither
 * the sender nor the owner has a key, their names are compared. The names are
 * read only for that comparison, in memory; only the yes/no answer is kept,
 * and nothing is logged. Cost: a contact whose name is exactly the owner's
 * name in the notification, in an app with no keys, would not flash.
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
    val sender = newest?.person
    val user = style?.user
    val userKey = user?.key
    val fromOwner = newest?.let {
        when {
            // No sender is the MessagingStyle convention for "the user".
            sender == null -> true
            // Keys decide whenever either side has one.
            userKey != null || sender.key != null -> userKey != null && sender.key == userKey
            // No keys at all (WhatsApp): the owner's own reply carries the owner's name.
            else -> {
                val userName = user?.name?.toString()
                userName != null && sender.name?.toString() == userName
            }
        }
    }
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
