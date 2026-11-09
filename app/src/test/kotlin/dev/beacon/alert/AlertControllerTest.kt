package dev.beacon.alert

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import android.os.HandlerThread
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
     * @param platform The pretend phone services.
     * @param torchLooper The looper that switches the light; the main looper unless a test needs its own.
     * @return A controller whose timing one looper idle drives.
     */
    private fun controller(
        fake: FakeTorchSwitch,
        platform: FakeAlertPlatform = FakeAlertPlatform(),
        torchLooper: Looper = Looper.getMainLooper(),
    ) = AlertController(fake, torchLooper, Looper.getMainLooper(), platform)

    /**
     * Builds a real WhatsApp request in Green at Medium.
     *
     * @param length How long it lasts, or null for until stopped.
     * @return The request.
     */
    private fun real(length: Long?) =
        AlertRequest(AlertKind.REAL, FlashColour.GREEN, BlinkSpeed.MEDIUM, length, MessageSource.WHATSAPP)

    /**
     * Advances the paused main looper.
     *
     * @param millis How far to move time forward.
     */
    private fun idle(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /**
     * Builds the 3-second Test alert request.
     *
     * @return A Test alert request: Yellow, Medium speed, 3 seconds, no source app.
     */
    private fun testRequest() =
        AlertRequest(AlertKind.TEST, FlashColour.YELLOW, BlinkSpeed.MEDIUM, TEST_LENGTH_MILLIS, null)

    /** Pins that a test alert runs, then ends by itself after 3 seconds. */
    @Test fun startRunsAndEndsAfterThreeSeconds() {
        val c = controller(FakeTorchSwitch())
        assertTrue(c.start(testRequest())); assertTrue(c.state.value is AlertState.Running)
        idle(3_000)
        assertEquals(AlertState.Idle, c.state.value)
    }

    /** A real alert replacing a test goes straight from Running(TEST) to Running(REAL), with no Idle in between. */
    @Test fun replacingATestNeverShowsIdle() {
        val c = controller(FakeTorchSwitch())
        val kinds = mutableListOf<AlertKind?>()
        // Unconfined, so every value the state takes is seen, not just the last one.
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.launch { c.state.collect { kinds += (it as? AlertState.Running)?.request?.kind } }
        c.start(testRequest())
        c.start(real(60_000L))
        idle(10)
        scope.cancel()
        assertEquals(listOf(null, AlertKind.TEST, AlertKind.REAL), kinds)
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

    /** Pins that a real alert replaces a test and opens the alert window once. */
    @Test fun realReplacesTest() {
        val p = FakeAlertPlatform(canShow = true); val c = controller(FakeTorchSwitch(), p)
        c.start(testRequest()); assertTrue(c.start(real(60_000)))
        assertEquals(AlertKind.REAL, (c.state.value as AlertState.Running).request.kind); assertEquals(1, p.screensShown)
    }

    /** Pins that a test never replaces a running alert. */
    @Test fun testNeverReplacesAnAlert() {
        val c = controller(FakeTorchSwitch())
        c.start(real(60_000)); assertFalse(c.start(testRequest()))
        c.stop(); c.start(testRequest()); assertFalse(c.start(testRequest()))
    }

    /** Pins that a real alert never replaces a real alert. */
    @Test fun realNeverReplacesReal() {
        val p = FakeAlertPlatform(canShow = true); val c = controller(FakeTorchSwitch(), p)
        assertTrue(c.start(real(60_000))); assertFalse(c.start(real(60_000)))
        assertEquals(1, p.screensShown)
    }

    /** Pins that without the overlay permission the alert is light-only. */
    @Test fun noScreenWithoutOverlayPermission() {
        val p = FakeAlertPlatform(canShow = false); val c = controller(FakeTorchSwitch(), p)
        assertTrue(c.start(real(60_000))); assertEquals(0, p.screensShown); assertTrue(c.state.value is AlertState.Running)
    }

    /** Pins that a test never opens the alert window. */
    @Test fun testNeverOpensTheAlertScreen() {
        val p = FakeAlertPlatform(canShow = true); val c = controller(FakeTorchSwitch(), p)
        c.start(testRequest()); assertEquals(0, p.screensShown)
    }

    /** Pins the wake lock length: the alert's length plus the margin, released on stop. */
    @Test fun wakeLockCoversLengthPlusMargin() {
        val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p)
        c.start(real(60_000)); assertEquals(listOf(70_000L), p.wakeLocks)
        c.stop(); assertTrue(p.released)
    }

    /** Pins that an alert with no end renews its wake lock every hour until stopped. */
    @Test fun untilStoppedRenewsEveryHour() {
        val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p)
        c.start(real(null)); assertEquals(listOf(3_610_000L), p.wakeLocks)
        idle(3_600_000); assertEquals(listOf(3_610_000L, 3_610_000L), p.wakeLocks)
        c.stop(); idle(3_600_000); assertEquals(2, p.wakeLocks.size)
    }

    /** Pins that a phone already too hot gets no light but still has one. */
    @Test fun tooHotAtStartMeansNoLight() {
        val fake = FakeTorchSwitch(); val c = controller(fake, FakeAlertPlatform(tooHot = true))
        c.start(real(60_000)); idle(1_000)
        assertFalse((c.state.value as AlertState.Running).lightFlashing); assertTrue(fake.events.isEmpty()); assertTrue(c.hasLight)
    }

    /** Pins that heat during an alert stops the light and keeps the alert running. */
    @Test fun heatStopsLightAndKeepsScreen() {
        val fake = FakeTorchSwitch(); val p = FakeAlertPlatform(); val c = controller(fake, p)
        c.start(real(60_000)); idle(200); p.fireTooHot(); idle(10)
        val r = c.state.value as AlertState.Running
        assertFalse(r.lightFlashing); assertFalse(fake.events.last().second)
        idle(1_000); assertTrue(fake.events.none { it.first > r.clock.startUptimeMillis + 300 && it.second })
    }

    /** Pins that stopIf ends only an alert of its kind. */
    @Test fun stopIfMatchesOnlyItsKind() {
        val c = controller(FakeTorchSwitch())
        c.start(testRequest()); c.stopIf(AlertKind.REAL)
        assertTrue(c.state.value is AlertState.Running)
        c.stopIf(AlertKind.TEST); assertEquals(AlertState.Idle, c.state.value)
    }

    /** Pins that stop ends the heat watch. */
    @Test fun stopEndsHeatWatch() {
        val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p)
        c.start(real(60_000)); assertTrue(p.watching)
        c.stop(); assertFalse(p.watching)
    }

    /** Pins that a heat callback arriving after the alert ended changes nothing. */
    @Test fun lateHeatCallbackIsIgnored() {
        val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p)
        c.start(real(60_000))
        val late = p.captureHeatCallback()
        c.stop(); c.start(real(60_000)); late()
        assertTrue((c.state.value as AlertState.Running).lightFlashing)
    }

    /** Pins that the wake lock is released only after the torch looper has run the light's off. */
    @Test fun wakeLockReleasedBehindTheOff() {
        val thread = HandlerThread("test-torch").apply { start() }
        val torchLooper = thread.looper
        val fake = FakeTorchSwitch(); val p = FakeAlertPlatform(); val c = controller(fake, p, torchLooper)
        c.start(real(60_000)); shadowOf(torchLooper).idle()
        c.stop()
        assertFalse(p.released)
        shadowOf(torchLooper).idle()
        assertFalse(p.released)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(p.released); assertFalse(fake.events.last().second)
        thread.quit()
    }

    /** Pins that a test's pending release does not free the lock of the real alert that replaced it. */
    @Test fun replacedTestsPendingReleaseIsNoOp() {
        val thread = HandlerThread("test-torch").apply { start() }
        val torchLooper = thread.looper
        val p = FakeAlertPlatform(); val c = controller(FakeTorchSwitch(), p, torchLooper)
        c.start(testRequest()); shadowOf(torchLooper).idle()
        assertTrue(c.start(real(60_000)))
        shadowOf(torchLooper).idle(); shadowOf(Looper.getMainLooper()).idle()
        assertFalse(p.released)
        thread.quit()
    }

    /** Pins that a refused heat listener falls back to polling, which stops the light when it gets hot. */
    @Test fun refusedListenerPollsTheHeat() {
        val fake = FakeTorchSwitch(); val p = FakeAlertPlatform(watchAccepted = false); val c = controller(fake, p)
        c.start(real(null)); idle(200)
        p.tooHot = true; idle(30_000)
        val r = c.state.value as AlertState.Running
        assertFalse(r.lightFlashing); assertFalse(fake.events.last().second)
    }

    /**
     * Runs the main looper's next due tasks one at a time: here the blinker's start, then its first flip.
     * A refused flip posts its report to the main looper, and this leaves that report waiting, so a test
     * can end or replace the alert before it arrives, as a late delivery would.
     *
     * @param count How many tasks to run.
     */
    private fun runTasks(count: Int) = repeat(count) { shadowOf(Looper.getMainLooper()).runOneTask() }

    /**
     * Pins that a refused light ends the heat poll and leaves everything else running: the poll is posted
     * because the heat listener is refused, then the light refuses at its second flip.
     */
    @Test fun aRefusedLightStopsTheHeatPoll() {
        val fake = FakeTorchSwitch(refuseFrom = 2); val p = FakeAlertPlatform(watchAccepted = false); val c = controller(fake, p)
        c.start(real(null)); idle(1_000)
        val checks = p.heatChecks
        idle(HEAT_POLL_MILLIS * 4)
        assertEquals(checks, p.heatChecks)
        val r = c.state.value as AlertState.Running
        assertFalse(r.lightFlashing)
        assertFalse(p.released)
        assertFalse(fake.events.last().second)
    }

    /** Pins that a refusal reported after the alert ended changes nothing, not even a newer alert. */
    @Test fun aRefusalAfterTheAlertEndedChangesNothing() {
        val fake = FakeTorchSwitch(refuseFrom = 1); val p = FakeAlertPlatform(watchAccepted = false); val c = controller(fake, p)
        c.start(real(60_000))
        // The start, then the first flip: refused, the light left off, and the report posted but not yet run.
        runTasks(2)
        assertFalse(fake.events.last().second)
        c.stop()
        assertEquals(AlertState.Idle, c.state.value)
        // A new alert whose light works starts before the old report arrives.
        fake.refuseFrom = Int.MAX_VALUE
        c.start(real(60_000)); idle(10)
        assertTrue((c.state.value as AlertState.Running).lightFlashing)
        // Its heat poll still runs.
        val checks = p.heatChecks
        idle(HEAT_POLL_MILLIS)
        assertEquals(checks + 1, p.heatChecks)
    }

    /** Pins that a refusal from a test that a real alert replaced leaves the real alert's light and poll alone. */
    @Test fun aRefusalOfAReplacedTestChangesTheNewAlertNothing() {
        val fake = FakeTorchSwitch(refuseFrom = 1); val p = FakeAlertPlatform(watchAccepted = false); val c = controller(fake, p)
        c.start(testRequest())
        // The test's start, then its first flip: refused, and the report left waiting.
        runTasks(2)
        fake.refuseFrom = Int.MAX_VALUE
        assertTrue(c.start(real(60_000)))
        idle(10)
        val r = c.state.value as AlertState.Running
        assertEquals(AlertKind.REAL, r.request.kind)
        assertTrue(r.lightFlashing)
        val checks = p.heatChecks
        idle(HEAT_POLL_MILLIS)
        assertEquals(checks + 1, p.heatChecks)
    }

    /** Pins that stop cancels the heat poll. */
    @Test fun stopEndsHeatPoll() {
        val p = FakeAlertPlatform(watchAccepted = false); val c = controller(FakeTorchSwitch(), p)
        c.start(real(null)); idle(100)
        val checks = p.heatChecks
        c.stop(); idle(60_000)
        assertEquals(checks, p.heatChecks)
    }
}
