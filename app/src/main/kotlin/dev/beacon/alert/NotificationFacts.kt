package dev.beacon.alert

/*
 * The only things the rules may know about a notification. Titles, message
 * text and names are deliberately absent: Beacon needs to know that a
 * message arrived, never what it says or who sent it (privacy rule). Keeping
 * this a plain data class also lets the classifier be tested without Android.
 */

/**
 * What the classifier needs from one notification.
 *
 * @property packageName the app that posted it.
 * @property key the notification's unique key, stable across updates.
 * @property ongoing whether it is an ongoing notification (for example a backup).
 * @property groupSummary whether it only summarises a group of notifications.
 * @property category the notification's category, or null.
 * @property hasMessagingStyle whether it uses the messaging layout.
 * @property newestMessageTime the newest message's time; null without a messaging layout or messages.
 * @property newestFromOwner whether the newest message was sent by the phone's owner; null without
 *   a messaging layout or messages.
 * @property whenMillis the notification's own `when` time.
 */
data class NotificationFacts(
    val packageName: String,
    val key: String,
    val ongoing: Boolean,
    val groupSummary: Boolean,
    val category: String?,
    val hasMessagingStyle: Boolean,
    val newestMessageTime: Long?,
    val newestFromOwner: Boolean?,
    val whenMillis: Long,
)
