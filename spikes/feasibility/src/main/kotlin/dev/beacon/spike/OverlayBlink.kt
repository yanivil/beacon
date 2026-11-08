package dev.beacon.spike

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

/*
 * Why test an overlay as well as an activity: an overlay draws over other apps without interrupting
 * them, so if it works it is the gentler route when the phone is in use. It needs the "show over
 * other apps" permission. It lets touches pass through, so the person can keep using the phone.
 */

/** Blinks a full-screen window over other apps from the background. */
object OverlayBlink {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Shows the blinking overlay for a fixed time.
     *
     * @param context Any context.
     * @param preset The blink speed.
     * @param seconds How long to blink.
     */
    fun show(context: Context, preset: BlinkPreset, seconds: Int) {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) {
            SpikeLog.write(app, "overlay.noPermission")
            return
        }
        val windowManager = app.getSystemService(WindowManager::class.java)
        val view = View(app)
        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE,
        )
        try {
            windowManager.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            SpikeLog.write(app, "overlay.refused", "error=${e.javaClass.simpleName}")
            return
        }
        SpikeLog.write(app, "overlay.added", DeviceState.snapshot(app))

        val stats = FlashStats()
        val startUptime = SystemClock.uptimeMillis()
        var step = 0L
        var colourOn = false
        val tick = object : Runnable {
            /** Flips the overlay colour and schedules the next flip. */
            override fun run() {
                colourOn = !colourOn
                view.setBackgroundColor(if (colourOn) Palette.YELLOW else Color.BLACK)
                stats.record(SystemClock.elapsedRealtime(), colourOn)
                step++
                handler.postAtTime(this, startUptime + step * preset.halfPeriodMillis)
            }
        }
        tick.run()
        handler.postDelayed(
            {
                // The window can be added and still not be visible, for example on the lock screen.
                SpikeLog.write(
                    app,
                    "overlay.visibility",
                    "shown=${view.isShown} windowVisibility=${view.windowVisibility} " +
                        DeviceState.snapshot(app),
                )
            },
            1000L,
        )
        handler.postDelayed(
            {
                handler.removeCallbacks(tick)
                windowManager.removeView(view)
                SpikeLog.write(app, "overlay.finished", stats.summary())
            },
            seconds * 1000L,
        )
    }
}
