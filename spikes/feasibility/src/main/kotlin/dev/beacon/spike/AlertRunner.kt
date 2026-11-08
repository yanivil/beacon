package dev.beacon.spike

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

/*
 * Why a wake lock: with the screen off the CPU can sleep, and the blink timer stops with it. A
 * partial wake lock keeps the CPU running without turning the screen on. Its timeout is a safety
 * net, so a crash can't leave the phone awake and draining its battery.
 *
 * Why the once-a-minute tick: the 60-minute run has to record the battery drop and any overheating
 * with no cable attached, so the record has to be written as it goes.
 */

/** Runs one alert at a time: the camera light, the wake lock and the progress log. */
object AlertRunner {
    private const val TICK_MILLIS = 60_000L
    private const val WAKE_LOCK_MARGIN_MILLIS = 10_000L

    private val handler = Handler(Looper.getMainLooper())
    private var blinker: TorchBlinker? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    private var onFinished: (() -> Unit)? = null
    private var startedAtLog: String = ""

    /** True while an alert is running. */
    @get:Synchronized
    var isRunning = false
        private set

    /**
     * Starts an alert unless one is already running.
     *
     * @param context Any context.
     * @param preset The blink speed.
     * @param seconds How long to run.
     * @param torch True to blink the camera light. False only times the alert.
     * @param reason What caused this alert, for the log.
     * @param finished Called when the alert ends.
     * @return False when an alert was already running.
     */
    @Synchronized
    fun start(
        context: Context,
        preset: BlinkPreset,
        seconds: Int,
        torch: Boolean,
        reason: String,
        finished: () -> Unit = {},
    ): Boolean {
        if (isRunning) return false
        val app = context.applicationContext
        isRunning = true
        onFinished = finished
        startedAtLog = DeviceState.snapshot(app)
        SpikeLog.write(
            app,
            "alert.started",
            "reason=$reason preset=$preset seconds=$seconds torch=$torch $startedAtLog",
        )

        val power = app.getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "beaconspike:alert").apply {
            setReferenceCounted(false)
            acquire(seconds * 1000L + WAKE_LOCK_MARGIN_MILLIS)
        }
        thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
            SpikeLog.write(app, "thermal.changed", "status=$status ${DeviceState.snapshot(app)}")
        }.also { power.addThermalStatusListener(it) }

        if (torch) {
            blinker = TorchBlinker(app, preset).also { it.start() }
        }
        handler.postDelayed(tickRunnable(app), TICK_MILLIS)
        handler.postDelayed({ stop(app, "timer") }, seconds * 1000L)
        return true
    }

    /**
     * Stops the running alert and releases everything it holds. Safe to call when none is running.
     *
     * @param context Any context.
     * @param why What ended the alert, for the log.
     */
    @Synchronized
    fun stop(context: Context, why: String) {
        if (!isRunning) return
        val app = context.applicationContext
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        blinker?.stop()
        blinker = null
        thermalListener?.let {
            app.getSystemService(PowerManager::class.java).removeThermalStatusListener(it)
        }
        thermalListener = null
        SpikeLog.write(
            app,
            "alert.stopped",
            "why=$why startState=[$startedAtLog] endState=[${DeviceState.snapshot(app)}]",
        )
        wakeLock?.release()
        wakeLock = null
        onFinished?.invoke()
        onFinished = null
    }

    /**
     * Builds the repeating once-a-minute progress entry.
     *
     * @param app The application context.
     * @return A runnable that logs and reschedules itself.
     */
    private fun tickRunnable(app: Context): Runnable = object : Runnable {
        /** Logs the device state and the light's timing, then schedules the next entry. */
        override fun run() {
            SpikeLog.write(app, "alert.tick", DeviceState.snapshot(app))
            blinker?.logProgress()
            handler.postDelayed(this, TICK_MILLIS)
        }
    }
}
