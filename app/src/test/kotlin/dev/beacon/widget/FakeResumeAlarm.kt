package dev.beacon.widget

/*
 * Why a shared fake: the coordinator's tests (and the view model's, which
 * pause through the coordinator) check which resume alarm was set or
 * cancelled, without a real AlarmManager.
 */

/** A resume alarm that only records what it was asked to do. */
internal class FakeResumeAlarm : ResumeAlarm {
    /** Every argument [schedule] received, in order; null means a cancel. */
    val scheduled: MutableList<Long?> = mutableListOf()

    /**
     * Records the request.
     *
     * @param atEpochMillis the requested wake-up time, or null for a cancel.
     */
    override fun schedule(atEpochMillis: Long?) {
        scheduled += atEpochMillis
    }
}
