package dev.beacon.ui

import androidx.test.core.app.ApplicationProvider
import org.junit.rules.RuleChain
import dev.beacon.widget.NoWidgetRefreshRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.BeaconApp
import dev.beacon.alert.AlertKind
import dev.beacon.alert.AlertRequest
import dev.beacon.alert.AlertState
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.After
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationManager
import dev.beacon.alert.BeaconListener
import dev.beacon.alert.ListenerConnection
import org.robolectric.Shadows.shadowOf

/*
 * Why Robolectric's activity controller and the app's own controller: these
 * rules live in MainActivity's lifecycle callbacks (start ends a real alert,
 * stop ends a test unless the activity is only being recreated), so the tests
 * move the real activity between lifecycle states and read the one controller
 * the activity uses. ActivityScenario would do the same, but its idle wait
 * jumps the clock to the alert's own end (or, for an alert with no end, loops
 * through every renewal), so the alert would be over before the check.
 * "Display over other apps" is off under Robolectric, so a real alert started
 * here opens no alert window of its own.
 */

/** Robolectric tests for how Beacon's main screen starting and stopping ends alerts, on a plain activity. */
@RunWith(AndroidJUnit4::class)
class MainActivityAlertTest {
    /** Skips the widget refresh that starting Beacon's main screen begins. */
    @get:Rule val noWidgets = NoWidgetRefreshRule()

    private val controller = ApplicationProvider.getApplicationContext<BeaconApp>().alertController

    /** Ends whatever a test left running, so the next test starts from Idle. */
    @After fun stopAlert() {
        controller.stop()
    }

    /**
     * Builds an alert request.
     *
     * @param kind test or real.
     * @return a request with a fixed colour and speed and no end ([source] is SMS for a real one).
     */
    private fun request(kind: AlertKind) = AlertRequest(
        kind, FlashColour.YELLOW, BlinkSpeed.MEDIUM, 60_000L,
        source = if (kind == AlertKind.REAL) MessageSource.SMS else null,
    )

    /** Opening Beacon ends a real alert, which is how a light-only alert is stopped. */
    @Test fun openingBeaconStopsRealAlert() {
        controller.start(request(AlertKind.REAL))
        assertTrue(controller.state.value is AlertState.Running)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertEquals(AlertState.Idle, controller.state.value)
        activity.pause().stop().destroy()
    }

    /** Rotating while Beacon is in front keeps a light-only real alert running on the same clock. */
    @Test fun rotationKeepsALightOnlyRealAlert() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        // Started with Beacon in front, as when a message arrives while the app is open and the window can't show.
        controller.start(request(AlertKind.REAL))
        val startedAt = (controller.state.value as AlertState.Running).clock.startUptimeMillis
        activity.recreate()
        val after = controller.state.value
        assertTrue("still running: $after", after is AlertState.Running)
        assertEquals(startedAt, (after as AlertState.Running).clock.startUptimeMillis)
        activity.pause().stop().destroy()
    }

    /** Resuming Beacon re-binds a listener the system dropped, when access is granted. */
    @Test fun resumeReconnectsADroppedListener() {
        val context = ApplicationProvider.getApplicationContext<BeaconApp>()
        val component = ComponentName(context, BeaconListener::class.java)
        shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .setNotificationListenerAccessGranted(component, true)
        ListenerConnection.connected = false
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, context.packageManager.getComponentEnabledSetting(component))
        activity.pause().stop().destroy()
    }

    /** A real alert that replaced a test is not ended by the main screen stopping. */
    @Test fun realAlertSurvivesMainScreenStopping() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.start(request(AlertKind.TEST))
        controller.start(request(AlertKind.REAL))
        activity.pause().stop()
        val state = controller.state.value
        assertTrue("real alert still running: $state", state is AlertState.Running && state.request.kind == AlertKind.REAL)
        activity.destroy()
    }
}

/*
 * Why the Compose rule for these two: while a test shows, the overlay draws a
 * frame on every frame, so a plain ActivityScenario's idle wait before each
 * lifecycle move races through those frames to the alert's end. The rule's
 * frame clock is moved by hand instead, as in the overlay tests.
 */

/** Robolectric tests for how Beacon's main screen stopping or being recreated treats a test that is showing. */
@RunWith(AndroidJUnit4::class)
class MainActivityRunningTestTest {
    /** Launches MainActivity for each test. */
    val rule = createAndroidComposeRule<MainActivity>()

    /** Skips the widget refresh that opening Beacon starts, before the activity launches. */
    @get:Rule val chain: RuleChain = RuleChain.outerRule(NoWidgetRefreshRule()).around(rule)

    private val controller = ApplicationProvider.getApplicationContext<BeaconApp>().alertController

    /** Ends whatever a test left running, so the next test starts from Idle. */
    @After fun stopAlert() {
        controller.stop()
    }

    /**
     * Starts a test and draws one frame so its overlay is on screen.
     *
     * @return the test's start time on the uptime clock.
     */
    private fun startShownTest(): Long {
        rule.mainClock.autoAdvance = false
        controller.start(AlertRequest(AlertKind.TEST, FlashColour.YELLOW, BlinkSpeed.MEDIUM, 60_000L, source = null))
        rule.mainClock.advanceTimeByFrame()
        return (controller.state.value as AlertState.Running).clock.startUptimeMillis
    }

    /** Leaving Beacon's main screen (it stops) ends a running test. */
    @Test fun leavingStopsTest() {
        startShownTest()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals(AlertState.Idle, controller.state.value)
    }

    /** Rotation recreates the activity but keeps the test running on the same clock. */
    @Test fun rotationKeepsTest() {
        val startedAt = startShownTest()
        rule.activityRule.scenario.recreate()
        val after = controller.state.value
        assertTrue("still running: $after", after is AlertState.Running)
        assertEquals(startedAt, (after as AlertState.Running).clock.startUptimeMillis)
    }
}
