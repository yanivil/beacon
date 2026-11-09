package dev.beacon

import android.app.Application
import android.content.res.Configuration
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
import dev.beacon.widget.AndroidResumeAlarm
import dev.beacon.widget.PauseCoordinator
import dev.beacon.widget.WidgetConfigKey
import dev.beacon.widget.WidgetRefresher
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** What the widget's drawing depends on outside the saved settings; set in [onCreate], when the resources exist. */
    private lateinit var widgetConfigState: MutableStateFlow<WidgetConfigKey>

    /**
     * The font size, density and languages the widget is fitted for, and the 12/24-hour setting and
     * time zone its resume time is shown in, as state its composition reads.
     */
    val widgetConfig: StateFlow<WidgetConfigKey> get() = widgetConfigState

    /** Reads the widget's configuration key, starts the light thread and runs the camera lookup on it. */
    override fun onCreate() {
        super.onCreate()
        widgetConfigState = MutableStateFlow(WidgetConfigKey.of(this, resources.configuration))
        torch = CameraTorchSwitch(this)
        torchThread.start()
        // Warm the lookup off the main thread (a PR 1 carry-over); later calls reuse the result.
        Handler(torchThread.looper).post { torch.available }
    }

    /**
     * Updates the widget's configuration key when the font size, density or language changes, and
     * then refreshes the widgets (ruling F6). The key is state the widget's composition reads, so a
     * Glance session that is still running draws again; the refresh starts one when none is running.
     * A rotation or any other change leaves the key as it is and starts nothing (ruling F3).
     *
     * @param newConfig the configuration now in force.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Unchanged for a rotation, a dark-mode switch and the like: no Glance session for those.
        if (!updateWidgetConfig(newConfig)) return
        appScope.launch {
            try {
                WidgetRefresher.refresh(this@BeaconApp)
            } catch (_: IOException) {
                // The widgets can't be refreshed now: the key change above still re-fits a running session,
                // and the next pause change refreshes them.
            }
        }
    }

    /**
     * Reads the widget's key again and stores it if it changed, so a Glance session that is still
     * running composes again (rulings F6 and R1). It refreshes nothing itself: each caller decides
     * whether a refresh is needed, so one change starts at most one Glance session. Called on the
     * main thread, by [onConfigurationChanged] and by the time-change receiver.
     *
     * @param config the configuration now in force; the app's current one by default, as when the
     *   12/24-hour setting or the time zone changed, which the configuration doesn't hold.
     * @return true if the key changed.
     */
    internal fun updateWidgetConfig(config: Configuration = resources.configuration): Boolean {
        val key = WidgetConfigKey.of(this, config)
        if (key == widgetConfigState.value) return false
        widgetConfigState.value = key
        return true
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

    /** The one place every pause change goes through: it saves, sets the resume alarm and refreshes the widget. */
    val pauseCoordinator: PauseCoordinator by lazy {
        PauseCoordinator(settingsRepository, AndroidResumeAlarm(this), refreshWidgets = { WidgetRefresher.refresh(this) })
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
