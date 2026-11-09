package dev.beacon.widget

import dev.beacon.settings.Pause
import dev.beacon.settings.SettingsRepository
import dev.beacon.settings.isExpiredAt
import dev.beacon.settings.pauseAfterWidgetTap
import dev.beacon.settings.resumeAtMillis
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/*
 * Why one coordinator (ruling 5): a pause change has three parts, the stored
 * pause, the resume alarm and the widget, and they must agree. Home, the
 * widget, the alarm, Beacon coming to the front and the screen turning on
 * all change the pause, so they all come through here, and each change ends
 * the same way: save, set or cancel the alarm for the saved pause, and
 * refresh the widget if the saved pause changed.
 *
 * A started change always finishes (ruling F1): Home's buttons run in the
 * screen's scope, which is cancelled the moment the person leaves Beacon.
 * Stopping half-way would leave the saved pause, the alarm and the widget
 * disagreeing until the next catch-up, so the change runs as non-cancellable.
 *
 * The widget is refreshed only when the saved pause changed (ruling F3): a
 * refresh starts a Glance session of about 45 seconds under a wake lock, and
 * every screen-on and every return to Beacon catches up. The alarm is always
 * set again, which also re-arms one lost to a restart.
 *
 * One exception (follow-up R1): after the clock, the time zone, the
 * 12/24-hour setting or the language changed, what the widget shows can be
 * out of date with the pause unchanged. A language change changes the words
 * of every state, so it always redraws. A time change redraws only while a
 * timed pause is running, because only then does the widget show a time:
 * automatic time sends TIME_SET whenever it corrects the clock, and every
 * refresh is a Glance session of about 45 seconds. Either way it is one
 * refresh at most.
 *
 * Threads: every function is a suspend function and safe from any thread.
 * DataStore makes each read-and-write atomic; the lock also keeps the alarm
 * in the order of the writes, so two quick changes can't leave it set for the
 * older one. The widget refresh needs no lock: the widget draws what is
 * stored, whichever refresh runs last.
 */

/**
 * Makes every pause change: saves it, sets the resume alarm for it and, when it changed, refreshes the widget.
 * After a time, time-zone or language change it also refreshes the widget when what it shows depends on it.
 *
 * @param repo the saved settings.
 * @param alarm the resume alarm, set for a timed pause and cancelled for any other.
 * @param refreshWidgets redraws every placed widget.
 * @param now the clock, in epoch milliseconds.
 */
class PauseCoordinator(
    private val repo: SettingsRepository,
    private val alarm: ResumeAlarm,
    private val refreshWidgets: suspend () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Orders the save and the alarm of one change before the next change's. */
    private val lock = Mutex()

    /**
     * Sets the pause, as Home's buttons do.
     *
     * @param pause the new pause.
     * @throws java.io.IOException if the settings can't be read or saved, and then the alarm and the widget
     *   are left as they were; or if the widget refresh fails, after the pause was saved and the alarm set.
     */
    suspend fun set(pause: Pause) {
        change({ pause })
    }

    /**
     * Applies a widget tap: pauses for 1 hour while ON, turns alerts ON while paused (spec section 5).
     *
     * @return the pause that was saved.
     * @throws java.io.IOException if the settings can't be read or saved, and then the alarm and the widget
     *   are left as they were; or if the widget refresh fails, after the pause was saved and the alarm set.
     */
    suspend fun tapFromWidget(): Pause = change({ pauseAfterWidgetTap(it, now()) })

    /**
     * Cleans up a timed pause whose deadline has passed: it becomes ON. Any other pause is kept, and
     * the alarm is set again for a timed pause that is still running.
     *
     * @throws java.io.IOException if the settings can't be read or saved, and then the alarm and the widget
     *   are left as they were; or if the widget refresh fails, after the pause was saved and the alarm set.
     */
    suspend fun catchUp() {
        change(::endIfExpired)
    }

    /**
     * Catches up after the clock, the time zone, the 12/24-hour setting or the language changed. It
     * cleans up an ended timed pause as [catchUp] does, and refreshes the widgets at most once: when
     * the saved pause changed, after a language change (every state shows words), or while a timed
     * pause is still running (only then does the widget show a time).
     *
     * @param languageChanged true after a language change; false after a change of the clock, the time
     *   zone or the 12/24-hour setting.
     * @throws java.io.IOException if the settings can't be read or saved, and then the alarm and the widget
     *   are left as they were; or if the widget refresh fails, after the pause was saved and the alarm set.
     */
    suspend fun catchUpAfterTimeChange(languageChanged: Boolean) {
        // A timed pause left after the clean-up is still running: an ended one has just become ON.
        change(::endIfExpired, redrawAnyway = { saved -> languageChanged || saved is Pause.Until })
    }

    /**
     * The catch-up's rule: a timed pause whose deadline has passed becomes ON, and any other pause is kept.
     *
     * @param stored the stored pause.
     * @return ON for a timed pause that has ended, else [stored].
     */
    private fun endIfExpired(stored: Pause): Pause = if (stored.isExpiredAt(now())) Pause.None else stored

    /**
     * Saves the pause [transform] gives and sets or cancels the alarm for it, then refreshes the
     * widgets once if the saved pause differs from the one stored before or [redrawAnyway] holds for
     * it. Runs to the end even if the caller is cancelled once it has started.
     *
     * @param transform maps the stored pause to the new one, inside one atomic edit; it may run more than once.
     * @param redrawAnyway given the saved pause, whether the widgets need redrawing although it didn't
     *   change; never, by default (ruling F3).
     * @return the pause that was saved.
     * @throws java.io.IOException if the settings can't be read or saved, and then the alarm and the widget
     *   are left as they were; or if the widget refresh fails, after the pause was saved and the alarm set.
     */
    private suspend fun change(
        transform: (Pause) -> Pause,
        redrawAnyway: (saved: Pause) -> Boolean = { false },
    ): Pause = withContext(NonCancellable) {
        // The pause the committed edit started from. Each run of the transform overwrites it, so the
        // value left is the last run's, the one DataStore wrote: the same rule updatePause's result follows.
        var before: Pause = Pause.None
        val saved = lock.withLock {
            // If the save fails, the IOException leaves here, before the alarm or the widget is touched.
            repo.updatePause { stored -> before = stored; transform(stored) }
                // Always set again, even when unchanged: this re-arms an alarm lost to a restart.
                .also { alarm.schedule(it.resumeAtMillis()) }
        }
        // Unchanged (a catch-up with nothing to clean up): the widget already shows it, so no Glance session,
        // unless the caller knows that what it shows is out of date (follow-up R1). One refresh either way.
        if (saved != before || redrawAnyway(saved)) refreshWidgets()
        saved
    }
}
