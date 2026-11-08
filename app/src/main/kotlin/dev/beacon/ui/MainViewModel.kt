package dev.beacon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.beacon.BeaconApp
import dev.beacon.alert.AlertController
import dev.beacon.alert.AlertState
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.ColourMode
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour
import dev.beacon.settings.Pause
import dev.beacon.settings.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
 * Why a view model between the screens and the stores: it outlives rotation
 * and font-size changes, so the settings stay loaded and a setter that was
 * started still finishes. The screens get plain values and plain functions,
 * and never touch DataStore, coroutines or an Android Context.
 *
 * Why failed saves are absorbed: DataStore throws an IOException when the
 * file can't be written (or read, which every write does first). An uncaught
 * exception in viewModelScope would crash the app, so a save that fails is
 * dropped and the screen keeps showing what is really saved. Only IOException
 * is caught; anything else is a bug and still surfaces.
 *
 * Why the settings start as null: DataStore reads the file in the
 * background. Showing defaults until then would flash the wrong state (for
 * example "ON" while alerts are paused), so the screen stays blank until the
 * first real value arrives.
 */

/**
 * Holds what Beacon's screens show and forwards their changes.
 *
 * @param repo the saved settings.
 * @param alerts the one alert controller.
 */
class MainViewModel(private val repo: SettingsRepository, private val alerts: AlertController) : ViewModel() {
    /** The saved settings, or null until the first read; read eagerly so they are ready as soon as possible. */
    val settings: StateFlow<BeaconSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** What the alert controller is doing now. */
    val alertState: StateFlow<AlertState> = alerts.state

    /** Whether this phone has a usable camera light. */
    val hasLight: Boolean = alerts.hasLight

    /**
     * Saves the pause state.
     *
     * @param pause the new pause.
     */
    fun setPause(pause: Pause) {
        save { repo.setPause(pause) }
    }

    /**
     * Turns alerts for one app on or off.
     *
     * @param source the message app.
     * @param enabled the new state.
     */
    fun setAppEnabled(source: MessageSource, enabled: Boolean) {
        save { repo.setAppEnabled(source, enabled) }
    }

    /**
     * Sets one app's own flash colour.
     *
     * @param source the message app.
     * @param colour the new colour.
     */
    fun setAppColour(source: MessageSource, colour: FlashColour) {
        save { repo.setAppColour(source, colour) }
    }

    /**
     * Chooses between one colour for all apps and a colour per app.
     *
     * @param mode the new mode.
     */
    fun setColourMode(mode: ColourMode) {
        save { repo.setColourMode(mode) }
    }

    /**
     * Sets the colour used when every app shares one.
     *
     * @param colour the new colour.
     */
    fun setGlobalColour(colour: FlashColour) {
        save { repo.setGlobalColour(colour) }
    }

    /**
     * Chooses how long an alert flashes.
     *
     * @param choice the new choice; the custom length is kept as it is.
     */
    fun setDuration(choice: DurationChoice) {
        save { repo.setDuration(choice) }
    }

    /**
     * Moves the custom length one step.
     *
     * @param longer true for the next longer step, false for the next shorter.
     */
    fun stepCustomLength(longer: Boolean) {
        save { repo.stepCustomLength(longer) }
    }

    /**
     * Sets the blink-speed preset.
     *
     * @param speed the new preset.
     */
    fun setSpeed(speed: BlinkSpeed) {
        save { repo.setSpeed(speed) }
    }

    /**
     * Runs one save in the background and absorbs a storage failure.
     *
     * @param write the repository call that saves the change.
     */
    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (e: IOException) {
                // The screen draws from settings, so a lost write just leaves the
                // saved value showing. No message and no log in PR 1 (privacy rule).
            }
        }
    }

    companion object {
        /**
         * Builds the view model from the app's single settings store and
         * alert controller. Creating it throws IllegalStateException when the
         * creation extras carry no application (an activity's always do).
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as BeaconApp
                MainViewModel(app.settingsRepository, app.alertController)
            }
        }
    }
}
