package dev.beacon.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.CustomLength
import dev.beacon.settings.DurationChoice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/*
 * Why these tests: the summaries feed Home, the Test alert overlay and the
 * widget, so their wording (singular and plural minutes, the speed's rate) is
 * pinned in one place. Each text is shown through a real composition, because
 * the summaries read plurals and strings from resources.
 */

/** Robolectric tests for the shared summary texts. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SummariesTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** Shows [text] on screen so the test can look it up. */
    private fun showText(text: @androidx.compose.runtime.Composable () -> String) {
        rule.setContent { androidx.compose.foundation.text.BasicText(text()) }
    }

    /** Every duration choice reads as the design's words, with correct singular and plural. */
    @Test fun durationTexts() {
        var settings by mutableStateOf(BeaconSettings.DEFAULT)
        showText { durationSummary(settings) }
        rule.onNodeWithText("1 minute").assertExists()
        settings = settings.copy(duration = DurationChoice.FIVE_MINUTES)
        rule.onNodeWithText("5 minutes").assertExists()
        settings = settings.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(1))
        rule.onNodeWithText("1 minute").assertExists()
        settings = settings.copy(customLength = CustomLength.of(45))
        rule.onNodeWithText("45 minutes").assertExists()
        settings = settings.copy(duration = DurationChoice.UNTIL_STOPPED)
        rule.onNodeWithText("Until I stop it").assertExists()
    }

    /** A speed reads as its name and its blinks a minute. */
    @Test fun speedText() {
        var speed by mutableStateOf(BlinkSpeed.MEDIUM)
        showText { speedSummary(speed) }
        rule.onNodeWithText("Medium, 60 a minute").assertExists()
        speed = BlinkSpeed.FAST
        rule.onNodeWithText("Fast, 90 a minute").assertExists()
    }
}
