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
import org.robolectric.shadows.ShadowSystemClock
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

    /** Pins that a stalled set() never causes a burst: no more than 3 flashes a second, always in phase. */
    @Test fun stallDoesNotReplayFlips() {
        val now = SystemClock.uptimeMillis()
        val clock = BlinkClock(now, BlinkSpeed.FAST, null)
        val fake = FakeTorchSwitch(onCall = { k ->
            // Stands in for a blocking binder call on the 2nd command.
            if (k == 2) ShadowSystemClock.advanceBy(Duration.ofMillis(2_000))
        })
        val b = TorchBlinker(fake, clock, looper)
        b.start(); idle(5_000)
        val ons = fake.events.filter { it.second }.map { it.first }
        for (t in ons) assertTrue(ons.count { it >= t && it < t + 1_000 } <= 3)
        for ((t, on) in fake.events) assertEquals(clock.isColourPhase(t), on)
        b.stop()
    }

    /** Pins that a clock that started in the past gives one flip in the current phase, not a replay. */
    @Test fun pastStartDoesNotReplay() {
        val now = SystemClock.uptimeMillis()
        val clock = BlinkClock(now - 5_000, BlinkSpeed.FAST, null)
        val fake = FakeTorchSwitch()
        val b = TorchBlinker(fake, clock, looper)
        b.start(); idle(1)
        assertEquals(1, fake.events.size)
        val (t, on) = fake.events.single()
        assertEquals(clock.isColourPhase(t), on)
        b.stop()
    }

    /** Pins that start, stop and start again gives no burst of catch-up flips. */
    @Test fun restartDoesNotReplay() {
        val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.FAST, null), looper)
        b.start(); idle(5_000)
        b.stop()
        val afterStop = fake.events.size
        b.start(); idle(1)
        assertEquals(1, fake.events.size - afterStop)
        b.stop()
    }

    /** Pins that a finite alert ends with the light off at its end and nothing after. */
    @Test fun finiteAlertEndsWithLightOff() {
        val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.MEDIUM, 1_200), looper)
        b.start(); idle(3_000)
        assertEquals(
            listOf(0L to true, 500L to false, 1_000L to true, 1_200L to false),
            fake.events.map { (t, on) -> (t - now) to on },
        )
    }

    /** Pins that stop() right after start() cancels the pending start: no ON ever, last event OFF. */
    @Test fun stopCancelsPendingStart() {
        val fake = FakeTorchSwitch(); val now = SystemClock.uptimeMillis()
        val b = TorchBlinker(fake, BlinkClock(now, BlinkSpeed.MEDIUM, null), looper)
        b.start(); b.stop(); idle(2_000)
        assertTrue(fake.events.none { it.second })
        assertFalse(fake.events.last().second)
    }
}
