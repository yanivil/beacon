package dev.beacon.settings

/*
 * Why one place for these names: the repository writes the kind of pause and
 * the codec reads it back. If the two spelled a name differently, a saved
 * pause would read back as "not paused" without any error. Both use these
 * constants. They are stored in the settings file, so they never change.
 */

/** The stored names of the kinds of [Pause], written by [SettingsRepository] and read by [SettingsCodec]. */
internal object PauseKinds {
    /** Alerts are on: [Pause.None]. */
    const val NONE = "NONE"

    /** Paused until the user turns alerts back on: [Pause.Indefinite]. */
    const val INDEFINITE = "INDEFINITE"

    /** Paused until the stored deadline: [Pause.Until]. */
    const val UNTIL = "UNTIL"
}
