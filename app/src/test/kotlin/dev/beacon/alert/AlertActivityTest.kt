package dev.beacon.alert

import android.content.Intent
import android.os.Looper
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.beacon.BeaconApp
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/*
 * Why these tests: the alert window is the only way a deaf user sees a real
 * message, so every way it can end (Stop, Back, Home, power button, timer) and
 * the case where nothing is running must close it and leave the light off.
 * Just as much, nothing but those must end it: a window opened while the
 * screen is off was once ended at once by Android's sleep pause (D2).
 */

/** Drives [AlertActivity] against the app's real controller. */
@RunWith(AndroidJUnit4::class)
class AlertActivityTest {
    @get:Rule val rule = createEmptyComposeRule()

    private fun controller() = ApplicationProvider.getApplicationContext<BeaconApp>().alertController

    /**
     * The phone's power service, whose screen-on state the tests set.
     *
     * @return the app's PowerManager.
     */
    private fun powerManager(): PowerManager =
        ApplicationProvider.getApplicationContext<BeaconApp>().getSystemService(PowerManager::class.java)

    /** Starts a real WhatsApp alert, as the watcher would, before each launch. */
    @Before fun startAlert() {
        // The alert screen animates forever, so the test drives Compose's clock by hand.
        rule.mainClock.autoAdvance = false
        controller().start(
            AlertRequest(AlertKind.REAL, FlashColour.YELLOW, BlinkSpeed.MEDIUM, 60_000L, MessageSource.WHATSAPP),
        )
    }

    /** Ends any alert still running, so the process-wide controller starts the next test idle. */
    @After fun stopAlert() = controller().stop()

    /** Lets Compose see the controller's latest state and run the effects it caused. */
    private fun settle() = repeat(3) { rule.mainClock.advanceTimeByFrame() }

    private fun launch() = ActivityScenario.launch<AlertActivity>(
        AlertActivity.intent(ApplicationProvider.getApplicationContext()),
    )

    /** The screen shows the title and app name, the window title names the app, and the screen stays on. */
    @Test fun showsTheAppNameAndTitle() {
        launch().onActivity { activity ->
            assertEquals("New message: WhatsApp messages", activity.title)
            assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        }
        settle()
        rule.onNodeWithText("New message").assertExists()
        rule.onNodeWithText("WhatsApp messages").assertExists()
    }

    /** Stop ends the alert and closes the window. */
    @Test fun stopEndsAndCloses() {
        val scenario = launch()
        settle()
        rule.onNodeWithText("Stop").performClick()
        assertEquals(AlertState.Idle, controller().state.value)
        settle()
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    /** Back ends the alert. */
    @Test fun backEnds() {
        val scenario = launch()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertEquals(AlertState.Idle, controller().state.value)
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    /** Home (the activity being left by the user, with the screen on) ends the alert. */
    @Test fun homeEnds() {
        shadowOf(powerManager()).turnScreenOn(true)
        val scenario = launch()
        scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnUserLeaving(it) }
        assertEquals(AlertState.Idle, controller().state.value)
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    /**
     * The window's start is marked as not being a user action, so Android doesn't treat the
     * alert's own arrival as the user leaving (D2: the sleep pause came with `userLeaving=true`).
     */
    @Test fun launchIsNotAUserAction() {
        val flags = AlertActivity.intent(ApplicationProvider.getApplicationContext()).flags
        assertTrue(flags and Intent.FLAG_ACTIVITY_NO_USER_ACTION != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    /**
     * A user-leaving call while the screen is off (the system's sleep pause, D2) leaves the alert
     * running. As on the emulator, the screen is already off when the window opens, so its own
     * screen-off receiver (the power button) isn't involved.
     */
    @Test fun userLeavingWhileScreenOffKeepsTheAlert() {
        shadowOf(powerManager()).turnScreenOn(false)
        // Delivers the screen-off broadcast before the window exists, as with a phone already asleep.
        shadowOf(Looper.getMainLooper()).idle()
        val scenario = launch()
        scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnUserLeaving(it) }
        assertTrue(controller().state.value is AlertState.Running)
        scenario.onActivity { assertFalse(it.isFinishing) }
    }

    /** The power button (screen off) ends the alert. */
    @Test fun screenOffEnds() {
        val scenario = launch()
        scenario.onActivity { it.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF)) }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(AlertState.Idle, controller().state.value)
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    /** With nothing running the window closes itself straight away. */
    @Test fun finishesWhenNothingRuns() {
        controller().stop()
        val controllerActivity = Robolectric.buildActivity(AlertActivity::class.java).create()
        assertTrue(controllerActivity.get().isFinishing)
    }

    /** When the timer ends the alert, the window closes. */
    @Test fun finishesWhenAlertEnds() {
        val scenario = launch()
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(60))
        assertEquals(AlertState.Idle, controller().state.value)
        settle()
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    /** Rotation recreates the window but the alert carries on from the same clock. */
    @Test fun rotationKeepsAlert() {
        val scenario = launch()
        val before = (controller().state.value as AlertState.Running).clock.startUptimeMillis
        scenario.recreate()
        val after = controller().state.value as AlertState.Running
        assertEquals(before, after.clock.startUptimeMillis)
    }

    /** The alert ending while the window is stopped still closes it (no Compose frame needed). */
    @Test fun closesWhileStopped() {
        val scenario = launch()
        scenario.moveToState(Lifecycle.State.CREATED)
        controller().stop()
        shadowOf(Looper.getMainLooper()).idle()
        scenario.onActivity { assertTrue(it.isFinishing) }
    }

    /** A lingering window must not stop a test alert on Home or screen-off. */
    @Test fun lingeringWindowLeavesATestAlone() {
        val scenario = launch()
        scenario.onActivity { activity ->
            // Simulate an instance that outlived its alert: stop, then a test starts.
            controller().stop()
            controller().start(
                AlertRequest(AlertKind.TEST, FlashColour.YELLOW, BlinkSpeed.MEDIUM, 60_000L, null),
            )
            activity.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
            shadowOf(Looper.getMainLooper()).idle()
            InstrumentationRegistry.getInstrumentation().callActivityOnUserLeaving(activity)
            assertTrue(controller().state.value is AlertState.Running)
        }
    }
}
