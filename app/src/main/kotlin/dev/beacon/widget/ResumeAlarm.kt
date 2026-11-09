package dev.beacon.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.beacon.BeaconApp
import java.io.IOException
import kotlinx.coroutines.launch

/*
 * Why a resume alarm: alerts check the clock themselves, so a timed pause
 * ends on time without it. The alarm only cleans up after the deadline:
 * it wakes Beacon near that time so the stored pause becomes ON and the
 * widget redraws. It is inexact, with a 10-minute window (setWindow), so no
 * exact-alarm permission is needed and it fires at most 10 minutes late
 * while the phone is awake (ruling F2, which replaces ruling 3's
 * setAndAllowWhileIdle and its window of 75 % of the delay). It is not set
 * again after a restart: there is no RECEIVE_BOOT_COMPLETED (ruling 4).
 */

/**
 * How late the resume alarm may fire, in milliseconds. 10 minutes is the smallest inexact window
 * Android allows without the exact-alarm permission; a shorter one is stretched to 10 minutes.
 * The alarm isn't allowed to fire in Doze: the widget is only seen with the screen on, and the
 * screen turning on already catches the pause up.
 */
internal const val RESUME_WINDOW_MILLIS = 10 * 60_000L

/** Wakes Beacon when a timed pause ends. */
interface ResumeAlarm {
    /**
     * Sets the one resume alarm, replacing any earlier one, or cancels it.
     *
     * @param atEpochMillis when the pause ends, in epoch milliseconds; null cancels the alarm.
     */
    fun schedule(atEpochMillis: Long?)
}

/**
 * The resume alarm on Android's [AlarmManager].
 *
 * @param context any context of this app; the application context is kept.
 */
class AndroidResumeAlarm(context: Context) : ResumeAlarm {
    /** The application context, so no screen is held on to. */
    private val appContext: Context = context.applicationContext

    /** The system alarm service. */
    private val alarmManager: AlarmManager = appContext.getSystemService(AlarmManager::class.java)

    /**
     * Sets an inexact wake-up between [atEpochMillis] and [RESUME_WINDOW_MILLIS] after it, or cancels the alarm.
     *
     * @param atEpochMillis when the pause ends, in epoch milliseconds; null cancels the alarm.
     */
    override fun schedule(atEpochMillis: Long?) {
        val pending = resumeIntent()
        if (atEpochMillis == null) {
            alarmManager.cancel(pending)
        } else {
            // The same pending intent each time, so a new alarm replaces the old one: there is only ever one.
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, atEpochMillis, RESUME_WINDOW_MILLIS, pending)
        }
    }

    /**
     * Builds the alarm's pending intent: an explicit broadcast to [ResumeReceiver].
     *
     * @return the pending intent; equal on every call, so setting and cancelling act on the same alarm.
     */
    private fun resumeIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(appContext, ResumeReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Receives the resume alarm and cleans up the pause that has ended. */
class ResumeReceiver : BroadcastReceiver() {
    /**
     * Catches the pause up off the receiver's call, then finishes the broadcast.
     *
     * @param context the app's context.
     * @param intent the alarm's intent; it carries nothing.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as BeaconApp
        // goAsync keeps the broadcast open while DataStore reads and writes off the main thread.
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.pauseCoordinator.catchUp()
            } catch (_: IOException) {
                // The settings can't be read or saved: leave them. Alerts still check the clock,
                // and the next catch-up (screen on, Beacon opened) tries again.
            } finally {
                pending.finish()
            }
        }
    }
}
