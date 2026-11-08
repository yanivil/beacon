package dev.beacon.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the three blink presets and the 3-flashes-per-second safety ceiling. */
class BlinkSpeedTest {
    /** The presets are Slow 30, Medium 60 and Fast 90 blinks per minute. */
    @Test
    fun ratesMatchPresets() {
        assertEquals(listOf(30, 60, 90), BlinkSpeed.entries.map { it.blinksPerMinute })
    }

    /** Half a period is 30,000 ms divided by the rate, rounded down. */
    @Test
    fun halfPeriods() {
        assertEquals(listOf(1000L, 500L, 333L), BlinkSpeed.entries.map { it.halfPeriodMillis })
    }

    /** No preset flashes more than 3 times a second (WCAG 2.3.1). */
    @Test
    fun neverMoreThanThreeFlashesPerSecond() {
        BlinkSpeed.entries.forEach { assertTrue(it.name, 1000.0 / (2 * it.halfPeriodMillis) <= 3.0) }
    }
}
