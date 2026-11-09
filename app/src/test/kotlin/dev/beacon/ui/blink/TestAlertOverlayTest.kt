package dev.beacon.ui.blink

import android.os.Looper
import dev.beacon.widget.NoWidgetRefreshRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.BeaconApp
import dev.beacon.alert.AlertKind
import dev.beacon.alert.AlertState
import dev.beacon.settings.Pause
import dev.beacon.ui.FontScaleRule
import dev.beacon.ui.MainActivity
import dev.beacon.ui.assertNoTextOverflow
import dev.beacon.ui.enableAccessibilityService
import dev.beacon.ui.recordAccessibilityEvents
import dev.beacon.ui.theme.BeaconTheme
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import dev.beacon.alert.AlertRequest
import dev.beacon.alert.BlinkClock
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import dev.beacon.ui.hasRole
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Why the real activity and two clocks: the overlay's job is wiring (the
 * button, the controller, Back, the hidden shell) and timing that survives
 * rotation, so these tests drive MainActivity with its real controller.
 * BlinkScreen asks for a frame on every frame, so the test clock never goes
 * idle: auto-advance is off and each test moves time itself. The controller
 * and BlinkScreen read SystemClock (moved by Robolectric's looper), while
 * Compose draws on the test's frame clock, so advance() moves both by the
 * same amount. Robolectric has no camera light, so the details say "Not on
 * this phone".
 */

/** The overlay's title, shown only while a test runs. */
private const val TITLE = "This is how your alerts will look"

/**
 * The app's one alert controller.
 *
 * @return the controller, shared by every activity in the test, recreated ones included.
 */
private fun controller() = ApplicationProvider.getApplicationContext<BeaconApp>().alertController

/**
 * Moves both clocks forward: Robolectric's SystemClock (running the
 * controller's automatic end when it falls due), then Compose's frame clock.
 *
 * @param ms how far to move, in milliseconds.
 */
private fun ComposeTestRule.advance(ms: Long) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    mainClock.advanceTimeBy(ms)
}

/** Taps Test alert and lets one frame draw the overlay. */
private fun ComposeTestRule.openTest() {
    onNodeWithTag("test_button").performClick()
    advance(16)
}

/**
 * Waits until something matching [matcher] is on screen.
 *
 * With auto-advance off, nothing recomposes unless the frame clock moves,
 * so each check first moves it one frame; a plain waitUntil would time out
 * with the loaded settings never reaching the screen.
 *
 * @param matcher what to wait for.
 * @throws androidx.compose.ui.test.ComposeTimeoutException if it doesn't appear within 5 seconds.
 */
private fun ComposeTestRule.waitShowing(matcher: SemanticsMatcher) {
    waitUntil(timeoutMillis = 5_000) {
        mainClock.advanceTimeByFrame()
        onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Scrolls a scrolling container all the way to its end.
 *
 * Not performScrollTo: in Compose 1.11 it repeats until the node is in view,
 * but each semantic scroll is animated, and with auto-advance off the
 * animation never moves, so it would loop for ever. One long scroll, then
 * a second of frames for the animation to land, ends in the same place.
 *
 * @param matcher the scrolling container.
 */
private fun ComposeTestRule.scrollToEnd(matcher: SemanticsMatcher) {
    onNode(matcher).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100_000f) }
    advance(1_000)
}

/** Matches a node that TalkBack announces politely when it changes. */
private val politeLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

/** Matches the overlay's root, which TalkBack announces as the "Test alert" pane. */
private val overlayPane = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Test alert")

