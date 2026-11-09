package dev.beacon.widget

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/*
 * Why this rule: opening Beacon or connecting the listener catches the pause
 * up, which can refresh the widgets through Glance. Glance keeps process-wide
 * state, and a Robolectric test that ends while an update is still running
 * can leave the next real update waiting forever. So every test class that
 * launches MainActivity or connects BeaconListener applies this rule, and
 * BeaconAppTest and TimeChangeReceiverTest apply it before swapping in
 * their own counter, so the real refresh is put back afterwards.
 * ResumeAlarmTest keeps the real refresh.
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
