package dev.beacon.alert

import android.app.Notification
import androidx.core.app.NotificationCompat

/*
 * The one place that touches a Notification. It copies out the few facts the
 * rules need and nothing else: it never reads the title, the text, a
 * message's text or a person's name. The owner check compares only whether
 * the sender is missing, or its key.
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
    val userKey = style?.user?.key
    // No sender is the MessagingStyle convention for "the user"; otherwise compare keys only (never names).
    val fromOwner = newest?.let { sender == null || (userKey != null && sender.key == userKey) }
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
