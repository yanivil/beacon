package dev.beacon.alert

import android.os.Looper
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.BlinkSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** Robolectric tests for [TorchBlinker] on the paused main looper. */
@RunWith(AndroidJUnit4::class)
class TorchBlinkerTest {
    private val looper: Looper = Looper.getMainLooper()

    /**
     * Advances the paused main looper.
     *
     * @param millis How far to move time forward.
     */
    private fun idle(millis: Long) = shadowOf(looper).idleFor(Duration.ofMillis(millis))

    /** Pins that flips land on the clock: on at 0, off at 500, on at 1000 for Medium. */
    @Test fun flipsOnTheClock() {
        val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.MEDIUM, null), looper)
        assertTrue(b.start()); idle(1_000)
        assertEquals(listOf(0L to true, 500L to false, 1_000L to true), fake.events.map { (t, on) -> (t - now) to on })
        b.stop()
    }

    /** Pins that stop ends with the light off and nothing happens afterwards. */
    @Test fun stopLeavesLightOff() {
        val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.MEDIUM, null), looper)
        b.start(); idle(700)
        b.stop(); idle(1)
        val countAfterStop = fake.events.size
        assertFalse(fake.events.last().second)
        idle(2_000)
        assertEquals(countAfterStop, fake.events.size)
        assertFalse(fake.events.last().second)
    }

    /** Pins that stopping twice is harmless and schedules nothing. */
    @Test fun stopTwiceIsSafe() {
        val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.FAST, null), looper)
        b.start(); idle(100)
        b.stop(); b.stop(); idle(2_000)
        assertFalse(fake.events.last().second)
    }

    /** Pins that start reports false on a phone without a camera light. */
    @Test fun noLightReturnsFalse() {
        assertFalse(TorchBlinker(FakeTorchSwitch(available = false), BlinkClock(0, BlinkSpeed.FAST, null), looper).start())
    }

    /** Pins that a refused command ends the run with one final off and no more flips. */
    @Test fun refusedLightStopsQuietly() {
        val fake = FakeTorchSwitch(refuseFrom = 2); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.FAST, null), looper)
        b.start(); idle(2_000)
        // Call 1 ON, call 2 refused, call 3 the final off.
        assertTrue(fake.events.size <= 3)
        assertFalse(fake.events.last().second)
    }
}
