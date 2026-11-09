package dev.beacon.alert

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * Why a controller owns the timing: the alert's start time and end live here,
 * not in a screen. If a screen is rebuilt (rotation, font change) it just
 * reads the running clock again, so the alert can't restart or run longer.
 *
 * Why one alert at a time: two overlapping alerts would fight over the camera
 * light and could push the flash rate past the safe limit (WCAG 2.3.1). A
 * second start while one runs is dropped, with one exception: a real alert
 * replaces a running test, because a message matters more than a button press.
 * The replacement runs stop() first, so the old blinker's off is queued on the
 * torch thread before the new blinker's start.
 *
 * Why the end and the renewal are posted with tokens: stop() must cancel both,
 * or an old end could cut a newer alert short, or a renewal could re-take the
 * wake lock after it was released.
 *
 * Why the wake lock is held 10 seconds past each period: the lock always has a
 * timeout so a bug can't keep the phone awake for good, and the margin makes
 * sure the hourly renewal runs before the old lock lapses.
 *
 * Why the heat callback is guarded: it is delivered through the main looper,
 * so it can arrive just after the alert ended or was replaced. It only acts if
 * the alert it was registered for is still the running one.
 */

/**
 * Runs one alert at a time and ends it on time.
 *
 * Use it from the main thread only: [start], [stop], [stopIf], the heat
 * callback and the automatic end all run on the main looper.
 *
 * @param torch The camera light.
 * @param torchLooper The thread that switches the light.
 * @param mainLooper The looper that runs the automatic end and the wake-lock renewal.
 * @param platform The wake lock, heat check, overlay permission and alert window.
 */
class AlertController(
    private val torch: TorchSwitch,
    private val torchLooper: Looper,
    mainLooper: Looper,
    private val platform: AlertPlatform,
) {
    private val mainHandler = Handler(mainLooper)
    private val mutableState = MutableStateFlow<AlertState>(AlertState.Idle)
    private var blinker: TorchBlinker? = null

    /** What the controller is doing now; screens observe this. */
    val state: StateFlow<AlertState> = mutableState.asStateFlow()

    /** Whether this phone has a usable camera light. */
    val hasLight: Boolean get() = torch.available

    /** The automatic end: posted on the main looper with [END_TOKEN] for the alert's end time, and calls [stop]. */
    private val endRunnable = Runnable { stop() }

    /** Renews the wake lock of an alert with no end, then posts the next renewal. Tagged [RENEW_TOKEN]. */
    private val renewRunnable: Runnable = object : Runnable {
        /** Re-holds the lock for another period and schedules the renewal after it. */
        override fun run() {
            // Only an alert with no end is renewed; stop() removes this, so this is a second guard.
            val running = mutableState.value as? AlertState.Running ?: return
            if (running.clock.endUptimeMillis != null) return
            platform.holdWakeLock(WAKE_LOCK_PERIOD_MILLIS + WAKE_LOCK_MARGIN_MILLIS)
            mainHandler.postAtTime(this, RENEW_TOKEN, SystemClock.uptimeMillis() + WAKE_LOCK_PERIOD_MILLIS)
        }
    }

    /**
     * Starts an alert. A real request replaces a running test; every other start while running is dropped.
     *
     * @param request What to flash and for how long.
     * @return True when it started; false when another alert is running (nothing changes).
     */
    fun start(request: AlertRequest): Boolean {
        val running = mutableState.value as? AlertState.Running
        if (running != null) {
            val replacesTest = request.kind == AlertKind.REAL && running.request.kind == AlertKind.TEST
            if (!replacesTest) return false
            // Ends the test completely (light off queued, lock released) before the new alert begins.
            stop()
        }
        val now = SystemClock.uptimeMillis()
        val clock = BlinkClock(now, request.speed, request.lengthMillis)
        // Wake lock first, so the CPU stays awake while everything else is set up.
        platform.holdWakeLock((request.lengthMillis ?: WAKE_LOCK_PERIOD_MILLIS) + WAKE_LOCK_MARGIN_MILLIS)
        if (request.lengthMillis == null) {
            mainHandler.postAtTime(renewRunnable, RENEW_TOKEN, now + WAKE_LOCK_PERIOD_MILLIS)
        }
        val newBlinker = TorchBlinker(torch, clock, torchLooper)
        blinker = newBlinker
        // False when there is no light, or the phone is already too hot: the screen blinks alone.
        val lightFlashing = !platform.isTooHot() && newBlinker.start()
        if (!lightFlashing) blinker = null
        mutableState.value = AlertState.Running(request, clock, lightFlashing)
        clock.endUptimeMillis?.let { mainHandler.postAtTime(endRunnable, END_TOKEN, it) }
        platform.watchHeat { onTooHot(newBlinker) }
        // A test never opens the alert window; without the permission Android would block it.
        if (request.kind == AlertKind.REAL && platform.canShowOverOtherApps()) platform.showAlertScreen()
        return true
    }

    /**
     * Stops the light but keeps the screen blinking, when the heat callback is for the running alert.
     *
     * @param forBlinker The blinker that was current when the heat watch began.
     */
    private fun onTooHot(forBlinker: TorchBlinker) {
        // A late callback for an alert that ended or was replaced must change nothing.
        if (blinker !== forBlinker) return
        val running = mutableState.value as? AlertState.Running ?: return
        forBlinker.stop()
        blinker = null
        mutableState.value = running.copy(lightFlashing = false)
    }

    /**
     * Ends the alert and leaves the light off. Safe to call when idle or more than once.
     */
    fun stop() {
        mainHandler.removeCallbacksAndMessages(END_TOKEN)
        mainHandler.removeCallbacksAndMessages(RENEW_TOKEN)
        blinker?.stop()
        blinker = null
        platform.stopWatchingHeat()
        platform.releaseWakeLock()
        mutableState.value = AlertState.Idle
    }

    /**
     * Ends the alert only if it is of the given kind.
     *
     * @param kind The kind to end; an alert of the other kind keeps running.
     */
    fun stopIf(kind: AlertKind) {
        val running = mutableState.value as? AlertState.Running ?: return
        if (running.request.kind == kind) stop()
    }

    private companion object {
        /** Tags the posted auto-end so [stop] can cancel it. */
        val END_TOKEN = Any()

        /** Tags the posted wake-lock renewal so [stop] can cancel it. */
        val RENEW_TOKEN = Any()
    }
}
