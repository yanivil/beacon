package dev.beacon.alert

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/*
 * Why flips are posted at absolute times: each flip is due at start + step x
 * half period, not "half a period after the last one", so small delays never
 * add up over a long alert. Even steps turn the light on, matching
 * BlinkClock.isColourPhase, so light and screen agree.
 *
 * Why a missed flip is never replayed: setTorchMode can stall inside the
 * camera service. If the blinker then ran every overdue flip back to back, the
 * light would flicker far faster than 3 flashes a second (WCAG 2.3.1). Instead
 * each flip works out which step the clock is in right now, sets the light to
 * that phase once, and schedules the step after it.
 *
 * Why a refusal ends the run: if the light says no (another app has the
 * camera), retrying on every flip would only hammer the camera service. The
 * blinker gives up quietly and the screen blink carries on alone.
 *
 * Why the looper: start, the flips and the final off all touch the same state,
 * so they run on one thread and can't overlap. stop() is called from any
 * thread: it cancels a pending start right away on the caller's thread (the
 * Handler's remove methods are thread-safe), then does the off on the looper,
 * inline when the caller is already on it.
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

    private val flip: Runnable = object : Runnable {
        /** Sets the light to the clock's current phase and schedules the next flip. */
        override fun run() {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            if (clock.isFinished(now)) {
                finish.run()
                return
            }
            // Skip any steps that were missed while stalled; never replay them.
            val current = maxOf(0L, (now - clock.startUptimeMillis) / clock.speed.halfPeriodMillis)
            if (!torch.set(current % 2 == 0L)) {
                // Refused: end the run, make one attempt to leave the light off, never retry.
                running = false
                handler.removeCallbacks(this)
                handler.removeCallbacks(finish)
                torch.set(false)
                return
            }
            step = current + 1
            scheduleNext()
        }
    }

    private val finish: Runnable = object : Runnable {
        /** Ends the run with the light off. */
        override fun run() {
            if (!running) return
            running = false
            handler.removeCallbacks(flip)
            torch.set(false)
        }
    }

    /**
     * Posts the flip for the current step, or the final off if the alert ends first.
     */
    private fun scheduleNext() {
        val at = clock.flipTime(step)
        val end = clock.endUptimeMillis
        if (end != null && at >= end) {
            // The next flip would be an "on" at or after the end; turn the light off at the end instead.
            handler.postAtTime(finish, end)
            return
        }
        handler.postAtTime(flip, at)
    }

    private val startRun = Runnable {
        handler.removeCallbacks(flip)
        handler.removeCallbacks(finish)
        running = true
        handler.postAtTime(flip, clock.flipTime(0))
    }

    /**
     * Starts blinking. The first flip happens on the looper after this returns,
     * at the clock's start, or at once in the clock's current phase if the
     * start has already passed.
     *
     * @return False when this phone has no usable camera light.
     */
    fun start(): Boolean {
        if (!torch.available) return false
        // Posted so it can't overlap a flip running on the looper; stop() can cancel it.
        handler.post(startRun)
        return true
    }

    /**
     * Stops blinking and leaves the light off. Safe to call more than once.
     */
    fun stop() {
        running = false
        // Cancel a start that hasn't run yet, or it would restart the blinking after the off.
        handler.removeCallbacks(startRun)
        val off = Runnable {
            handler.removeCallbacks(flip)
            handler.removeCallbacks(finish)
            torch.set(false)
        }
        // The light must be switched on the looper's thread, like the flips.
        if (Looper.myLooper() == looper) off.run() else handler.post(off)
    }
}
