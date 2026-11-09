package dev.beacon.alert

/*
 * Why a fake: the wake lock, thermal service and activity start can't be
 * observed from the controller's unit tests. This one records every request.
 */

/**
 * An [AlertPlatform] that records calls instead of touching the phone.
 *
 * @param tooHot What [isTooHot] answers; a test may change it while an alert runs.
 * @param canShow What [canShowOverOtherApps] answers.
 * @param watchAccepted What [watchHeat] returns; false plays a system that refuses the listener.
 */
class FakeAlertPlatform(
    var tooHot: Boolean = false,
    private val canShow: Boolean = false,
    private val watchAccepted: Boolean = true,
) : AlertPlatform {
    private val locks = mutableListOf<Long>()
    private var heatCallback: (() -> Unit)? = null

    /** Every [holdWakeLock] timeout, in order. */
    val wakeLocks: List<Long> get() = locks

    /** True when the last wake-lock call was a release. */
    var released = false
        private set

    /** True while a heat watch is registered. */
    val watching: Boolean get() = heatCallback != null

    /** How many times [isTooHot] was asked. */
    var heatChecks = 0
        private set

    /** How many times [showAlertScreen] was called. */
    var screensShown = 0
        private set

    /** The currently watched callback, so a test can call it later, as a late delivery would. */
    fun captureHeatCallback(): () -> Unit = heatCallback!!

    /** Calls the watched heat callback, as the system would. */
    fun fireTooHot() = heatCallback?.invoke()

    override fun holdWakeLock(timeoutMillis: Long) { locks += timeoutMillis; released = false }
    override fun releaseWakeLock() { released = true }
    override fun isTooHot(): Boolean { heatChecks++; return tooHot }
    override fun watchHeat(onTooHot: () -> Unit): Boolean { heatCallback = onTooHot; return watchAccepted }
    override fun stopWatchingHeat() { heatCallback = null }
    override fun canShowOverOtherApps() = canShow
    override fun showAlertScreen() { screensShown++ }
}
