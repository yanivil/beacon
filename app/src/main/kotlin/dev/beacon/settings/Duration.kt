package dev.beacon.settings

/*
 * "How long to flash" has four choices. Custom uses a fixed ladder of steps
 * instead of free entry, so no slider or number field is needed.
 */

/** The four choices for how long an alert flashes. */
enum class DurationChoice { ONE_MINUTE, FIVE_MINUTES, CUSTOM, UNTIL_STOPPED }

/**
 * A custom alert length, always one of [STEPS] minutes.
 *
 * @property minutes the length in minutes; only reachable through [of] or
 *   [shorter]/[longer], so it is always a valid step.
 */
@JvmInline
value class CustomLength private constructor(val minutes: Int) {
    /** True when this is the smallest step. */
    val isShortest: Boolean get() = minutes == STEPS.first()

    /** True when this is the largest step. */
    val isLongest: Boolean get() = minutes == STEPS.last()

    /**
     * The next shorter step.
     *
     * @return one step down, or this length if already the shortest.
     */
    fun shorter(): CustomLength = CustomLength(STEPS[(STEPS.indexOf(minutes) - 1).coerceAtLeast(0)])

    /**
     * The next longer step.
     *
     * @return one step up, or this length if already the longest.
     */
    fun longer(): CustomLength =
        CustomLength(STEPS[(STEPS.indexOf(minutes) + 1).coerceAtMost(STEPS.lastIndex)])

    companion object {
        /** The allowed lengths in minutes, shortest first. */
        val STEPS = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)

        /** The length used when nothing valid is stored: 10 minutes. */
        val DEFAULT = CustomLength(10)

        /**
         * Builds a length from a stored value.
         *
         * @param minutes the stored minutes.
         * @return that length if it is a step, otherwise [DEFAULT].
         */
        fun of(minutes: Int): CustomLength =
            // Unknown or corrupted stored values fall back to the default.
            if (minutes in STEPS) CustomLength(minutes) else DEFAULT
    }
}
