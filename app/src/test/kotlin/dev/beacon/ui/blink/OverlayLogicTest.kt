package dev.beacon.ui.blink

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import dev.beacon.R
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
 * at the edges, which the Robolectric tests can't reach exactly. The bar-strip
 * cases pin where the blink is drawn: Robolectric gives a window no system-bar
 * insets, so the rule is checked here as plain geometry, and on the emulator.
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

    /**
     * During the colour phase, the strips behind the system bars stay black and the rest of the
     * window shows the flash colour (D1: the bars stay visible, white icons on black).
     */
    @Test fun barStripsStayBlackDuringTheColourPhase() {
        val clock = BlinkClock(1_000, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS)
        assertEquals(true, clock.isColourPhase(1_000)) // the colour phase comes first
        val yellow = blinkColour(clock, FlashColour.YELLOW, 1_000)

        // Portrait, gesture navigation: status bar on top, navigation bar at the bottom.
        val portrait = blinkRegions(Size(1_080f, 2_400f), left = 0f, top = 136f, right = 0f, bottom = 63f, yellow)
        assertEquals(BeaconColors.Black, portrait.paintedAt(Offset(540f, 60f)))
        assertEquals(BeaconColors.Black, portrait.paintedAt(Offset(540f, 2_380f)))
        assertEquals(Color(FlashColour.YELLOW.argb), portrait.paintedAt(Offset(540f, 1_200f)))

        // Landscape, 3-button navigation: the navigation bar is a strip at the side.
        val landscape = blinkRegions(Size(2_400f, 1_080f), left = 0f, top = 63f, right = 126f, bottom = 0f, yellow)
        assertEquals(BeaconColors.Black, landscape.paintedAt(Offset(2_380f, 540f)))
        assertEquals(Color(FlashColour.YELLOW.argb), landscape.paintedAt(Offset(1_200f, 540f)))
    }

    /** Rotation 270 with 3-button navigation puts the navigation bar at the left: its strip stays black. */
    @Test fun leftBarStripIsBlack() {
        val clock = BlinkClock(1_000, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS)
        val yellow = blinkColour(clock, FlashColour.YELLOW, 1_000)
        val regions = blinkRegions(Size(2_400f, 1_080f), left = 48f, top = 63f, right = 0f, bottom = 0f, yellow)
        assertEquals(BeaconColors.Black, regions.paintedAt(Offset(10f, 540f)))
        assertEquals(Color(FlashColour.YELLOW.argb), regions.paintedAt(Offset(1_200f, 540f)))
    }

    /** In the black phase the whole window is black, strips and all. */
    @Test fun blackPhaseIsBlackEverywhere() {
        val clock = BlinkClock(1_000, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS)
        val black = blinkColour(clock, FlashColour.YELLOW, 1_000 + BlinkSpeed.MEDIUM.halfPeriodMillis)
        val regions = blinkRegions(Size(1_080f, 2_400f), left = 0f, top = 136f, right = 0f, bottom = 63f, black)
        assertEquals(BeaconColors.Black, regions.paintedAt(Offset(540f, 1_200f)))
        assertEquals(BeaconColors.Black, regions.paintedAt(Offset(540f, 60f)))
    }

    /**
     * The colour a canvas ends up with at [point] after painting these regions in order.
     *
     * @param point a point in the window, in pixels.
     * @return the colour of the last region that covers the point.
     * @throws NoSuchElementException if no region covers the point, which would leave it unpainted.
     */
    private fun List<Pair<Rect, Color>>.paintedAt(point: Offset): Color = last { it.first.contains(point) }.second

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

    /** The camera row says "Flashing too", "Off while the phone is hot" or "Not on this phone". */
    @Test fun cameraLine() {
        assertEquals(R.string.test_camera_on, cameraLineRes(hasLight = true, lightFlashing = true))
        assertEquals(R.string.test_camera_hot, cameraLineRes(hasLight = true, lightFlashing = false))
        assertEquals(R.string.test_camera_none, cameraLineRes(hasLight = false, lightFlashing = false))
    }
}
