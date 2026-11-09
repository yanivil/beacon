package dev.beacon.settings

/*
 * Whether alerts are paused is stored as a deadline, not a countdown, so it
 * stays correct even if the app or widget is not running when time passes.
 */

/** Whether alerts are paused, and until when. */
sealed interface Pause {
    /** Alerts are on. */
    data object None : Pause

    /**
     * Alerts are paused until a moment in time.
     *
     * @property epochMillis the moment alerts resume, in epoch milliseconds.
     */
    data class Until(val epochMillis: Long) : Pause

    /** Alerts are paused until the user turns them back on. */
    data object Indefinite : Pause
}

/**
 * Tells whether this pause is in effect at a given moment.
 *
 * @param nowEpochMillis the moment to check, in epoch milliseconds.
 * @return true if alerts are paused then; a timed pause ends exactly at its deadline.
 */
fun Pause.isPausedAt(nowEpochMillis: Long): Boolean = when (this) {
    Pause.None -> false
    Pause.Indefinite -> true
    is Pause.Until -> nowEpochMillis < epochMillis
}

/** How long the widget's one-tap pause lasts: 1 hour, in milliseconds. */
const val WIDGET_PAUSE_MILLIS = 3_600_000L

/**
 * Gives the moment a timed pause ends.
 *
 * @return the deadline in epoch milliseconds for [Pause.Until]; null for any other pause, which has none.
 */
fun Pause.resumeAtMillis(): Long? = (this as? Pause.Until)?.epochMillis

/**
 * Tells whether this is a timed pause whose deadline has been reached.
 *
 * @param nowEpochMillis the moment to check, in epoch milliseconds.
 * @return true only for [Pause.Until] at or after its deadline; [Pause.None] and [Pause.Indefinite] never expire.
 */
fun Pause.isExpiredAt(nowEpochMillis: Long): Boolean = this is Pause.Until && nowEpochMillis >= epochMillis

/**
 * Works out the pause after a tap on the widget (spec section 5).
 *
 * @param current the stored pause.
 * @param nowEpochMillis the moment of the tap, in epoch milliseconds.
 * @return [Pause.None] if a pause is in effect, so the tap turns alerts on; otherwise a pause until
 *   [WIDGET_PAUSE_MILLIS] from now. An expired [Pause.Until] is no longer in effect, so it counts as ON.
 */
fun pauseAfterWidgetTap(current: Pause, nowEpochMillis: Long): Pause =
    if (current.isPausedAt(nowEpochMillis)) Pause.None else Pause.Until(nowEpochMillis + WIDGET_PAUSE_MILLIS)
