package dev.beacon.spike

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager

/*
 * Why a notification listener: it is the one route that can see all of these apps through a single
 * mechanism, without the SMS and call-log permissions that Google Play restricts. Android keeps a
 * listener bound, so it can react with the app closed, and the test measures whether that holds up.
 *
 * Privacy: this test app reads other apps' notifications, so it records as little as it can. Apps
 * outside the test are ignored completely. Message text and all titles are never written, only
 * their length. Text is kept only for call notifications, because the open question is whether a
 * video call can be told from a voice call, and the wording may be the only clue. The log stays
 * private to this app until fetched over adb.
 */

/** Sees notifications from the tested apps, records what they contain and fires the test alert. */
class AlertListener : NotificationListenerService() {

    /** Records that Android bound the listener. */
    override fun onListenerConnected() {
        connected = true
        SpikeLog.write(this, "listener.connected", DeviceState.snapshot(this))
    }

    /** Records losing the listener and asks to be bound again. */
    override fun onListenerDisconnected() {
        connected = false
        SpikeLog.write(this, "listener.disconnected", DeviceState.snapshot(this))
        requestRebind(ComponentName(this, AlertListener::class.java))
    }

    /**
     * Records a tested app's notification and fires the alert when it should.
     *
     * @param sbn The posted notification.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        val source = AlertSource.classify(this, posted) ?: return
        val delay = System.currentTimeMillis() - posted.postTime
        SpikeLog.write(
            this,
            "notification.posted",
            "source=$source delayMs=$delay ${describe(posted)} ${DeviceState.snapshot(this)}",
        )
        val skip = skipReason(posted)
        if (skip != null) {
            SpikeLog.write(this, "alert.skipped", "source=$source why=$skip")
            return
        }
        // Only an adb test notification may carry a setup, never a real message's text.
        val overrides = if (source == AlertSource.SHELL_TEST) {
            posted.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        } else {
            null
        }
        AlertTrigger.fire(this, source, overrides)
    }

    /**
     * Records a tested app removing its notification, which is how a call ending would show up.
     *
     * @param sbn The removed notification.
     * @param rankingMap Unused.
     * @param reason Android's removal reason code.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        val removed = sbn ?: return
        val source = AlertSource.classify(this, removed) ?: return
        SpikeLog.write(this, "notification.removed", "source=$source reason=$reason")
    }

    /**
     * Decides whether a notification should not flash, because it isn't a new arrival.
     *
     * @param sbn The notification.
     * @return The reason to skip it, or null when it should flash.
     */
    private fun skipReason(sbn: StatusBarNotification): String? {
        val notification = sbn.notification
        return when {
            // A group summary repeats a message already seen: flashing would double-alert.
            notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 -> "groupSummary"
            // Call type 2 is a call already in progress, not one ringing.
            notification.extras.getInt(Notification.EXTRA_CALL_TYPE) == CALL_TYPE_ONGOING -> "callInProgress"
            else -> null
        }
    }

    /**
     * Describes a notification's structure without its private content.
     *
     * @param sbn The notification.
     * @return Space-separated key=value text for the log.
     */
    private fun describe(sbn: StatusBarNotification): String {
        val n = sbn.notification
        val extras = n.extras
        val channel = n.channelId.orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val callLike = n.category == Notification.CATEGORY_CALL || n.fullScreenIntent != null ||
            channel.contains("call", ignoreCase = true) || channel.contains("voip", ignoreCase = true)
        val isVideo = if (extras.containsKey(Notification.EXTRA_CALL_IS_VIDEO)) {
            extras.getBoolean(Notification.EXTRA_CALL_IS_VIDEO).toString()
        } else {
            "absent"
        }
        val actions = n.actions?.joinToString("|") { it.title.toString() }.orEmpty()
        return "pkg=${sbn.packageName} category=${n.category} channel=$channel " +
            "template=${extras.getString(Notification.EXTRA_TEMPLATE)?.substringAfterLast('$')} " +
            "ongoing=${sbn.isOngoing} fullScreen=${n.fullScreenIntent != null} " +
            "summary=${n.flags and Notification.FLAG_GROUP_SUMMARY != 0} " +
            "callType=${extras.getInt(Notification.EXTRA_CALL_TYPE)} callIsVideo=$isVideo " +
            "actions=[$actions] titleLen=${title.length} textLen=${text.length} " +
            "callText=[${if (callLike) text else "hidden"}]"
    }

