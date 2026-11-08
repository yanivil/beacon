package dev.beacon.spike

/*
 * Why measure instead of trusting the timer: the safety rule is about what the light really did.
 * The peak is counted in a sliding one-second window, the same way WCAG 2.3.1 defines the limit,
 * so a late timer that bunches flashes together shows up even when the average looks fine.
 */

/**
 * Measures real on/off timing for a light or screen.
 *
 * Not thread-safe: use it from one thread.
 */
class FlashStats {
    private var lastChange = 0L
    private var gaps = 0L
    private var gapTotal = 0L
    private var gapMin = Long.MAX_VALUE
    private var gapMax = 0L
    private var flashes = 0L
    private var peakFlashesPerSecond = 0
    private val recentFlashes = ArrayDeque<Long>()

    /**
     * Records one change of state.
     *
     * @param nowMillis Monotonic time of the change, in milliseconds.
     * @param on True when the light or screen has just turned on.
     */
    fun record(nowMillis: Long, on: Boolean) {
        if (lastChange != 0L) {
            val gap = nowMillis - lastChange
            gaps++
            gapTotal += gap
            gapMin = minOf(gapMin, gap)
            gapMax = maxOf(gapMax, gap)
        }
        lastChange = nowMillis
        if (on) {
            flashes++
            recentFlashes.addLast(nowMillis)
            while (nowMillis - recentFlashes.first() >= 1000L) recentFlashes.removeFirst()
            peakFlashesPerSecond = maxOf(peakFlashesPerSecond, recentFlashes.size)
        }
    }

    /**
     * Summarises the timing so far.
     *
     * @return Space-separated key=value text for the log.
     */
    fun summary(): String {
        // Edge case: no gaps yet means the light never changed, for example camera in use.
        if (gaps == 0L) return "flashes=$flashes gaps=0"
        val meanGap = gapTotal.toDouble() / gaps
        val blinksPerMinute = 60_000.0 / (2 * meanGap)
        return "flashes=$flashes measuredBpm=${"%.1f".format(blinksPerMinute)} " +
            "gapMeanMs=${"%.1f".format(meanGap)} gapMinMs=$gapMin gapMaxMs=$gapMax " +
            "peakFlashesPerSecond=$peakFlashesPerSecond"
    }
}
