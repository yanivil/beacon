package dev.beacon.alert

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.beacon.BeaconApp

/*
 * Why this file: Android tells Beacon about messages only through a
 * notification listener. The service stays thin: it reads the few allowed
 * facts (never titles or text) and hands them to the MessageWatcher. On
 * HyperOS the system can drop the listener binding while the app stays
 * installed (docs/feasibility.md); the fix is to switch the component off and
 * on, which ListenerConnection does when the app comes to the front.
 */

/** Tracks whether the listener is bound, and repairs a dropped binding. */
object ListenerConnection {
    /** True while [BeaconListener] is bound; set only by the listener. */
    @Volatile var connected: Boolean = false

    /**
     * Re-binds the listener when access is granted but the system dropped it.
     *
     * @param context any context of this app.
     */
    fun reconnectIfDropped(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val component = ComponentName(context, BeaconListener::class.java)
        if (connected || !manager.isNotificationListenerAccessGranted(component)) return
        // Off then on makes the system bind the service again (the Step 1 fix); DONT_KILL_APP keeps Beacon running.
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }
}

/** The notification listener; forwards message events to the app's [MessageWatcher]. */
class BeaconListener : NotificationListenerService() {
    /** The app's watcher. */
    private val watcher: MessageWatcher get() = (application as BeaconApp).messageWatcher

    /** Marks the listener bound and records the notifications already showing. */
    override fun onListenerConnected() {
        ListenerConnection.connected = true
        // The framework returns null when not bound, and a SecurityException can occur if access was just revoked: both mean none.
        val active = try {
            activeNotifications?.toList().orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
        watcher.onConnected(active.map { factsOf(it.packageName, it.key, it.notification) })
    }

    /** Marks the listener unbound and asks the system to bind it again. */
    override fun onListenerDisconnected() {
        ListenerConnection.connected = false
        requestRebind(ComponentName(this, BeaconListener::class.java))
    }

    /**
     * Passes a new or updated notification to the watcher.
     *
     * @param sbn the notification, or null (ignored).
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // Facts are read now; only the decision may wait for the settings.
        watcher.onPosted(factsOf(sbn.packageName, sbn.key, sbn.notification))
    }

    /**
     * Tells the watcher a notification is gone.
     *
     * @param sbn the removed notification, or null (ignored).
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        watcher.onRemoved(sbn.key)
    }
}
