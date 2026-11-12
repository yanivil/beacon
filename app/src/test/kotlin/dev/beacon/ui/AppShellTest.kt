package dev.beacon.ui

import androidx.compose.ui.semantics.Role
import dev.beacon.widget.NoWidgetRefreshRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.content.Intent
import androidx.compose.ui.test.performScrollTo
import org.robolectric.Shadows
import org.junit.Assert.assertEquals

/*
 * Why the real activity: the shell's job is wiring (view model, saved tab,
 * Back, the fixed-area rule against the real window), so these tests launch
 * MainActivity with its real settings store instead of composing the screen
 * by hand. Settings load asynchronously and the screen stays blank until
 * then, so every test first waits for the two tabs. Native graphics give real
 * text measurement, which the fixed-area rule depends on.
 */

/**
 * Waits until the settings have loaded and the two tabs are on screen.
 *
 * @throws androidx.compose.ui.test.ComposeTimeoutException if they don't
 *   appear within 5 seconds.
 */
private fun ComposeTestRule.waitForTabs() {
    waitUntil(timeoutMillis = 5_000) { onAllNodes(hasRole(Role.Tab)).fetchSemanticsNodes().size == 2 }
}

/** Robolectric tests for the app shell at the normal font size on a typical 360 × 800 dp phone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class AppShellTest {
    /** Launches MainActivity for each test. */
    val rule = createAndroidComposeRule<MainActivity>()

    /** Skips the widget refresh that opening Beacon starts, before the activity launches. */
    @get:Rule val chain: RuleChain = RuleChain.outerRule(NoWidgetRefreshRule()).around(rule)

    /** Waits for the settings to load before each test. */
    @Before fun waitForSettings() = rule.waitForTabs()

    /** There are exactly two Tabs; Home starts selected and tapping Alert settings selects it and shows its title. */
    @Test fun twoTabsWithRolesAndSelection() {
        rule.onAllNodes(hasRole(Role.Tab)).assertCountEquals(2)
        rule.onNode(hasRole(Role.Tab) and hasText("Home")).assertIsSelected()
        rule.onNode(hasRole(Role.Tab) and hasText("Alert settings")).performClick().assertIsSelected()
        rule.onNode(hasText("Alert settings") and isHeading()).assertExists()
    }

    /** Back on Alert settings returns to Home instead of leaving the app. */
    @Test fun backFromSettingsGoesHome() {
        // Asserting waits for the recomposition that turns the Back handler on.
        rule.onNode(hasRole(Role.Tab) and hasText("Alert settings")).performClick().assertIsSelected()
        // The dispatcher is what the system Back gesture and button call.
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNode(hasRole(Role.Tab) and hasText("Home")).assertIsSelected()
        assertFalse(rule.activity.isFinishing)
    }

    /** At the normal font the Test alert button is fixed above the tabs, not in the scrolling content. */
    @Test fun testButtonFixedAtNormalFont() {
        rule.onNode(hasTestTag("test_button") and hasAnyAncestor(hasTestTag("scroll_content"))).assertDoesNotExist()
        // The button must exist at all, or the check above would pass on a screen without one.
        val button = rule.onNodeWithTag("test_button").assertIsDisplayed().getBoundsInRoot()
        val tab = rule.onNode(hasRole(Role.Tab) and hasText("Home")).getBoundsInRoot()
        assertTrue("button ${button.bottom} above tabs ${tab.top}", button.bottom <= tab.top)
    }

    /**
     * Switches the window out of touch mode, as a real keyboard press does.
     *
     * In touch mode Compose gives buttons no keyboard focus, and Robolectric
     * starts every window in it. There is no public call, so this uses the
     * window root's own method.
     */
    private fun leaveTouchMode() {
        rule.runOnUiThread {
            val decor = rule.activity.window.decorView
            val root = android.view.View::class.java.getMethod("getViewRootImpl").invoke(decor)!!
            val ensure = root.javaClass.getDeclaredMethod("ensureTouchMode", Boolean::class.javaPrimitiveType)
            ensure.isAccessible = true
            ensure.invoke(root, false)
        }
        rule.mainClock.advanceTimeByFrame()
    }

    /** Presses Tab 10 times and checks focus never reaches the shell's content, its tab bar or its Test alert button. */
    private fun assertTabNeverReachesShell() {
        repeat(10) {
            rule.onRoot().performKeyInput { pressKey(Key.Tab) }
            rule.mainClock.advanceTimeByFrame()
            // The unmerged tree, because the shell's semantics are cleared in the merged one.
            val inShell = rule.onAllNodes(
                isFocused() and (hasAnyAncestor(hasTestTag(SCROLL_CONTENT_TAG)) or hasRole(Role.Tab) or
                    hasAnyAncestor(hasRole(Role.Tab)) or hasTestTag("test_button") or hasTestTag(SCROLL_CONTENT_TAG)),
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            assertTrue("Tab ${it + 1} put focus in the shell: $inShell", inShell.isEmpty())
        }
    }

    /** The shell connects Alert settings' privacy button to the browser: a tap starts a VIEW of the policy. */
    @Test fun privacyButtonOpensThePolicy() {
        rule.onNode(hasRole(Role.Tab) and hasText("Alert settings")).performClick().assertIsSelected()
        rule.onNode(hasText("Read the privacy policy") and hasRole(Role.Button)).performScrollTo().performClick()
        val started = Shadows.shadowOf(rule.activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started?.action)
        assertEquals("https://github.com/yanivil/beacon/blob/main/PRIVACY.md", started?.dataString)
    }

    /** A keyboard user who starts the test from the shell's Test alert button has focus moved to Stop, and Tab can't leave it. */
    @Test fun overlayTakesFocusFromTheTestButton() {
        rule.mainClock.autoAdvance = false
        leaveTouchMode()
        rule.onNodeWithTag("test_button").requestFocus()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("test_button").assertIsFocused()
        // Enter on the focused button, as a keyboard user starts it.
        rule.onRoot().performKeyInput { pressKey(Key.Enter) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).assertIsFocused()
        assertTabNeverReachesShell()
    }

    /** While the test overlay shows, Tab never moves keyboard focus into the shell's content or tab bar under it. */
    @Test fun keyboardFocusStaysOnTheOverlay() {
        // The blink screen asks for a frame every frame, so the clock must be moved by hand.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("test_button").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        leaveTouchMode()
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).requestFocus()
        rule.mainClock.advanceTimeByFrame()
        // Without this check a failed focus request would make every later check pass for nothing.
        rule.onNode(hasText("Stop") and hasRole(Role.Button)).assertIsFocused()
        assertTabNeverReachesShell()
    }
}

/** Robolectric tests for the app shell at the largest system font size on a small 360 × 640 dp phone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppShellLargestFontTest {
    /** Launches MainActivity for each test. */
    val rule = createAndroidComposeRule<MainActivity>()

    /** Sets the 200% font scale before the activity launches, and resets it after. */
    @get:Rule val chain: RuleChain = RuleChain.outerRule(NoWidgetRefreshRule()).around(FontScaleRule(2.0f)).around(rule)

    /** At the largest font the fixed area would take over a third of the window, so the button scrolls with the content. */
    @Test @Config(qualifiers = "w360dp-h640dp") fun testButtonScrollsAtLargestFont() {
        rule.waitForTabs()
        rule.onNode(hasTestTag("test_button") and hasAnyAncestor(hasTestTag("scroll_content"))).assertExists()
        rule.assertNoTextOverflow()
    }
}
