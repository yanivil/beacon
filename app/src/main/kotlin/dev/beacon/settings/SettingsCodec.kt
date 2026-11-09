package dev.beacon.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.beacon.alert.MessageSource

/*
 * Why a separate codec: the key names and the "what if the stored value is
 * missing or unrecognised" rules live in one place, away from DataStore I/O,
 * so they can be tested on plain Preferences values. Enums are stored by
 * name, which survives reordering; an unknown name (for example from a newer
 * or damaged file) falls back to the default for that one field only.
 */

/** Reads [BeaconSettings] out of stored preferences and names every stored key. */
object SettingsCodec {
    /** Stored kind of pause: one of the [PauseKinds] names. */
    val PAUSE_KIND = stringPreferencesKey("pause_kind")

    /** Deadline in epoch milliseconds; only meaningful when the kind is UNTIL. */
    val PAUSE_UNTIL = longPreferencesKey("pause_until")

    /**
     * The key for whether alerts are on for one app.
     *
     * @param source the message app.
     * @return the boolean key `app_<SOURCE>_enabled`.
     */
    fun appEnabled(source: MessageSource) = booleanPreferencesKey("app_${source.name}_enabled")

    /**
     * The key for one app's flash colour name.
     *
     * @param source the message app.
     * @return the string key `app_<SOURCE>_colour`.
     */
    fun appColour(source: MessageSource) = stringPreferencesKey("app_${source.name}_colour")

    /** Stored [ColourMode] name. */
    val COLOUR_MODE = stringPreferencesKey("colour_mode")

    /** Stored global [FlashColour] name. */
    val GLOBAL_COLOUR = stringPreferencesKey("global_colour")

    /** Stored [DurationChoice] name. */
    val DURATION = stringPreferencesKey("duration")

    /** Stored custom length in minutes. */
    val CUSTOM_MINUTES = intPreferencesKey("custom_minutes")

    /** Stored [BlinkSpeed] name. */
    val SPEED = stringPreferencesKey("speed")

    /**
     * Builds settings from stored preferences.
     *
     * @param prefs the stored preferences; may be empty.
     * @return the settings, with the default for every missing or unrecognised field.
     */
    fun decode(prefs: Preferences): BeaconSettings {
        val d = BeaconSettings.DEFAULT
        return BeaconSettings(
            pause = decodePause(prefs),
            // Field by field, so one missing key keeps that app's own default.
            apps = MessageSource.entries.associateWith { source ->
                AppSetting(
                    enabled = prefs[appEnabled(source)] ?: source.defaultEnabled,
                    colour = enumOrNull<FlashColour>(prefs[appColour(source)]) ?: source.defaultColour,
                )
            },
            colourMode = enumOrNull<ColourMode>(prefs[COLOUR_MODE]) ?: d.colourMode,
            globalColour = enumOrNull<FlashColour>(prefs[GLOBAL_COLOUR]) ?: d.globalColour,
            duration = enumOrNull<DurationChoice>(prefs[DURATION]) ?: d.duration,
            // CustomLength.of snaps anything that is not a step to the default.
            customLength = prefs[CUSTOM_MINUTES]?.let { CustomLength.of(it) } ?: d.customLength,
            speed = enumOrNull<BlinkSpeed>(prefs[SPEED]) ?: d.speed,
        )
    }

    /**
     * Reads the pause from its two keys.
     *
     * @param prefs the stored preferences.
     * @return the pause; [Pause.None] if the kind is unknown or UNTIL has no deadline.
     */
    internal fun decodePause(prefs: Preferences): Pause = when (prefs[PAUSE_KIND]) {
        PauseKinds.INDEFINITE -> Pause.Indefinite
        // A timed pause without a deadline can't be honoured; treat as not paused.
        PauseKinds.UNTIL -> prefs[PAUSE_UNTIL]?.let { Pause.Until(it) } ?: Pause.None
        else -> Pause.None
    }

    /**
     * Finds an enum constant by name.
     *
     * @param name the stored name, or null.
     * @return the constant, or null if [name] is null or not a constant.
     */
    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }
}
