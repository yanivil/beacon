package dev.beacon.widget

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/*
 * Why this rule: opening Beacon (or connecting the listener) now catches the
 * pause up, which refreshes the widgets through Glance. Glance keeps
 * process-wide state, and a Robolectric test that ends while an update is
 * still running can leave the next real update waiting forever. Tests that
 * only need the screen skip the refresh; ResumeAlarmTest keeps the real one.
 */

/** Replaces the widget refresh with a no-op for one test, then restores it. */
class NoWidgetRefreshRule : TestWatcher() {
    /** The refresh that was in place before the test. */
    private var saved: (suspend (android.content.Context) -> Unit)? = null

    /**
     * Swaps in the no-op.
     *
     * @param description the test, unused.
     */
    override fun starting(description: Description) {
        saved = WidgetRefresher.update
        WidgetRefresher.update = {}
    }

    /**
     * Puts the real refresh back, even when the test failed.
     *
     * @param description the test, unused.
     */
    override fun finished(description: Description) {
        saved?.let { WidgetRefresher.update = it }
    }
}
