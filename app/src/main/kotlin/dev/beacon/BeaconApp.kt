package dev.beacon

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import dev.beacon.alert.AlertController
import dev.beacon.alert.AndroidAlertPlatform
import dev.beacon.alert.CameraTorchSwitch
import dev.beacon.alert.MessageWatcher
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Application class for Beacon.
 *
 * Why it exists: app-wide singletons (settings store, later alert state) hang
 * off this class so every screen and service shares them.
 */
class BeaconApp : Application() {
    /** Background thread that switches the camera light; lives as long as the process, so never quit. */
    private val torchThread = HandlerThread("beacon-torch")

    /** The camera light; built in [onCreate] because the context isn't attached yet at construction. */
    private lateinit var torch: CameraTorchSwitch

    /** Starts the light thread and runs the camera lookup on it. */
    override fun onCreate() {
        super.onCreate()
        torch = CameraTorchSwitch(this)
        torchThread.start()
        // Warm the lookup off the main thread (a PR 1 carry-over); later calls reuse the result.
        Handler(torchThread.looper).post { torch.available }
    }

    /** The one alert controller; created on first use, after [onCreate] has started the light thread. */
    val alertController: AlertController by lazy {
        AlertController(torch, torchThread.looper, Looper.getMainLooper(), AndroidAlertPlatform(this))
    }

    /** The saved settings; created on first use, once per process (one DataStore per file). */
    val settingsRepository: SettingsRepository by lazy { SettingsRepository.create(this) }

    /** App-wide scope on the main thread; lives as long as the process. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The saved settings as a state; null until the first load finishes, so nothing decides with defaults. */
    val settingsState: StateFlow<BeaconSettings?> by lazy {
        settingsRepository.settings.stateIn(appScope, SharingStarted.Eagerly, null)
    }

    /** Applies the message rules with the saved settings; used by the notification listener. */
    val messageWatcher: MessageWatcher by lazy {
        MessageWatcher(
            settingsState, alertController, appScope,
            // Read on every call: the user can change the default SMS app at any time.
            defaultSmsPackage = { Telephony.Sms.getDefaultSmsPackage(this) },
            // The only log in the app: the decision alone, and only in debug builds.
            log = { if (BuildConfig.DEBUG) Log.d("Beacon", it.toString()) },
        )
    }
}
