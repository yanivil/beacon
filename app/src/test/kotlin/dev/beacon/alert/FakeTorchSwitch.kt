package dev.beacon.alert

import android.os.SystemClock

/*
 * Why a fake: the real camera light can't be driven in a unit test. This one
 * records every command with the time it arrived, so tests can check both the
 * order and the timing of the blinks. Task 6's controller tests reuse it.
 */

/**
 * A [TorchSwitch] that records commands instead of lighting anything.
 *
 * @param available Whether the pretend phone has a camera light.
 * @param refuseFrom The 1-based call number from which [set] returns false.
 * @param onCall Called after each command is recorded, with its 1-based number; a test can use it to stall time.
 * @property events Every call as (uptime in ms, requested on/off), in order.
 */
class FakeTorchSwitch(
    override val available: Boolean = true,
    private val refuseFrom: Int = Int.MAX_VALUE,
    private val onCall: (Int) -> Unit = {},
) : TorchSwitch {
    private val recorded = mutableListOf<Pair<Long, Boolean>>()

    /** Every call received so far, as (uptime in ms, requested on/off). */
    val events: List<Pair<Long, Boolean>> get() = recorded

    /**
     * Records the call and answers like a light that may refuse.
     *
     * @param on True to switch on.
     * @return False from call number [refuseFrom] on.
     */
    override fun set(on: Boolean): Boolean {
        recorded += SystemClock.uptimeMillis() to on
        onCall(recorded.size)
        return recorded.size < refuseFrom
    }
}
