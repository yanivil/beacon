package dev.beacon.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Why this test exists: WCAG AAA (7:1) is a non-negotiable project rule, so
 * the build itself must fail if any text colour pair drops below it.
 */

/** Pins the contrast function and enforces 7:1 on every listed text pair. */
class ContrastTest {

    /** White on black is exactly 21:1 and Disabled on Card is 7.78:1. */
    @Test fun knownRatios() {
        assertEquals(21.0, contrastRatio(Color.White, Color.Black), 0.01)
        assertEquals(7.78, contrastRatio(Color(0xFFAEAEB6), Color(0xFF1B1B20)), 0.01)
    }

    /** Every pair in BeaconColors.textPairs reaches the AAA ratio of 7:1. */
    @Test fun everyTextPairIsAtLeast7to1() {
        BeaconColors.textPairs.forEach {
            val ratio = contrastRatio(it.text, it.background)
            assertTrue("${it.use}: $ratio", ratio >= 7.0)
        }
    }

    /** The list holds exactly the 11 pairs of spec section 11. */
    @Test fun elevenPairsListed() {
        assertEquals(11, BeaconColors.textPairs.size)
    }
}
