package dev.beacon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.ui.components.OptionButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why only three options: blink speed is one of the BlinkSpeed presets
 * (Slow 30, Medium 60, Fast 90 a minute), all under the 3 flashes per second
 * limit. The screen passes the preset itself, never a number.
 */

/**
 * The "Blink speed" section: Slow, Medium and Fast as a radio group.
 *
 * @param settings the current settings.
 * @param actions the changes the screen can ask for.
 */
@Composable
fun SpeedSection(settings: BeaconSettings, actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.speed_heading))
        BeaconText(stringResource(R.string.speed_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BlinkSpeed.entries.forEach { speed ->
                OptionButton(
                    label = stringResource(speed.nameRes),
                    selected = settings.speed == speed,
                    onSelect = { actions.setSpeed(speed) },
                    minHeight = 96.dp,
                    modifier = Modifier.fillMaxWidth(),
                    sub = buildList {
                        // A plural (lint's PluralsCandidate): the count picks the form, then fills it.
                        add(pluralStringResource(R.plurals.speed_rate, speed.blinksPerMinute, speed.blinksPerMinute))
                        // Fast is the highest safe speed; say so in words.
                        if (speed == BlinkSpeed.FAST) add(stringResource(R.string.speed_fast_note))
                    },
                )
            }
        }
    }
}
