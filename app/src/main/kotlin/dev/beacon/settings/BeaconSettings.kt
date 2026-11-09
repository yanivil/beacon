package dev.beacon.settings

import dev.beacon.alert.MessageSource

/*
 * The whole settings model in one immutable value. The store (a later task)
 * saves and loads it; screens and the alert service only read copies of it.
 */

/**
 * One app's alert setting.
 *
 * @property enabled whether alerts for the app are on.
 * @property colour the app's own flash colour (used in [ColourMode.PER_APP]).
 */
data class AppSetting(val enabled: Boolean, val colour: FlashColour)

/** Whether every app flashes the same colour or each has its own. */
enum class ColourMode { SAME_FOR_ALL, PER_APP }

/**
 * Everything the user can set.
 *
 * @property pause whether alerts are paused.
 * @property apps per-app settings.
 * @property colourMode which colour rule applies.
 * @property globalColour the colour used in [ColourMode.SAME_FOR_ALL].
 * @property duration how long to flash.
 * @property customLength the length used when [duration] is CUSTOM; kept otherwise.
 * @property speed the blink-speed preset.
 */
data class BeaconSettings(
    val pause: Pause,
    val apps: Map<MessageSource, AppSetting>,
    val colourMode: ColourMode,
    val globalColour: FlashColour,
    val duration: DurationChoice,
    val customLength: CustomLength,
    val speed: BlinkSpeed,
) {
    /**
     * The colour a message from [source] flashes.
     *
     * @param source the message app.
     * @return the global colour, or the app's own colour in per-app mode.
     */
    fun colourFor(source: MessageSource): FlashColour = when (colourMode) {
        ColourMode.SAME_FOR_ALL -> globalColour
        // The map is public, so a missing entry falls back to the app's default.
        ColourMode.PER_APP -> apps[source]?.colour ?: source.defaultColour
    }

    /**
     * How long one alert flashes.
     *
     * @return milliseconds, or null to flash until the user stops it.
     */
    fun alertLengthMillis(): Long? = when (duration) {
        DurationChoice.ONE_MINUTE -> 60_000L
        DurationChoice.FIVE_MINUTES -> 300_000L
        DurationChoice.CUSTOM -> customLength.minutes * 60_000L
        DurationChoice.UNTIL_STOPPED -> null
    }

    companion object {
        /** First-run settings: alerts on, SMS and WhatsApp enabled, yellow, 1 minute, Medium. */
        val DEFAULT = BeaconSettings(
            pause = Pause.None,
            // One entry per source, from each source's own defaults.
            apps = MessageSource.entries.associateWith { AppSetting(it.defaultEnabled, it.defaultColour) },
            colourMode = ColourMode.SAME_FOR_ALL,
            globalColour = FlashColour.YELLOW,
            duration = DurationChoice.ONE_MINUTE,
            customLength = CustomLength.DEFAULT,
            speed = BlinkSpeed.MEDIUM,
        )
    }
}
