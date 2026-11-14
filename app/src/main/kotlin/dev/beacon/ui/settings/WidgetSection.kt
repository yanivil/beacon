package dev.beacon.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.WidgetTap
import dev.beacon.ui.components.OptionButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why the widget's tap is chosen here and not on the widget: the widget keeps
 * its one big button, which stays readable at the largest font on a 4 x 2
 * widget, so what that tap does is a choice in Alert settings rather than a
 * second button (the owner's decision, 14 November 2026). "Pause for 1 hour"
 * stays first and the default: alerts come back on by themselves, so a
 * forgotten pause can't hide messages for long. A tap while paused turns
 * alerts ON whichever is chosen, so the choice only changes what OFF means.
 */

/**
 * The "Widget" section: what one widget tap does while alerts are ON, as a radio group.
 *
 * @param settings the current settings.
 * @param actions the changes the screen can ask for.
 */
@Composable
fun WidgetSection(settings: BeaconSettings, actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.widget_heading))
        BeaconText(stringResource(R.string.widget_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WidgetTap.entries.forEach { tap ->
                OptionButton(
                    label = stringResource(labelRes(tap)),
                    selected = settings.widgetTap == tap,
                    onSelect = { actions.setWidgetTap(tap) },
                    minHeight = 96.dp,
                    modifier = Modifier.fillMaxWidth(),
                    sub = listOf(stringResource(subRes(tap))),
                )
            }
        }
    }
}

/**
 * Names a widget-tap choice.
 *
 * @param tap the choice.
 * @return the option's label.
 */
@StringRes
private fun labelRes(tap: WidgetTap): Int = when (tap) {
    WidgetTap.PAUSE_ONE_HOUR -> R.string.widget_choice_hour
    WidgetTap.OFF_UNTIL_TAPPED -> R.string.widget_choice_off
}

/**
 * Says what a widget-tap choice means for when alerts come back.
 *
 * @param tap the choice.
 * @return the option's sub-line.
 */
@StringRes
private fun subRes(tap: WidgetTap): Int = when (tap) {
    WidgetTap.PAUSE_ONE_HOUR -> R.string.widget_choice_hour_sub
    WidgetTap.OFF_UNTIL_TAPPED -> R.string.widget_choice_off_sub
}
