package dev.beacon.widget

import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import android.provider.Settings
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * Why Robolectric: the receiver only works through a real broadcast with
 * goAsync(), through the real app, its coordinator and its settings file,
 * as ResumeAlarmTest's receiver test does. The widget refresh is counted
 * through WidgetRefresher's seam instead of running Glance. The broadcasts
 * are sent to the receiver by name, so no other app component gets them;
 * the last test checks, through the package manager, that the manifest lets
 * the three system broadcasts reach it (follow-up R1).
 */

/** Tests for [TimeChangeReceiver]. */
@RunWith(AndroidJUnit4::class)
class TimeChangeReceiverTest {
    /** Puts the real refresh back after each test; the count below replaces it meanwhile. */
    @get:Rule val noWidgets = NoWidgetRefreshRule()

    /** The real app, whose manifest declares the receiver. */
    private val app = ApplicationProvider.getApplicationContext<BeaconApp>()

    /** How many times a refresh was asked for. */
    private var refreshes = 0

    /** The three system broadcasts the receiver handles. */
    private val actions = listOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED)

    /** Counts refreshes instead of running Glance. */
    @Before fun countRefreshes() {
        WidgetRefresher.update = { refreshes++ }
    }

    /**
     * Saves a pause in the app's settings, as the widget or Home would have.
     *
     * @param pause the pause to save.
     */
    private fun save(pause: Pause) = runBlocking { app.settingsRepository.setPause(pause) }

    /** A timed pause that ends half an hour from the real clock, which the app's coordinator reads. */
    private fun runningTimedPause() = Pause.Until(System.currentTimeMillis() + 30 * 60_000L)

    /**
     * Sends a broadcast to the receiver as the system does, and waits for it to finish the broadcast.
     *
     * @param action the broadcast's action.
     * @return true if the broadcast was finished within 5 seconds.
     */
    private fun deliver(action: String): Boolean {
        // The instance Robolectric registered from the manifest, which the broadcast reaches.
        val receiver = shadowOf(app).registeredReceivers.map { it.broadcastReceiver }.filterIsInstance<TimeChangeReceiver>().single()
        app.sendBroadcast(Intent(action).setComponent(ComponentName(app, TimeChangeReceiver::class.java)))
        // Delivers the broadcast; the receiver goes async and catches up on the app's main-thread scope.
        shadowOf(Looper.getMainLooper()).idle()
        val finished = shadowOf(shadowOf(receiver).originalPendingResult).future
        idleMainUntil { finished.isDone }
        return finished.isDone
    }

    /**
     * Checks that [action], with a timed pause running, redraws the widget once and finishes the broadcast.
     *
     * @param action the broadcast's action.
     */
    private fun assertOneRedrawDuringATimedPause(action: String) {
        val pause = runningTimedPause()
        save(pause)
        assertTrue("the receiver finished its broadcast", deliver(action))
        assertEquals(1, refreshes)
        assertEquals(pause, runBlocking { app.settingsRepository.settings.first() }.pause)
    }

    /** A 12/24-hour change or a clock change during a timed pause redraws the widget once. */
    @Test fun timeSetRedrawsARunningTimedPause() = assertOneRedrawDuringATimedPause(Intent.ACTION_TIME_CHANGED)

    /** A time-zone change during a timed pause redraws the widget once. */
    @Test fun timeZoneChangeRedrawsARunningTimedPause() = assertOneRedrawDuringATimedPause(Intent.ACTION_TIMEZONE_CHANGED)

    /** A language change during a timed pause redraws the widget once. */
    @Test fun localeChangeRedrawsARunningTimedPause() = assertOneRedrawDuringATimedPause(Intent.ACTION_LOCALE_CHANGED)

    /** With alerts ON the widget shows no time, so a time change redraws nothing (ruling F3's reason). */
    @Test fun timeSetWithAlertsOnDoesNotRedraw() {
        save(Pause.None)
        assertTrue("the receiver finished its broadcast", deliver(Intent.ACTION_TIME_CHANGED))
        assertEquals(0, refreshes)
    }

    /** With alerts ON a language change still redraws once: the widget's words change. */
    @Test fun localeChangeWithAlertsOnRedrawsOnce() {
        save(Pause.None)
        assertTrue("the receiver finished its broadcast", deliver(Intent.ACTION_LOCALE_CHANGED))
        assertEquals(1, refreshes)
    }

    /** The receiver stores the new key before it asks for the redraw, so a running session draws the new format. */
    @Test fun timeSetUpdatesTheKeyBeforeTheRedraw() {
        var keyAtRefresh: WidgetConfigKey? = null
        WidgetRefresher.update = { refreshes++; keyAtRefresh = app.widgetConfig.value }
        // Start from the 12-hour setting, whatever the process started with.
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        app.updateWidgetConfig()
        save(runningTimedPause())
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "24")
        assertTrue("the receiver finished its broadcast", deliver(Intent.ACTION_TIME_CHANGED))
        assertEquals(1, refreshes)
        assertEquals(true, keyAtRefresh?.is24Hour)
    }

    /** Any other action is ignored: an ended pause is left stored as it was, and nothing is redrawn. */
    @Test fun anUnrelatedActionIsIgnored() {
        save(Pause.Until(1_000))
        // Without goAsync, Robolectric finishes the broadcast itself once onReceive returns.
        assertTrue("the broadcast reached the receiver", deliver("dev.beacon.test.UNRELATED"))
        assertEquals(Pause.Until(1_000), runBlocking { app.settingsRepository.settings.first() }.pause)
        assertEquals(0, refreshes)
    }

    /** The manifest lets each of the three system broadcasts reach the receiver, which isn't exported. */
    @Test fun theManifestRegistersTheReceiver() {
        for (action in actions) {
            val found = app.packageManager.queryBroadcastReceivers(Intent(action).setPackage(app.packageName), 0)
                .map { it.activityInfo }
                .filter { it.name == TimeChangeReceiver::class.java.name }
            assertEquals("$action reaches TimeChangeReceiver", 1, found.size)
            assertFalse("TimeChangeReceiver isn't exported", found.single().exported)
        }
    }
}
