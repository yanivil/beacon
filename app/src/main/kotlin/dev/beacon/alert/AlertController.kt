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
 * second start while one runs is dropped. (A later PR lets a real alert
 * replace a test.)
 *
 * Why the end is posted with a token: stop() must be able to cancel the
 * pending auto-end, or an old end could cut a newer alert short.
 */

/**
 * Runs one alert at a time and ends it on time.
 *
 * Use it from the main thread only: [start], [stop] and the automatic end all
 * run on the main looper.
 *
 * @param torch The camera light.
 * @param torchLooper The thread that switches the light.
 * @param mainLooper The looper that runs the automatic end.
 */
class AlertController(
    private val torch: TorchSwitch,
    private val torchLooper: Looper,
    mainLooper: Looper,
) {
    private val mainHandler = Handler(mainLooper)
    private val mutableState = MutableStateFlow<AlertState>(AlertState.Idle)
    private var blinker: TorchBlinker? = null

    /** What the controller is doing now; screens observe this. */
    val state: StateFlow<AlertState> = mutableState.asStateFlow()

    /** Whether this phone has a usable camera light. */
    val hasLight: Boolean get() = torch.available

    private val endRunnable = Runnable { stop() }

    /**
     * Starts an alert unless one is already running.
     *
     * @param request What to flash and for how long.
     * @return True when it started; false when another alert is running (nothing changes).
     */
    fun start(request: AlertRequest): Boolean {
        if (mutableState.value is AlertState.Running) return false
        val clock = BlinkClock(SystemClock.uptimeMillis(), request.speed, request.lengthMillis)
        val newBlinker = TorchBlinker(torch, clock, torchLooper)
        blinker = newBlinker
        // False when there is no light: the screen blinks alone.
        val lightFlashing = newBlinker.start()
        mutableState.value = AlertState.Running(request, clock, lightFlashing)
        clock.endUptimeMillis?.let { mainHandler.postAtTime(endRunnable, END_TOKEN, it) }
        return true
    }

    /**
     * Ends the alert and leaves the light off. Safe to call when idle or more than once.
     */
    fun stop() {
        mainHandler.removeCallbacksAndMessages(END_TOKEN)
        blinker?.stop()
        blinker = null
        mutableState.value = AlertState.Idle
    }

    private companion object {
        /** Tags the posted auto-end so [stop] can cancel it. */
        val END_TOKEN = Any()
    }
}
