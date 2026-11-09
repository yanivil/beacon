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
