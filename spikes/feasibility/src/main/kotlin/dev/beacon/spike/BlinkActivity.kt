package dev.beacon.spike

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout

/*
 * Why an activity: a normal window drawn over other apps isn't expected to appear above the lock
 * screen, while an activity marked showWhenLocked is. Step 1 tests both routes. This one logs each
 * lifecycle step, so "was it really shown?" is answered by evidence: if Android blocks the launch,
 * the log simply has no blink.created line.
 */

/** Blinks the whole screen at a safe preset, on top of other apps and the lock screen. */
class BlinkActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val stats = FlashStats()
    private lateinit var root: FrameLayout
    private lateinit var preset: BlinkPreset
    private var route = ""
    private var colourOn = false
    private var startUptime = 0L
    private var step = 0L

    private val tick = object : Runnable {
        /** Flips the screen colour and schedules the next flip. */
        override fun run() {
            colourOn = !colourOn
            root.setBackgroundColor(if (colourOn) Palette.YELLOW else Color.BLACK)
            stats.record(SystemClock.elapsedRealtime(), colourOn)
            step++
            handler.postAtTime(this, startUptime + step * preset.halfPeriodMillis)
        }
    }

    /**
     * Builds the screen and starts blinking.
     *
     * @param savedInstanceState Unused.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preset = BlinkPreset.fromName(intent.getStringExtra(EXTRA_PRESET))
        route = intent.getStringExtra(EXTRA_ROUTE).orEmpty()
        SpikeLog.write(this, "blink.created", "route=$route ${DeviceState.snapshot(this)}")

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val stop = Button(this).apply {
            setText(R.string.button_stop)
            isAllCaps = false
            textSize = 32f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            minHeight = (96 * resources.displayMetrics.density).toInt()
            setOnClickListener { finishBlink("stopButton") }
        }
        root = FrameLayout(this).apply {
            addView(
                stop,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM,
                ),
            )
        }
        setContentView(root)
        // insetsController is API 30+. On API 29 the system bars simply stay visible.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        }

        startUptime = SystemClock.uptimeMillis()
        tick.run()
        handler.postDelayed(
            { finishBlink("timer") },
            intent.getIntExtra(EXTRA_SECONDS, 10) * 1000L,
        )
    }

    /** Records that the screen is really in front of the user. */
    override fun onResume() {
        super.onResume()
        SpikeLog.write(this, "blink.resumed", "route=$route ${DeviceState.snapshot(this)}")
    }

    /**
     * Records whether the window has focus, which separates "launched" from "actually on top".
     *
     * @param hasFocus True when this window has focus.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SpikeLog.write(this, "blink.focus", "route=$route hasFocus=$hasFocus")
    }

    /**
     * Stops the blink, stops the alert and leaves no trace on the back stack.
     *
     * @param why What ended the blink, for the log.
     */
    private fun finishBlink(why: String) {
        handler.removeCallbacksAndMessages(null)
        SpikeLog.write(this, "blink.finished", "route=$route why=$why ${stats.summary()}")
        getSystemService(NotificationManager::class.java).cancel(Notifications.ID_ALERT)
        if (why == "stopButton") AlertRunner.stop(this, "stopButton")
        finishAndRemoveTask()
    }

    companion object {
        private const val EXTRA_PRESET = "preset"
        private const val EXTRA_SECONDS = "seconds"
        private const val EXTRA_ROUTE = "route"

        /**
         * Builds the intent that opens this screen.
         *
         * @param context Any context.
         * @param preset The blink speed.
         * @param seconds How long to blink.
         * @param route Which route launched it, for the log.
         * @return The intent, safe to start from outside an activity.
         */
        fun intent(context: Context, preset: BlinkPreset, seconds: Int, route: Route): Intent =
            Intent(context, BlinkActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_PRESET, preset.name)
                .putExtra(EXTRA_SECONDS, seconds)
                .putExtra(EXTRA_ROUTE, route.name)
    }
}
