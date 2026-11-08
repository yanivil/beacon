package dev.beacon.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder

/*
 * Why a foreground service: it is Android's approved way to keep working with the screen off, at
 * the price of a visible notification. The open question for Step 1 is whether it can be started at
 * the moment a notification arrives, because Android restricts starting one from the background.
 * Starting it here reports that refusal instead of hiding it.
 *
 * The specialUse type is chosen because none of the standard types describes visual alerts. Google
 * Play reviews this type, so it is a policy question to report, not just a technical one.
 */

/** Hosts an alert in a foreground service: the 60-minute run and the service alert path. */
class BlinkService : Service() {

    /**
     * Not a bound service.
     *
     * @param intent Unused.
     * @return Always null.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Enters the foreground and starts the alert.
     *
     * @param intent Carries the preset, duration and reason.
     * @param flags Unused.
     * @param startId Unused.
     * @return [START_NOT_STICKY], because a killed test must show up in the log as killed rather
     *   than quietly restarting and looking like a pass.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_spike)
            .setContentTitle(getString(R.string.notification_running))
            .setOngoing(true)
            .build()
        try {
            startForeground(
                Notifications.ID_STATUS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (e: IllegalStateException) {
            SpikeLog.write(this, "service.foregroundRefused", "error=${e.javaClass.simpleName}")
            stopSelf()
            return START_NOT_STICKY
        }
        SpikeLog.write(this, "service.foreground", DeviceState.snapshot(this))
        AlertRunner.start(
            context = this,
            preset = BlinkPreset.fromName(intent?.getStringExtra(EXTRA_PRESET)),
            seconds = intent?.getIntExtra(EXTRA_SECONDS, 10) ?: 10,
            torch = true,
            reason = intent?.getStringExtra(EXTRA_REASON) ?: "service",
            finished = {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            },
        )
        return START_NOT_STICKY
    }

    /** Stops the alert if the system ends the service early, and records that it happened. */
    override fun onDestroy() {
        AlertRunner.stop(this, "serviceDestroyed")
        SpikeLog.write(this, "service.destroyed", DeviceState.snapshot(this))
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PRESET = "preset"
        private const val EXTRA_SECONDS = "seconds"
        private const val EXTRA_REASON = "reason"

        /**
         * Tries to start the service.
         *
         * @param context Any context.
         * @param preset The blink speed.
         * @param seconds How long to run.
         * @param reason What caused this alert, for the log.
         * @return False when Android refused to start it. That refusal is a Step 1 result.
         */
        fun start(context: Context, preset: BlinkPreset, seconds: Int, reason: String): Boolean {
            val intent = Intent(context, BlinkService::class.java)
                .putExtra(EXTRA_PRESET, preset.name)
                .putExtra(EXTRA_SECONDS, seconds)
                .putExtra(EXTRA_REASON, reason)
            return try {
                context.startForegroundService(intent)
                true
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException is an IllegalStateException.
                SpikeLog.write(context, "service.startRefused", "error=${e.javaClass.simpleName}")
                false
            }
        }
    }
}

/** Notification channels and ids shared by the test app. */
object Notifications {
    const val CHANNEL_STATUS = "status"
    const val CHANNEL_ALERT = "alert"
    const val ID_STATUS = 1
    const val ID_ALERT = 2
    const val ID_WATCH = 3

    /**
     * Creates the channels. Safe to call repeatedly.
     *
     * @param context Any context.
     */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.channel_status),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        // A full-screen alert is only honoured on a high-importance channel.
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT,
                context.getString(R.string.channel_alert),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }
}
