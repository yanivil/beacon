package dev.beacon.alert

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/*
 * Why an interface: the blinker only needs "is there a light" and "set it".
 * Hiding the camera behind it lets tests use a fake, and keeps the Android
 * camera calls and their exceptions in one place.
 */

/** A camera light that can be switched on and off. */
interface TorchSwitch {
    /** True when this phone has a camera light Beacon can use. */
    val available: Boolean

    /**
     * Switches the light.
     *
     * @param on True for on, false for off.
     * @return False when the light refused (for example another app holds the camera).
     */
    fun set(on: Boolean): Boolean
}

/**
 * The real camera light, driven through [CameraManager.setTorchMode].
 *
 * @param context Any context; only the application context is kept.
 */
class CameraTorchSwitch(context: Context) : TorchSwitch {
    // Null on a phone without a camera service: then there is no light, and no crash.
    private val cameraManager: CameraManager? = context.applicationContext.getSystemService(CameraManager::class.java)

    // Looked up once, on first use of [available] or [set] (BeaconApp makes that first use
    // on the torch thread, so the slow camera query never runs on the main thread).
    // Synchronized so two threads asking at once still run the lookup only once.
    private val cameraId: String? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { findFlashCamera() }

    override val available: Boolean get() = cameraId != null

    /**
     * Switches the camera light.
     *
     * @param on True for on, false for off.
     * @return False when the system refused the command or there is no light.
     */
    override fun set(on: Boolean): Boolean {
        val id = cameraId ?: return false
        // A camera id is only found when the manager exists, so this never returns here in practice.
        val manager = cameraManager ?: return false
        return try {
            manager.setTorchMode(id, on)
            true
        } catch (e: CameraAccessException) {
            false
        } catch (e: IllegalArgumentException) {
            // The camera id is no longer valid, for example the camera was removed.
            false
        }
    }

    /**
     * Finds the camera with the light, preferring the back camera.
     *
     * @return The camera id, or null if there is no camera service or no camera has a light.
     */
    private fun findFlashCamera(): String? {
        // No camera service at all: no light.
        val manager = cameraManager ?: return null
        // The camera list can throw when the camera service is down; treat that as "no light".
        val withFlash = try {
            manager.cameraIdList.filter { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            return null
        }
        return withFlash.firstOrNull { id ->
            try {
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } catch (e: CameraAccessException) {
                false
            }
        } ?: withFlash.firstOrNull()
    }
}
