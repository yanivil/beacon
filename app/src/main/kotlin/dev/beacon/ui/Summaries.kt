package dev.beacon.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.beacon.R
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour

/*
 * Why shared summary functions: Home's "How alerts look" card, the Test alert
 * overlay and the widget all describe the same settings in the same words.
 * Keeping the wording in one place, built from strings.xml (with plurals for
 * "1 minute" against "5 minutes"), stops the screens drifting apart and keeps
 * a translation to one set of resources.
 */

/**
 * How long an alert flashes, in words.
 *
 * @param s the settings to describe.
 * @return "1 minute", "5 minutes", "N minute(s)" for a custom length, or "Until I stop it".
 */
@Composable
fun durationSummary(s: BeaconSettings): String = when (s.duration) {
    DurationChoice.ONE_MINUTE -> pluralStringResource(R.plurals.duration_minutes, 1, 1)
    DurationChoice.FIVE_MINUTES -> pluralStringResource(R.plurals.duration_minutes, 5, 5)
    // The plural form follows the number, so a custom 1 minute reads "1 minute".
    DurationChoice.CUSTOM -> pluralStringResource(R.plurals.duration_minutes, s.customLength.minutes, s.customLength.minutes)
    DurationChoice.UNTIL_STOPPED -> stringResource(R.string.duration_until_stopped)
}

/**
 * A blink speed with its rate, in words.
 *
 * @param speed the preset to describe.
 * @return for example "Medium, 60 a minute".
 */
@Composable
fun speedSummary(speed: BlinkSpeed): String =
    stringResource(R.string.speed_summary, stringResource(speed.nameRes), speed.blinksPerMinute)

/**
 * A flash colour's visible name.
 *
 * @param c the colour.
 * @return for example "Yellow".
 */
@Composable
fun colourName(c: FlashColour): String = stringResource(c.nameRes)
