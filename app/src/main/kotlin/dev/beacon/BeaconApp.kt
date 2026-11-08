package dev.beacon

import android.app.Application
import android.os.HandlerThread
import android.os.Looper
import dev.beacon.alert.AlertController
import dev.beacon.alert.CameraTorchSwitch
import dev.beacon.settings.SettingsRepository

/**
 * Application class for Beacon.
 *
 * Why it exists: app-wide singletons (settings store, later alert state) hang
 * off this class so every screen and service shares them.
 */
class BeaconApp : Application() {
    /** Background thread that switches the camera light; lives as long as the process, so never quit. */
    private val torchThread = HandlerThread("beacon-torch")

    /** Starts the light thread. */
    override fun onCreate() {
        super.onCreate()
        torchThread.start()
    }

    /** The one alert controller; created on first use, after [onCreate] has started the light thread. */
    val alertController: AlertController by lazy {
        AlertController(CameraTorchSwitch(this), torchThread.looper, Looper.getMainLooper())
    }

    /** The saved settings; created on first use, once per process (one DataStore per file). */
    val settingsRepository: SettingsRepository by lazy { SettingsRepository.create(this) }
}
