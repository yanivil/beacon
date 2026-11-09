package dev.beacon.alert

import dev.beacon.settings.BlinkSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plain JVM tests for [BlinkClock]. */
class BlinkClockTest {
    /** Pins the colour/off phases: colour first, and colour before the start. */
    @Test fun phases() {
        val c = BlinkClock(1_000, BlinkSpeed.MEDIUM, 3_000)
        assertTrue(c.isColourPhase(1_000)); assertTrue(c.isColourPhase(1_499))
        assertFalse(c.isColourPhase(1_500)); assertTrue(c.isColourPhase(2_000)); assertTrue(c.isColourPhase(999))
    }

    /** Pins the end time, isFinished and a remaining time that never goes below 0. */
    @Test fun endAndRemaining() {
        val c = BlinkClock(1_000, BlinkSpeed.FAST, 3_000)
        assertEquals(4_000L, c.endUptimeMillis); assertFalse(c.isFinished(3_999)); assertTrue(c.isFinished(4_000))
        assertEquals(2_000L, c.remainingMillis(2_000)); assertEquals(0L, c.remainingMillis(9_000))
    }

    /** Pins that an alert with no length has no end and no remaining time. */
    @Test fun untilStoppedNeverEnds() {
        val c = BlinkClock(0, BlinkSpeed.SLOW, null)
        assertNull(c.endUptimeMillis); assertFalse(c.isFinished(Long.MAX_VALUE / 2)); assertNull(c.remainingMillis(5))
    }
}
