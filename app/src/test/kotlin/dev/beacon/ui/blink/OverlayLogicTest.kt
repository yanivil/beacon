package dev.beacon.ui.blink

import androidx.compose.ui.graphics.Color
import dev.beacon.alert.BlinkClock
import dev.beacon.alert.TEST_LENGTH_MILLIS
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import dev.beacon.ui.theme.BeaconColors
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why a safety test on the background: the camera light and the screen must
 * blink from one formula, BlinkClock.isColourPhase, which only takes the
 * three safe presets (WCAG 2.3.1). This pins that the overlay's colour comes
 * from that clock at every millisecond, so a second, faster formula can't
 * creep into the screen. The countdown cases pin its rounding and clamping
 * at the edges, which the Robolectric tests can't reach exactly.
 */

/** Plain JVM tests for the overlay's background colour and countdown. */
class OverlayLogicTest {
    /** For every preset, the background is the flash colour exactly when the clock is in its colour phase, else black. */
    @Test fun backgroundFollowsTheClockPhase() {
        BlinkSpeed.entries.forEach { speed ->
            val clock = BlinkClock(1_000, speed, TEST_LENGTH_MILLIS)
            // From just before the start to just after the end, every millisecond.
            for (now in 990L..4_010L) {
                val expected = if (clock.isColourPhase(now)) Color(FlashColour.BLUE.argb) else BeaconColors.Black
                assertEquals("$speed at $now", expected, blinkColour(clock, FlashColour.BLUE, now))
            }
        }
    }

    /** The countdown rounds the time left up to whole seconds and stays within 1 to 3. */
    @Test fun countdownRoundsUpAndClamps() {
        val clock = BlinkClock(1_000, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS)
        assertEquals(3, countdownSeconds(clock, 500)) // before the start: 3.5 s left, clamped to 3
        assertEquals(3, countdownSeconds(clock, 1_000))
        assertEquals(3, countdownSeconds(clock, 1_999))
        assertEquals(2, countdownSeconds(clock, 2_000))
        assertEquals(1, countdownSeconds(clock, 3_999))
        assertEquals(1, countdownSeconds(clock, 4_000)) // at the end: 0 s left, still "1" until the overlay closes
    }
}
