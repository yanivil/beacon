package dev.beacon.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the fixed-area rule of the bottom area (spec section 3, row 9). */
class BottomAreaTest {
    /** The Test alert button stays fixed while the fixed area is at most a third of the window. */
    @Test fun fixedUpToAThird() {
        assertTrue(testButtonFixed(300, 900))
        assertFalse(testButtonFixed(301, 900))
    }
}