    companion object {
        private const val CALL_TYPE_ONGOING = 2

        /** True while Android has this listener connected. A dead process reads as false. */
        @Volatile
        var connected = false
            private set

        /**
         * Makes Android reconnect a dropped listener. HyperOS refuses to reconnect it while the app
         * isn't running and Autostart is off, and then nothing retries, even once the app is running
         * again.
         *
         * requestRebind doesn't help: tested on the phone, it did nothing for a listener the system
         * had dropped. Switching the component off and on makes Android treat the listener as
         * changed and bind it afresh.
         *
         * @param context Any context.
         */
        fun reconnectIfDropped(context: Context) {
            val listener = ComponentName(context, AlertListener::class.java)
            val manager = context.getSystemService(NotificationManager::class.java)
            // Switching the component off would drop a healthy connection, so only do it when needed.
            if (connected || !manager.isNotificationListenerAccessGranted(listener)) return
            val packages = context.packageManager
            packages.setComponentEnabledSetting(
                listener,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            packages.setComponentEnabledSetting(
                listener,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            SpikeLog.write(context, "listener.reconnectRequested")
        }
    }
}

/** The alert sources Step 1 has to tell apart. */
enum class AlertSource {
    PHONE_CALL,
    SMS,
    WHATSAPP_MESSAGE,
    WHATSAPP_VIDEO_CALL,
    WHATSAPP_VOICE_CALL,
    WHATSAPP_CALL_KIND_UNKNOWN,
    TELEGRAM,
    SHELL_TEST;

    companion object {
        private const val WHATSAPP = "com.whatsapp"
        private const val TELEGRAM_PLAY = "org.telegram.messenger"
        private const val TELEGRAM_WEB = "org.telegram.messenger.web"

        // Posted with "adb shell cmd notification post", to trigger the real path repeatably.
        private const val SHELL = "com.android.shell"

        /**
         * Works out which tested source a notification came from.
         *
         * @param context Any context.
         * @param sbn The notification.
         * @return The source, or null for anything outside the test.
         */
        fun classify(context: Context, sbn: StatusBarNotification): AlertSource? {
            val pkg = sbn.packageName
            val isCall = sbn.notification.category == Notification.CATEGORY_CALL
            val dialer = context.getSystemService(TelecomManager::class.java).defaultDialerPackage
            return when {
                pkg == SHELL -> SHELL_TEST
                pkg == dialer && isCall -> PHONE_CALL
                pkg == Telephony.Sms.getDefaultSmsPackage(context) -> SMS
                pkg == WHATSAPP && !isCall -> WHATSAPP_MESSAGE
                pkg == WHATSAPP -> whatsAppCallKind(sbn)
                pkg == TELEGRAM_PLAY || pkg == TELEGRAM_WEB -> TELEGRAM
                else -> null
            }
        }

        /**
         * Reads a WhatsApp call's kind from Android's own call field, which doesn't depend on
         * language. The test shows whether WhatsApp fills it in.
         *
         * @param sbn A WhatsApp call notification.
         * @return The kind of call, or unknown when WhatsApp doesn't say.
         */
        private fun whatsAppCallKind(sbn: StatusBarNotification): AlertSource {
            val extras = sbn.notification.extras
            return when {
                !extras.containsKey(Notification.EXTRA_CALL_IS_VIDEO) -> WHATSAPP_CALL_KIND_UNKNOWN
                extras.getBoolean(Notification.EXTRA_CALL_IS_VIDEO) -> WHATSAPP_VIDEO_CALL
                else -> WHATSAPP_VOICE_CALL
            }
        }
    }
}
