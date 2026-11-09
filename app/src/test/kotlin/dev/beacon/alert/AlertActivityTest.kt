package dev.beacon.alert

import android.content.Intent
import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.beacon.BeaconApp
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import org.junit.After
import org.junit.Assert.assertEquals
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
 */

/** Drives [AlertActivity] against the app's real controller. */
@RunWith(AndroidJUnit4::class)
class AlertActivityTest {
    @get:Rule val rule = createEmptyComposeRule()

    private fun controller() = ApplicationProvider.getApplicationContext<BeaconApp>().alertController

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
        launch().onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertEquals(AlertState.Idle, controller().state.value)
    }

    /** Home (the activity being left by the user) ends the alert. */
    @Test fun homeEnds() {
        launch().onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnUserLeaving(it) }
        assertEquals(AlertState.Idle, controller().state.value)
    }

    /** The power button (screen off) ends the alert. */
    @Test fun screenOffEnds() {
        launch().onActivity { it.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF)) }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(AlertState.Idle, controller().state.value)
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
}