/** Robolectric tests for the Test alert overlay at the normal font size on a typical 360 × 800 dp phone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class TestAlertOverlayTest {
    /** Launches MainActivity for each test. */
    val rule = createAndroidComposeRule<MainActivity>()

    /** Skips the widget refresh that opening Beacon starts, before the activity launches. */
    @get:Rule val chain: RuleChain = RuleChain.outerRule(NoWidgetRefreshRule()).around(rule)

    /** Turns off auto-advance (the blink never idles), then waits for the settings to load. */
    @Before fun setUp() {
        rule.mainClock.autoAdvance = false
        rule.waitShowing(hasRole(Role.Tab))
    }

    /** Ends any test still running, so the process-wide controller starts the next test idle. */
    @After fun stopAlert() = controller().stop()

    /** The overlay shows the title, a full 3-second countdown and the four details; Home underneath is hidden. */
    @Test fun overlayShowsDetails() {
        rule.openTest()
        rule.onNodeWithText(TITLE).assertExists()
        rule.onNodeWithText("3").assertExists()
        rule.onNodeWithText("seconds left").assertExists()
        // Home's summary also says "Yellow"; its semantics are cleared while the overlay shows.
        rule.onAllNodesWithText("Yellow").assertCountEquals(1)
        rule.onNodeWithText("Medium, 60 a minute").assertExists()
        rule.onNodeWithText("Not on this phone").assertExists()
        rule.onNodeWithText("1 minute").assertExists()
    }

    /** While the overlay shows, TalkBack reaches only it: a named pane, two headings, live regions; no tabs or button. */
    @Test fun talkBackOnlyReachesTheOverlay() {
        rule.openTest()
        rule.onNode(overlayPane).assertExists()
        rule.onNode(hasText("Test alert") and isHeading()).assertExists()
        rule.onNode(hasText(TITLE) and isHeading() and politeLiveRegion).assertExists()
        rule.onNode(hasText("3") and politeLiveRegion).assertExists()
        rule.onAllNodes(hasRole(Role.Tab)).assertCountEquals(0)
        rule.onNodeWithTag("test_button").assertDoesNotExist()
        // Once the test is stopped, the shell is reachable again.
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).performClick()
        rule.advance(16)
        rule.onAllNodes(hasRole(Role.Tab)).assertCountEquals(2)
    }

    /** Stop closes the overlay; Back closes it too, before the tab's own Back handler, and doesn't leave the app. */
    @Test fun stopAndBackClose() {
        rule.openTest()
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).performClick()
        rule.advance(16)
        rule.onNodeWithText(TITLE).assertDoesNotExist()
        assertEquals(AlertState.Idle, controller().state.value)

        // On Alert settings, Back would normally go Home; with the overlay open it must only stop the test.
        rule.onNode(hasRole(Role.Tab) and hasText("Alert settings")).performClick()
        rule.advance(16)
        rule.openTest()
        rule.onNodeWithText(TITLE).assertExists()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.advance(16)
        rule.onNodeWithText(TITLE).assertDoesNotExist()
        assertEquals(AlertState.Idle, controller().state.value)
        rule.onNode(hasRole(Role.Tab) and hasText("Alert settings")).assertIsSelected()
        assertFalse(rule.activity.isFinishing)
    }

    /** The overlay counts down to "1 second left" and closes itself once the 3 seconds are up. */
    @Test fun endsAfterThreeSeconds() {
        rule.openTest()
        rule.advance(2_900)
        rule.onNodeWithText(TITLE).assertExists()
        rule.onNodeWithText("1").assertExists()
        rule.onNodeWithText("second left").assertExists()
        rule.advance(100)
        rule.onNodeWithText(TITLE).assertDoesNotExist()
        assertEquals(AlertState.Idle, controller().state.value)
    }

    /** Two taps before the next frame, as a quick double tap would land, start one test and draw one overlay. */
    @Test fun oneRunWhenTappedTwice() {
        rule.onNodeWithTag("test_button").performClick()
        rule.onNodeWithTag("test_button").performClick()
        rule.advance(16)
        rule.onAllNodesWithText(TITLE).assertCountEquals(1)
    }

    /** Recreating the activity (as rotation does) keeps the controller's clock: the countdown goes on at 2, not 3. */
    @Test fun recreateContinuesCountdown() {
        rule.openTest()
        rule.advance(1_100)
        rule.activityRule.scenario.recreate()
        rule.advance(16)
        rule.onNodeWithText("2").assertExists()
        rule.onNodeWithText("3").assertDoesNotExist()
    }

    /** A test runs even while alerts are paused: the pause only holds back real alerts. */
    @Test fun runsWhilePaused() {
        val app = ApplicationProvider.getApplicationContext<BeaconApp>()
        runBlocking { app.settingsRepository.setPause(Pause.Indefinite) }
        // Wait until the screen shows the pause, so the test really starts from paused settings.
        rule.waitShowing(hasText("PAUSED"))
        rule.openTest()
        rule.onNodeWithText(TITLE).assertExists()
        val running = controller().state.value
        assertTrue("controller runs a test: $running", running is AlertState.Running && running.request.kind == AlertKind.TEST)
    }

    /** A tap on the overlay never reaches the screen underneath: the Alert settings tab below stays unselected. */
    @Test fun tapsDontReachTheScreenUnderneath() {
        rule.openTest()
        // In the overlay's 20 dp side margin, over the Alert settings tab at the bottom end of the shell.
        rule.onNode(overlayPane).performTouchInput { click(Offset(width - 10.dp.toPx(), height - 40.dp.toPx())) }
        rule.advance(16)
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).performClick()
        rule.advance(16)
        rule.onNode(hasRole(Role.Tab) and hasText("Home")).assertIsSelected()
    }

    /**
     * The countdown is one polite live region whose text is a new node each
     * second, so TalkBack gets a subtree change from it at 2 and again at 1.
     * Each step starts with no events, so the second one can't ride on the first.
     */
    @Test fun countdownReachesTalkBackEverySecond() {
        enableAccessibilityService()
        rule.openTest()
        val events = rule.recordAccessibilityEvents()
        val region = rule.onNode(hasText("3") and politeLiveRegion).fetchSemanticsNode()
        listOf("2", "1").forEach { second ->
            events.clear()
            rule.advance(1_000)
            // Compose batches its accessibility events and sends them up to 100 ms after a
            // change, so time moves on, still inside this second, before the events are checked.
            rule.advance(300)
            rule.onNode(hasText(second) and politeLiveRegion).assertExists()
            val changes = events.filter {
                it.type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                    it.sourceId == region.id &&
                    it.changes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0
            }
            // Compose sends this change in two batches 100 ms apart, so "at least one", as in D4.
            assertTrue("TalkBack gets a subtree change from the countdown at $second; sent: $events", changes.isNotEmpty())
        }
    }
}

