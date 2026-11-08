package dev.beacon.spike

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock

/*
 * Why a separate thread: setTorchMode can stall inside the camera service, and that must not delay
 * the screen blink on the main thread.
 *
 * Why the torch callback: it reports what the system says the light did, which is better evidence
 * than counting the commands sent. It also reports when another app takes the camera, which is the
 * likely failure during a video call.
 */

/**
 * Blinks the camera light at a safe preset until stopped.
 *
 * @param context Any context; only the application context is kept.
 * @param preset The blink speed.
 */
class TorchBlinker(context: Context, private val preset: BlinkPreset) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("torch-blink")
    private lateinit var handler: Handler
    private var cameraId: String? = null
    private var running = false
    private var lightOn = false
    private var startUptime = 0L
    private var step = 0L
    private var commandErrors = 0
    private var unavailableEvents = 0
    private val stats = FlashStats()

    private val torchCallback = object : CameraManager.TorchCallback() {
        /**
         * Records a light change the system reports.
         *
         * @param id The camera that changed.
         * @param enabled True when the light is now on.
         */
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            // The callback also fires once on registration, before the test starts.
            if (running && id == cameraId) stats.record(SystemClock.elapsedRealtime(), enabled)
        }

        /**
         * Records that the light cannot be used, usually because an app has the camera open.
         *
         * @param id The camera that became unavailable.
         */
        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) {
                unavailableEvents++
                SpikeLog.write(appContext, "torch.unavailable", DeviceState.snapshot(appContext))
            }
        }
    }

    private val tick = object : Runnable {
        /** Flips the light and schedules the next flip. */
        override fun run() {
            if (!running) return
            setTorch(!lightOn)
            step++
            // Timed from the start, not from "now", so delays can't add up over a 60-minute run.
            handler.postAtTime(this, startUptime + step * preset.halfPeriodMillis)
        }
    }

    /**
     * Starts blinking.
     *
     * @return False when this phone has no usable camera light.
     */
    fun start(): Boolean {
        cameraId = findFlashCamera()
        if (cameraId == null) {
            SpikeLog.write(appContext, "torch.noFlashCamera")
            return false
        }
        thread.start()
        handler = Handler(thread.looper)
        cameraManager.registerTorchCallback(torchCallback, handler)
        handler.post {
            running = true
            startUptime = SystemClock.uptimeMillis()
            tick.run()
        }
        SpikeLog.write(appContext, "torch.started", "preset=$preset camera=$cameraId")
        return true
    }

    /** Writes the timing measured so far to the log. */
    fun logProgress() {
        handler.post { SpikeLog.write(appContext, "torch.progress", progress()) }
    }

    /** Stops blinking, always leaves the light off, and writes the final timing to the log. */
    fun stop() {
        handler.post {
            running = false
            handler.removeCallbacks(tick)
            setTorch(false)
            cameraManager.unregisterTorchCallback(torchCallback)
            SpikeLog.write(appContext, "torch.stopped", "preset=$preset ${progress()}")
            thread.quitSafely()
        }
    }

    /**
     * Describes the run so far.
     *
     * @return Space-separated key=value text for the log.
     */
    private fun progress(): String =
        "${stats.summary()} commandErrors=$commandErrors unavailable=$unavailableEvents"

    /**
     * Turns the light on or off and records any refusal.
     *
     * @param on True to turn the light on.
     */
    private fun setTorch(on: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, on)
            lightOn = on
        } catch (e: CameraAccessException) {
            commandErrors++
            // Only the first few: a camera held open would otherwise fill the log on every tick.
            if (commandErrors <= 5) {
                SpikeLog.write(appContext, "torch.commandError", "reason=${e.reason}")
            }
        } catch (e: IllegalArgumentException) {
            commandErrors++
            if (commandErrors <= 5) {
                SpikeLog.write(appContext, "torch.commandError", "reason=${e.message}")
            }
        }
    }

    /**
     * Finds the camera with the light, preferring the back camera.
     *
     * @return The camera id, or null if no camera has a light.
     */
    private fun findFlashCamera(): String? {
        val withFlash = cameraManager.cameraIdList.filter { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        return withFlash.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: withFlash.firstOrNull()
    }
}
