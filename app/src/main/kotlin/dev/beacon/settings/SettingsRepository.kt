package dev.beacon.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.beacon.alert.MessageSource
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/*
 * Why a repository over DataStore: screens, the widget and the alert service
 * all need the same saved settings. They read one Flow and call small named
 * setters, so none of them touches storage keys. Each setter is a single
 * atomic edit, so a reader never sees a half-written change.
 */

/**
 * Saves and loads [BeaconSettings].
 *
 * @property dataStore the preferences store that holds the settings.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    /**
     * The current settings; emits again after every change.
     *
     * If the file can't be read (an IOException), it emits the defaults
     * instead of failing, so the screens still open (spec section 13).
     */
    val settings: Flow<BeaconSettings> = dataStore.data
        // Only read errors fall back; any other exception is a bug and is rethrown.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { SettingsCodec.decode(it) }

    /**
     * Saves the pause state.
     *
     * @param pause the new pause.
     * @throws IOException if the settings file can't be read or written.
     */
    suspend fun setPause(pause: Pause) {
        updatePause { pause }
    }

    /**
     * Changes the pause based on the stored one, in a single atomic edit.
     *
     * Reading and writing happen in the same edit, so two quick callers (for
     * example two widget taps) never both act on the same old value.
     *
     * @param transform maps the stored pause to the new one; it may run more than once, so keep it free of side effects.
     * @return the pause that was written.
     * @throws IOException if the settings file can't be read or written.
     */
    suspend fun updatePause(transform: (Pause) -> Pause): Pause {
        var result: Pause = Pause.None
        dataStore.edit { p ->
            // Same codec as `settings`, so the transform sees exactly what readers see.
            val pause = transform(SettingsCodec.decodePause(p))
            result = pause
            when (pause) {
                // No deadline to keep, so remove the stale one.
                Pause.None -> { p[SettingsCodec.PAUSE_KIND] = PauseKinds.NONE; p.remove(SettingsCodec.PAUSE_UNTIL) }
                Pause.Indefinite -> { p[SettingsCodec.PAUSE_KIND] = PauseKinds.INDEFINITE; p.remove(SettingsCodec.PAUSE_UNTIL) }
                // Both keys in one edit so the kind never appears without its time.
                is Pause.Until -> { p[SettingsCodec.PAUSE_KIND] = PauseKinds.UNTIL; p[SettingsCodec.PAUSE_UNTIL] = pause.epochMillis }
            }
        }
        return result
    }

    /**
     * Turns alerts for one app on or off.
     *
     * @param source the message app.
     * @param enabled the new state.
     */
    suspend fun setAppEnabled(source: MessageSource, enabled: Boolean) {
        dataStore.edit { it[SettingsCodec.appEnabled(source)] = enabled }
    }

    /**
     * Sets one app's own flash colour.
     *
     * @param source the message app.
     * @param colour the new colour.
     */
    suspend fun setAppColour(source: MessageSource, colour: FlashColour) {
        dataStore.edit { it[SettingsCodec.appColour(source)] = colour.name }
    }

    /**
     * Chooses between one colour for all apps and a colour per app.
     *
     * @param mode the new mode.
     */
    suspend fun setColourMode(mode: ColourMode) {
        dataStore.edit { it[SettingsCodec.COLOUR_MODE] = mode.name }
    }

    /**
     * Sets the colour used when every app shares one.
     *
     * @param colour the new colour.
     */
    suspend fun setGlobalColour(colour: FlashColour) {
        dataStore.edit { it[SettingsCodec.GLOBAL_COLOUR] = colour.name }
    }

    /**
     * Chooses how long an alert flashes.
     *
     * @param choice the new choice; the custom length is kept as it is.
     */
    suspend fun setDuration(choice: DurationChoice) {
        dataStore.edit { it[SettingsCodec.DURATION] = choice.name }
    }

    /**
     * Moves the custom length one step; does not change which duration is chosen.
     *
     * @param longer true for the next longer step, false for the next shorter.
     */
    suspend fun stepCustomLength(longer: Boolean) {
        dataStore.edit { p ->
            // Read inside the edit so two quick taps never act on the same old value.
            val current = p[SettingsCodec.CUSTOM_MINUTES]?.let { CustomLength.of(it) } ?: CustomLength.DEFAULT
            p[SettingsCodec.CUSTOM_MINUTES] = (if (longer) current.longer() else current.shorter()).minutes
        }
    }

    /**
     * Sets the blink-speed preset.
     *
     * @param speed the new preset.
     */
    suspend fun setSpeed(speed: BlinkSpeed) {
        dataStore.edit { it[SettingsCodec.SPEED] = speed.name }
    }

    companion object {
        /**
         * Opens the app's settings file.
         *
         * Call once per process: two DataStores on one file would fail, so the
         * application class holds the only instance.
         *
         * @param context any context; the application context is used.
         * @return a repository over `beacon_settings`.
         */
        fun create(context: Context): SettingsRepository = SettingsRepository(
            PreferenceDataStoreFactory.create(
                // A damaged file is replaced by defaults rather than crashing the app.
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            ) { context.applicationContext.preferencesDataStoreFile("beacon_settings") },
        )
    }
}
