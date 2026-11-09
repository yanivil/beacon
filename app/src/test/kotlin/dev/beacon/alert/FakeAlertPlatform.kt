package dev.beacon.alert

/*
 * Why a fake: the wake lock, thermal service and activity start can't be
 * observed from the controller's unit tests. This one records every request.
 */

/**
 * An [AlertPlatform] that records calls instead of touching the phone.
 *
 * @param tooHot What [isTooHot] answers.
 * @param canShow What [canShowOverOtherApps] answers.
 */
class FakeAlertPlatform(private val tooHot: Boolean = false, private val canShow: Boolean = false) : AlertPlatform {
    private val locks = mutableListOf<Long>()
    private var heatCallback: (() -> Unit)? = null

    /** Every [holdWakeLock] timeout, in order. */
    val wakeLocks: List<Long> get() = locks

    /** True when the last wake-lock call was a release. */
    var released = false
        private set

    /** True while a heat watch is registered. */
    val watching: Boolean get() = heatCallback != null

    /** How many times [showAlertScreen] was called. */
    var screensShown = 0
        private set

    /** The currently watched callback, so a test can call it later, as a late delivery would. */
    fun captureHeatCallback(): () -> Unit = heatCallback!!

    /** Calls the watched heat callback, as the system would. */
    fun fireTooHot() = heatCallback?.invoke()

    override fun holdWakeLock(timeoutMillis: Long) { locks += timeoutMillis; released = false }
    override fun releaseWakeLock() { released = true }
    override fun isTooHot() = tooHot
    override fun watchHeat(onTooHot: () -> Unit) { heatCallback = onTooHot }
    override fun stopWatchingHeat() { heatCallback = null }
    override fun canShowOverOtherApps() = canShow
    override fun showAlertScreen() { screensShown++ }
}
