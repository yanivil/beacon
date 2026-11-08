package dev.beacon.spike

/*
 * Why an enum and not a number: the blink-safety rule allows three fixed speeds and no free entry.
 * Code that blinks takes this type, so a raw rate cannot reach the light or the screen.
 */

/**
 * The only blink speeds allowed. One blink is one on/off cycle.
 *
 * @property blinksPerMinute On/off cycles per minute.
 * @throws IllegalArgumentException If a preset would exceed 3 flashes per second (WCAG 2.3.1).
 */
enum class BlinkPreset(val blinksPerMinute: Int) {
    SLOW(30),
    MEDIUM(60),
    FAST(90);

    init {
        // 180 a minute is 3 flashes a second, the WCAG 2.3.1 ceiling.
        require(blinksPerMinute <= 180) { "$name exceeds 3 flashes per second" }
    }

    /** How long the light or screen stays in one state, in milliseconds. */
    val halfPeriodMillis: Long
        get() = 30_000L / blinksPerMinute

    companion object {
        /**
         * Reads a preset by name, for values passed in over adb.
         *
         * @param name Preset name, or null.
         * @return The matching preset. An unknown name falls back to [MEDIUM], never to a raw rate.
         */
        fun fromName(name: String?): BlinkPreset =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}
