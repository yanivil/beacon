package dev.beacon.settings

import androidx.annotation.StringRes
import dev.beacon.R

/*
 * Why an enum and not a number: the only blink rates Beacon may ever use are
 * these three presets, all well under the WCAG 2.3.1 limit of 3 flashes a
 * second. Code that blinks takes a BlinkSpeed, so a raw rate can't sneak in.
 */

/**
 * One of the three blink-speed presets.
 *
 * @property blinksPerMinute blinks per minute; one blink is one on/off cycle.
 * @property nameRes string resource for the speed's visible name.
 * @throws IllegalArgumentException at class load if a preset exceeds 180 blinks
 *   a minute (3 flashes a second).
 */
enum class BlinkSpeed(val blinksPerMinute: Int, @param:StringRes val nameRes: Int) {
    SLOW(30, R.string.speed_slow),
    MEDIUM(60, R.string.speed_medium),
    FAST(90, R.string.speed_fast);

    /** How long the light stays on, and then off, in one blink. Rounded down. */
    val halfPeriodMillis: Long = 30_000L / blinksPerMinute

    init {
        // Safety net: 180 blinks a minute is exactly 3 flashes a second.
        require(blinksPerMinute <= 180) { "$name exceeds 3 flashes per second" }
    }
}
