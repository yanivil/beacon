package dev.beacon.ui.blink

import android.os.SystemClock
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.alert.AlertKind
import dev.beacon.alert.AlertRequest
import dev.beacon.alert.AlertState
import dev.beacon.alert.BlinkClock
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import dev.beacon.ui.WithFontScale
import dev.beacon.ui.assertNoTextOverflow
import dev.beacon.ui.hasRole
import dev.beacon.ui.theme.BeaconTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Why Robolectric Compose with the frame clock off: BlinkScreen asks for a
 * frame on every frame, so the test clock never goes idle and each test would
 * wait for ever. With auto-advance off, the tests read the screen as drawn
 * and move time by hand where they need to.
 */

/** Robolectric Compose tests for the real alert screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class AlertScreenTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** How many times Stop was tapped in the current test. */
    private var stops = 0

    /** Turns off auto-advance, because the blink never idles. */
    @Before fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    /** A real WhatsApp alert: Yellow, Medium, one minute. */
    private fun running() = AlertState.Running(
        request = AlertRequest(AlertKind.REAL, FlashColour.YELLOW, BlinkSpeed.MEDIUM, 60_000L, MessageSource.WHATSAPP),
        clock = BlinkClock(SystemClock.uptimeMillis(), BlinkSpeed.MEDIUM, 60_000L),
        lightFlashing = true,
    )

    /**
     * Shows the alert screen.
     *
     * @param fontScale the font scale to show it at.
     */
    private fun show(fontScale: Float = 1f) {
        rule.setContent {
            BeaconTheme {
                WithFontScale(fontScale) {
                    AlertScreen(running(), appName = "WhatsApp messages", onStop = { stops++ })
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    /** The screen says "New message", the app's name and a Stop button, and nothing else (no message content). */
    @Test fun showsOnlyTitleNameAndStop() {
        show()
        rule.onNode(hasText("New message") and isHeading()).assertExists()
        val name = rule.onNode(hasText("WhatsApp messages") and isHeading())
        name.assertExists()
        assertEquals(LiveRegionMode.Polite, name.fetchSemanticsNode().config[SemanticsProperties.LiveRegion])
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).assertExists()
        // The unmerged tree reaches every text, so nothing hidden inside a merged control is missed.
        val texts = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }
        assertEquals(listOf("New message", "WhatsApp messages", "Stop"), texts)
    }

    /** TalkBack reaches Stop before the title card: it has a lower traversal index, in the same traversal group. */
    @Test fun stopIsFirstForTalkBack() {
        show()
        val stop = rule.onNode(hasText("Stop") and hasRole(Role.Button)).fetchSemanticsNode()
        // The card is the title's parent in the merged tree: it carries the card's shape.
        val titleCard = rule.onNode(hasText("WhatsApp messages") and isHeading()).fetchSemanticsNode().parent
        assertTrue("the title sits in a card node", titleCard != null)
        val stopIndex = stop.config.getOrNull(SemanticsProperties.TraversalIndex)
        val cardIndex = titleCard!!.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f
        assertTrue("Stop has a traversal index", stopIndex != null)
        assertTrue("Stop ($stopIndex) comes before the title card ($cardIndex)", stopIndex!! < cardIndex)
        val parent = stop.parent
        assertTrue("Stop and the title card share a parent", parent != null && parent.id == titleCard.parent?.id)
        assertEquals(true, parent!!.config.getOrNull(SemanticsProperties.IsTraversalGroup))
    }

    /** Tapping Stop calls onStop once. */
    @Test fun stopCallsOnStop() {
        show()
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).performClick()
        assertEquals(1, stops)
    }

    /** At the largest font on a phone held upright, no text is cut off and Stop is on screen. */
    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun noOverflowAtLargestFont() {
        show(fontScale = 2f)
        rule.assertNoTextOverflow()
        rule.onNodeWithText("Stop").assertIsDisplayed()
    }

    /** At the largest font with the phone on its side, no text is cut off (the column scrolls). */
    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun noOverflowAtLargestFontLandscape() {
        show(fontScale = 2f)
        rule.assertNoTextOverflow()
    }
}
