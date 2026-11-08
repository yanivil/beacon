package dev.beacon.alert

import dev.beacon.settings.BlinkSpeed

/*
 * Why one clock: the screen colour and the camera light must blink together.
 * Both read their phase from the same start time and the same BlinkSpeed, so
 * they can't drift apart. The clock takes the preset, never a raw number, so
 * the rate can't go above the safe limit (WCAG 2.3.1).
 */

/**
 * Fixes the timing of one alert: when it starts, how fast it blinks and when
 * it ends. All times are `SystemClock.uptimeMillis()` values.
 *
 * @param startUptimeMillis When the first flash begins.
 * @param speed The blink-speed preset.
 * @param lengthMillis How long the alert lasts, or null to run until stopped.
 */
class BlinkClock(
    val startUptimeMillis: Long,
    val speed: BlinkSpeed,
    val lengthMillis: Long?,
) {
    /** When the alert ends, or null when it runs until stopped. */
    val endUptimeMillis: Long? = lengthMillis?.let { startUptimeMillis + it }

    /**
     * Tells whether the screen colour (and the light) should be on.
     *
     * @param nowUptimeMillis The current uptime.
     * @return True during the colour half of a blink; also true before the start.
     */
    fun isColourPhase(nowUptimeMillis: Long): Boolean {
        // Before the start there is no phase yet; show the colour, as at step 0.
        if (nowUptimeMillis < startUptimeMillis) return true
        val step = (nowUptimeMillis - startUptimeMillis) / speed.halfPeriodMillis
        return step % 2 == 0L
    }

    /**
     * Tells whether the alert's time is up.
     *
     * @param nowUptimeMillis The current uptime.
     * @return True at or after the end; always false for an alert with no end.
     */
    fun isFinished(nowUptimeMillis: Long): Boolean {
        val end = endUptimeMillis ?: return false
        return nowUptimeMillis >= end
    }

    /**
     * Tells how much time is left.
     *
     * @param nowUptimeMillis The current uptime.
     * @return Milliseconds left, never below 0; null when there is no end.
     */
    fun remainingMillis(nowUptimeMillis: Long): Long? {
        val end = endUptimeMillis ?: return null
        return (end - nowUptimeMillis).coerceAtLeast(0L)
    }

    /**
     * Gives the moment a flip should happen. Even steps turn the light on,
     * odd steps turn it off.
     *
     * @param step The flip number, counted from 0 at the start.
     * @return The uptime at which that flip is due.
     */
    fun flipTime(step: Long): Long = startUptimeMillis + step * speed.halfPeriodMillis
}
