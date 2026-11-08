package dev.beacon.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the fixed custom-length steps and how Shorter/Longer move along them. */
class CustomLengthTest {
    /** The allowed steps are exactly the design's ten values, in order. */
    @Test
    fun stepsInOrder() {
        assertEquals(listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60), CustomLength.STEPS)
    }

    /** A value that is not a step becomes the 10-minute default. */
    @Test
    fun unknownSnapsToTen() {
        assertEquals(10, CustomLength.of(7).minutes)
        assertEquals(10, CustomLength.of(0).minutes)
    }

    /** Longer and Shorter move exactly one step. */
    @Test
    fun stepsMoveOneAtATime() {
        assertEquals(15, CustomLength.of(10).longer().minutes)
        assertEquals(5, CustomLength.of(10).shorter().minutes)
    }

    /** At 1 and 60 minutes the length stays put and the end flags are set. */
    @Test
    fun endsStayPut() {
        assertEquals(1, CustomLength.of(1).shorter().minutes)
        assertTrue(CustomLength.of(1).isShortest)
        assertEquals(60, CustomLength.of(60).longer().minutes)
        assertTrue(CustomLength.of(60).isLongest)
    }
}
