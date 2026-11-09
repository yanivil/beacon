package dev.beacon.alert

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import dev.beacon.BeaconApp
import java.io.IOException
import kotlinx.coroutines.launch

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

/**
 * The notification listener; forwards message events to the app's [MessageWatcher].
 * While bound it also listens for the screen turning on, to keep the stored pause and the widget current.
 */
class BeaconListener : NotificationListenerService() {
    /** The screen-on receiver while the listener is bound; null otherwise. */
    private var screenOnReceiver: BroadcastReceiver? = null

    /** The app's watcher. */
    private val watcher: MessageWatcher get() = (application as BeaconApp).messageWatcher

    /** Marks the listener bound, starts watching for the screen turning on, and records the notifications already showing. */
    override fun onListenerConnected() {
        ListenerConnection.connected = true
        registerScreenOnReceiver()
        // The framework returns null when not bound, and a SecurityException can occur if access was just revoked: both mean none.
        val active = try {
            activeNotifications?.toList().orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
        watcher.onConnected(active.map { factsOf(it.packageName, it.key, it.notification) })
    }

    /** Marks the listener unbound, stops watching the screen and asks the system to bind it again. */
    override fun onListenerDisconnected() {
        ListenerConnection.connected = false
        unregisterScreenOnReceiver()
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

    /** Removes the screen-on receiver if the service ends while still bound. */
    override fun onDestroy() {
        unregisterScreenOnReceiver()
        super.onDestroy()
    }

    /**
     * Registers a receiver that, each time the screen turns on, cleans up an ended timed pause
     * and redraws the widget (a stale widget would otherwise be seen when the phone is picked up).
     * ACTION_SCREEN_ON can't be declared in the manifest, so it is registered while bound.
     */
    private fun registerScreenOnReceiver() {
        // A second connect without a disconnect must not leave two receivers behind.
        unregisterScreenOnReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val app = application as BeaconApp
                app.appScope.launch {
                    try {
                        app.pauseCoordinator.catchUp()
                    } catch (_: IOException) {
                        // The settings can't be read or saved: skip this refresh; the next one tries again.
                    }
                }
            }
        }
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_NOT_EXPORTED)
        screenOnReceiver = receiver
    }

    /** Unregisters the screen-on receiver; does nothing when there is none. */
    private fun unregisterScreenOnReceiver() {
        screenOnReceiver?.let { unregisterReceiver(it) }
        screenOnReceiver = null
    }
}
