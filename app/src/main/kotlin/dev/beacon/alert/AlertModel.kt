package dev.beacon.alert

import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour

/*
 * Why a small shared model: the controller, the full-screen overlay and (later)
 * the widget all talk about "an alert" in the same terms. Keeping the types in
 * one file means they can't drift apart. The running state carries the
 * BlinkClock, so every screen reads the same timing and rebuilding a screen
 * can't restart or lengthen an alert.
 */

/** How long a Test alert lasts, in milliseconds (3 seconds). */
const val TEST_LENGTH_MILLIS = 3_000L

/** Extra time the wake lock outlasts each period, so a renewal always runs before the old lock lapses. */
const val WAKE_LOCK_MARGIN_MILLIS = 10_000L

/** How long "Until I stop it" holds the wake lock before renewing it (60 minutes). */
const val WAKE_LOCK_PERIOD_MILLIS = 3_600_000L

/** How often the controller checks the heat itself, in milliseconds (30 seconds), when the system refused the heat listener. */
const val HEAT_POLL_MILLIS = 30_000L

/** Why an alert is running. */
enum class AlertKind {
    /** The user pressed Test alert. */
    TEST,

    /** A message arrived. */
    REAL,
}

/**
 * What to flash and for how long.
 *
 * @property kind Whether this is a test or a real alert.
 * @property colour The screen colour.
 * @property speed The blink-speed preset (never a raw number).
 * @property lengthMillis How long it lasts, or null to run until stopped.
 * @property source The message app that triggered it, or null for a test.
 */
data class AlertRequest(
    val kind: AlertKind,
    val colour: FlashColour,
    val speed: BlinkSpeed,
    val lengthMillis: Long?,
    val source: MessageSource?,
)

/** What the alert controller is doing right now. */
sealed interface AlertState {
    /** No alert is running. */
    data object Idle : AlertState

    /**
     * An alert is running.
     *
     * @property request What was asked for.
     * @property clock The shared timing of the screen and the light.
     * @property lightFlashing False when the phone has no usable camera light or is too hot, so only the screen blinks.
     */
    data class Running(
        val request: AlertRequest,
        val clock: BlinkClock,
        val lightFlashing: Boolean,
    ) : AlertState
}
