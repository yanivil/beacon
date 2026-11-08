package dev.beacon.alert

import android.os.Handler
import android.os.Looper

/*
 * Why flips are timed from the clock's start: each flip is posted at an
 * absolute time (start + step x half period), not "half a period after the
 * last one", so small delays never add up over a long alert. Even steps turn
 * the light on, matching BlinkClock.isColourPhase, so light and screen agree.
 *
 * Why a refusal ends the run: if the light says no (another app has the
 * camera), retrying on every flip would only hammer the camera service. The
 * blinker gives up quietly and the screen blink carries on alone.
 */

/**
 * Blinks the camera light on the schedule of a [BlinkClock].
 *
 * @param torch The light to drive.
 * @param clock The alert's timing; it also fixes the blink speed.
 * @param looper The thread that runs the flips and the stop.
 */
class TorchBlinker(
    private val torch: TorchSwitch,
    private val clock: BlinkClock,
    private val looper: Looper,
) {
    private val handler = Handler(looper)
    private var step = 0L

    @Volatile
    private var running = false

    private val flip = object : Runnable {
        /** Flips the light for the current step and schedules the next flip. */
        override fun run() {
            if (!running) return
            if (!torch.set(step % 2 == 0L)) {
                // Refused: end the run, make one attempt to leave the light off, never retry.
                running = false
                handler.removeCallbacks(this)
                torch.set(false)
                return
            }
            step++
            scheduleNext()
        }
    }

    /**
     * Posts the flip for the current step, unless the alert ends first.
     */
    private fun scheduleNext() {
        val at = clock.flipTime(step)
        // No stray flip at or after the end; the controller stops the blinker there too.
        val end = clock.endUptimeMillis
        if (end != null && at >= end) return
        handler.postAtTime(flip, at)
    }

    /**
     * Starts blinking, with the first flip (on) at the clock's start.
     *
     * @return False when this phone has no usable camera light.
     */
    fun start(): Boolean {
        if (!torch.available) return false
        handler.removeCallbacks(flip)
        step = 0L
        running = true
        scheduleNext()
        return true
    }

    /**
     * Stops blinking and leaves the light off. Safe to call more than once.
     */
    fun stop() {
        running = false
        handler.removeCallbacks(flip)
        // The light must be switched on the looper's thread, like the flips.
        if (Looper.myLooper() == looper) torch.set(false) else handler.post { torch.set(false) }
    }
}
