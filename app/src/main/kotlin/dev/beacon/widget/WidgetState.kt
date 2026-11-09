package dev.beacon.widget

import dev.beacon.settings.Pause
import dev.beacon.settings.isPausedAt
import dev.beacon.settings.resumeAtMillis

/*
 * Why a separate state type: the widget draws from the clock, not from the
 * stored pause alone. A timed pause whose deadline has passed is ON, even if
 * nothing has cleaned the stored value up yet.
 */

/** What the widget shows. */
sealed interface WidgetState {
    /** Alerts are on. */
    data object On : WidgetState

    /**
     * Alerts are paused.
     *
     * @property resumeAtMillis when a timed pause ends, in epoch milliseconds; null for an open-ended pause.
     */
    data class Paused(val resumeAtMillis: Long?) : WidgetState
}

/**
 * Works out what the widget shows at a given moment.
 *
 * @param pause the stored pause.
 * @param nowEpochMillis the moment to check, in epoch milliseconds.
 * @return [WidgetState.On] if the pause is not in effect then, else [WidgetState.Paused] with the deadline, if any.
 */
fun widgetStateFor(pause: Pause, nowEpochMillis: Long): WidgetState =
    if (!pause.isPausedAt(nowEpochMillis)) WidgetState.On else WidgetState.Paused(pause.resumeAtMillis())
