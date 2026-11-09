package dev.beacon.widget

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.BeaconApp
import dev.beacon.idleMainUntil
import dev.beacon.settings.Pause
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/*
 * Why Robolectric: the alarm is only as good as what AlarmManager is asked
 * for (inexact, a 10-minute window, aimed at Beacon's own receiver), and the
 * receiver only works through a real broadcast with goAsync(). The receiver
 * test goes through the real app, its coordinator and its settings file.
 */

/** Tests for [AndroidResumeAlarm] and [ResumeReceiver]. */
@RunWith(AndroidJUnit4::class)
class ResumeAlarmTest {
    /** The real app, whose manifest declares the receiver. */
    private val app = ApplicationProvider.getApplicationContext<BeaconApp>()

    /** Robolectric's view of the alarms that were set. */
    private val alarms: ShadowAlarmManager get() = shadowOf(app.getSystemService(AlarmManager::class.java))

    /** A deadline sets one inexact wake-up with a 10-minute window, which reaches the resume receiver (ruling F2). */
    @Test fun schedulesAnInexactWakeup() {
        AndroidResumeAlarm(app).schedule(5_000)
        val alarm = alarms.scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType())
        assertEquals(5_000L, alarm.getTriggerAtMs())
        // Inexact with a set window: 10 minutes at most, not Android's 75 % of the delay, and no exact-alarm permission.
        assertEquals(10 * 60_000L, alarm.getWindowLengthMs())
        // setWindow alarms wait for Doze's maintenance windows; the screen-on catch-up covers the time the widget is seen.
        assertFalse("not allowed while idle", alarm.isAllowWhileIdle())
        // Firing it sends its pending intent: an explicit broadcast to the resume receiver.
        alarms.fireAlarm(alarm)
        assertEquals(ComponentName(app, ResumeReceiver::class.java), shadowOf(app).broadcastIntents.last().component)
    }

    /** No deadline cancels the alarm that was set. */
    @Test fun nullCancels() {
        val alarm = AndroidResumeAlarm(app)
        alarm.schedule(5_000)
        alarm.schedule(null)
        assertTrue("no alarm left", alarms.scheduledAlarms.isEmpty())
    }

    /** The receiver cleans up an expired timed pause in the app's settings, then finishes the broadcast. */
    @Test fun receiverCatchesUp() {
        val repo = app.settingsRepository
        runBlocking { repo.setPause(Pause.Until(1_000)) }
        // The instance Robolectric registered from the manifest, which the broadcast reaches.
        val receiver = shadowOf(app).registeredReceivers.map { it.broadcastReceiver }.filterIsInstance<ResumeReceiver>().single()
        app.sendBroadcast(Intent(app, ResumeReceiver::class.java))
        // Delivers the broadcast; the receiver goes async and starts catching up on the app's main-thread scope.
        shadowOf(Looper.getMainLooper()).idle()
        val finished = shadowOf(shadowOf(receiver).originalPendingResult).future
        idleMainUntil { finished.isDone }
        assertTrue("receiver finished its broadcast", finished.isDone)
        assertEquals(Pause.None, runBlocking { repo.settings.first() }.pause)
    }
}
