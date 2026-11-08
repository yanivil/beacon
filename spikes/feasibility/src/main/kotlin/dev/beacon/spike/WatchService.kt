package dev.beacon.spike

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder

/*
 * Why this exists: with the screen off on battery, the test phone held back notifications from the
 * listener for over a minute, until the phone woke. A foreground service is how an app tells Android
 * that the person knows it is working in the background, at the price of a permanent notification.
 * This tests whether that alone is enough to keep alerts on time, because it is the one fix the app
 * controls without sending an older person through Xiaomi's settings.
 *
 * It is exported only to callers holding DUMP, which in practice means adb, so a test can start it
 * while the phone is locked. The real app would start it from its own screen and never export it.
 */

/** Keeps the app in the foreground state and does nothing else. */
class WatchService : Service() {

    /**
     * Not a bound service.
     *
     * @param intent Unused.
     * @return Always null.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Enters the foreground and makes sure the listener is connected.
     *
     * @param intent Unused.
     * @param flags Unused.
     * @param startId Unused.
     * @return [START_STICKY], because a watcher that stays dead after being killed means silent
     *   missed alerts.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_spike)
            .setContentTitle(getString(R.string.notification_watching))
            .setOngoing(true)
            .build()
        startForeground(
            Notifications.ID_WATCH,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        SpikeLog.write(this, "watch.started", DeviceState.snapshot(this))
        AlertListener.reconnectIfDropped(this)
        return START_STICKY
    }

    /** Records the service ending, so a system kill shows up in the log. */
    override fun onDestroy() {
        SpikeLog.write(this, "watch.destroyed", DeviceState.snapshot(this))
        super.onDestroy()
    }
}
