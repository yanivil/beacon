package dev.beacon.alert

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** Robolectric tests for [AlertController]; the main looper plays both threads. */
@RunWith(AndroidJUnit4::class)
class AlertControllerTest {
    /**
     * Builds a controller on the main looper.
     *
     * @param fake The pretend camera light.
     * @return A controller whose timing one looper idle drives.
     */
    private fun controller(fake: FakeTorchSwitch) =
        AlertController(fake, Looper.getMainLooper(), Looper.getMainLooper())

    /**
     * Advances the paused main looper.
     *
     * @param millis How far to move time forward.
     */
    private fun idle(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** Builds the 3-second Test alert request. */
    private fun testRequest() =
        AlertRequest(AlertKind.TEST, FlashColour.YELLOW, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS, null)

    /** Pins that a test alert runs, then ends by itself after 3 seconds. */
    @Test fun startRunsAndEndsAfterThreeSeconds() {
        val c = controller(FakeTorchSwitch())
        assertTrue(c.start(testRequest())); assertTrue(c.state.value is AlertState.Running)
        idle(3_000)
        assertEquals(AlertState.Idle, c.state.value)
    }

    /** Pins that a second start while running is dropped and the light runs once. */
    @Test fun secondStartIsDropped() {
        val fake = FakeTorchSwitch(); val c = controller(fake)
        assertTrue(c.start(testRequest())); assertFalse(c.start(testRequest()))
        idle(10)
        assertEquals(1, fake.events.count { it.second })
    }

    /** Pins that stop goes idle and leaves the light off. */
    @Test fun stopTurnsLightOffAndGoesIdle() {
        val fake = FakeTorchSwitch(); val c = controller(fake)
        c.start(testRequest()); idle(200)
        c.stop(); idle(10)
        assertEquals(AlertState.Idle, c.state.value)
        assertFalse(fake.events.last().second)
    }

    /** Pins that without a light the alert still runs, screen only. */
    @Test fun noLightStillRunsScreenOnly() {
        val c = controller(FakeTorchSwitch(available = false))
        assertTrue(c.start(testRequest()))
        assertFalse((c.state.value as AlertState.Running).lightFlashing)
        assertFalse(c.hasLight)
    }

    /** Pins that the automatic end leaves the light off. */
    @Test fun autoEndLeavesLightOff() {
        val fake = FakeTorchSwitch(); val c = controller(fake)
        c.start(testRequest()); idle(3_000)
        assertFalse(fake.events.last().second)
    }
}
