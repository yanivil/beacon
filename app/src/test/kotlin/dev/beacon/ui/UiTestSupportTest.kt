package dev.beacon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.ui.theme.BeaconType
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/*
 * Why the helper is tested: every largest-font test trusts
 * assertNoTextOverflow to fail when text is cut off. If the helper stopped
 * catching overflow, those tests would all pass and prove nothing. These
 * tests show it fails on the two ways a text gets cut (ellipsized, and too
 * tall for its box) and passes on text that fits. Native graphics give real
 * text measurement, which the helper needs.
 */

/** Robolectric Compose tests for the shared test helpers in UiTestSupport.kt. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiTestSupportTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /**
     * Runs the overflow check and expects it to fail because a text is cut
     * off, not because it found no text.
     *
     * @throws AssertionError if the check passes or fails for another reason.
     */
    private fun assertOverflowCaught() {
        val error = assertThrows(AssertionError::class.java) { rule.assertNoTextOverflow() }
        assertTrue("failed for another reason: ${error.message}", error.message.orEmpty().startsWith("Text overflows"))
    }

    /** The check fails on one ellipsized line in a box too narrow for it. */
    @Test fun failsOnEllipsizedText() {
        rule.setContent {
            Box(Modifier.width(80.dp)) {
                BasicText("A sentence far too long for this box", style = BeaconType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        assertOverflowCaught()
    }

    /** The check fails on clipped text in a box too short for its lines. */
    @Test fun failsOnTextClippedByAShortBox() {
        rule.setContent {
            Box(Modifier.width(200.dp).height(20.dp)) {
                BasicText("Several words that wrap onto more than one line", style = BeaconType.Body, overflow = TextOverflow.Clip)
            }
        }
        assertOverflowCaught()
    }

    /** The check passes on a text that fits its box. */
    @Test fun passesOnTextThatFits() {
        rule.setContent {
            Box(Modifier.width(300.dp)) {
                BasicText("Fits", style = BeaconType.Body)
            }
        }
        rule.assertNoTextOverflow()
    }
}