/** Robolectric test for the camera row of the Test alert overlay when the heat check has stopped the light. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class TestAlertOverlayCameraTest {
    /** Hosts the overlay alone in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** A phone with a light whose light has stopped: the row says why, not "Flashing too". */
    @Test fun cameraLineSaysHotWhenLightStopped() {
        // BlinkScreen asks for a frame on every frame, so the clock never idles.
        rule.mainClock.autoAdvance = false
        val running = AlertState.Running(
            request = AlertRequest(AlertKind.TEST, FlashColour.YELLOW, BlinkSpeed.MEDIUM, 3_000L, null),
            clock = BlinkClock(SystemClock.uptimeMillis(), BlinkSpeed.MEDIUM, 3_000L),
            lightFlashing = false,
        )
        rule.setContent {
            BeaconTheme {
                TestAlertOverlay(running, "Yellow", "Medium, 60 a minute", "1 minute", hasLight = true, onStop = {})
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Off while the phone is hot").assertExists()
        rule.onNodeWithText("Flashing too").assertDoesNotExist()
    }
}

/** Robolectric test for the Test alert overlay at the largest system font size on a small 360 × 640 dp phone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp")
class TestAlertOverlayLargestFontTest {
    /** Launches MainActivity for each test. */
    val rule = createAndroidComposeRule<MainActivity>()

    /** Sets the 200% font scale before the activity launches, and resets it after. */
    @get:Rule val chain: RuleChain = RuleChain.outerRule(NoWidgetRefreshRule()).around(FontScaleRule(2.0f)).around(rule)

    /** Turns off auto-advance (the blink never idles), then waits for the settings to load. */
    @Before fun setUp() {
        rule.mainClock.autoAdvance = false
        rule.waitShowing(hasRole(Role.Tab))
    }

    /** Ends any test still running, so the process-wide controller starts the next test idle. */
    @After fun stopAlert() = controller().stop()

    /** At the largest font no overlay text is cut off, and Stop can be scrolled into view. */
    @Test fun noOverflowAtLargestFont() {
        // At this size the Test alert button is the last item of the scrolling content, below the fold.
        rule.scrollToEnd(hasTestTag("scroll_content"))
        rule.openTest()
        // Without the overlay, the overflow check would only cover the shell.
        rule.onNodeWithText(TITLE).assertExists()
        rule.assertNoTextOverflow()
        // The overlay is taller than the window here; its Stop button sits at the end.
        rule.scrollToEnd(hasScrollAction() and hasAnyAncestor(overlayPane))
        // Opened 16 ms in and scrolled for 1 s, so the test is still running.
        rule.onNodeWithText(TITLE).assertExists()
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).assertIsDisplayed()
    }
}
