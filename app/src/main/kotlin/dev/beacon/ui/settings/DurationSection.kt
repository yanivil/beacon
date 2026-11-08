package dev.beacon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.DurationChoice
import dev.beacon.ui.components.AdaptiveGrid
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.components.OptionButton
import dev.beacon.ui.components.OutlineButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType

/*
 * Why Shorter/Longer buttons instead of a slider: sliders are banned by the
 * project rules, so the custom length moves one preset step at a time. The
 * value is a polite live region, so TalkBack reads each new length aloud.
 * The panel exists only while Custom is chosen, so other choices stay short.
 */

/**
 * The "How long to flash" section: four duration options and, for Custom,
 * the Custom length panel.
 *
 * @param settings the current settings.
 * @param actions the changes the screen can ask for.
 */
@Composable
fun DurationSection(settings: BeaconSettings, actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.duration_heading))
        BasicText(stringResource(R.string.duration_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        val choices = DurationChoice.entries
        AdaptiveGrid(count = choices.size, modifier = Modifier.selectableGroup()) { index ->
            val choice = choices[index]
            OptionButton(
                label = stringResource(durationLabel(choice)),
                selected = settings.duration == choice,
                onSelect = { actions.setDuration(choice) },
                minHeight = 120.dp,
                stacked = true,
                // Only Custom has a sub-line: its current length.
                sub = if (choice == DurationChoice.CUSTOM) {
                    listOf(stringResource(R.string.minutes_short, settings.customLength.minutes))
                } else {
                    emptyList()
                },
            )
        }
        if (settings.duration == DurationChoice.CUSTOM) CustomPanel(settings, actions)
    }
}

/**
 * The string resource for a duration option's label.
 *
 * @param choice the duration option.
 * @return the label's string resource id.
 */
private fun durationLabel(choice: DurationChoice): Int = when (choice) {
    DurationChoice.ONE_MINUTE -> R.string.duration_one_minute
    DurationChoice.FIVE_MINUTES -> R.string.duration_five_minutes
    DurationChoice.CUSTOM -> R.string.duration_custom
    DurationChoice.UNTIL_STOPPED -> R.string.duration_until_stopped
}

/**
 * The Custom length panel: the value, Shorter and Longer, and the range.
 *
 * @param settings the current settings.
 * @param actions the changes the screen can ask for.
 */
@Composable
private fun CustomPanel(settings: BeaconSettings, actions: SettingsActions) {
    val shape = RoundedCornerShape(20.dp)
    val length = settings.customLength
    Column(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Card, shape)
            .border(2.dp, BeaconColors.Border, shape)
            // The design's 14 dp padding plus the 2 dp border, which is drawn inside the bounds.
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionHeading(stringResource(R.string.custom_heading))
        BasicText(
            text = stringResource(R.string.minutes_short, length.minutes),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = BeaconType.CustomValue.copy(color = BeaconColors.Text, textAlign = TextAlign.Center),
        )
        AdaptiveGrid(count = 2) { index ->
            val longer = index == 1
            val enabled = if (longer) !length.isLongest else !length.isShortest
            OutlineButton(
                text = stringResource(if (longer) R.string.custom_longer else R.string.custom_shorter),
                onClick = { actions.stepCustom(longer) },
                content = BeaconColors.White,
                icon = if (longer) BeaconIcons.Plus else BeaconIcons.Minus,
                minHeight = 76.dp,
                style = BeaconType.Button,
                cornerRadius = 20.dp,
                enabled = enabled,
            )
        }
        BasicText(stringResource(R.string.custom_range), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
    }
}
