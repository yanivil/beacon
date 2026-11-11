package dev.beacon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import dev.beacon.ui.components.radioList
import dev.beacon.ui.components.radioListItem
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why Shorter/Longer buttons instead of a slider: sliders are banned by the
 * project rules, so the custom length moves one preset step at a time. The
 * value is a polite live region, so TalkBack reads each new length aloud.
 * The panel exists only while Custom is chosen, so other choices stay short.
 *
 * Why the value's text is a new node for each length: TalkBack reads a live
 * region when it gets a content-change event from it. Compose 1.11 reports a
 * changed text through a pass that keeps only the verdict of the last
 * property it compares, and a BasicText (which BeaconText draws) also
 * carries text-substitution actions that are rebuilt on every change but
 * count as unchanged. Whether a
 * text change survives that pass depends on hash order, and on the Android 16
 * emulator it never did, so the value was never read aloud. Compose also
 * reports a child's layout change (a new or resized child) as a subtree change
 * of the live region that holds it, which is how the test overlay's countdown
 * is read. Keying the text on the length makes every step that kind of change.
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
        BeaconText(stringResource(R.string.duration_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        val choices = DurationChoice.entries
        // A grid, so the group and each option state their list positions (see RadioList.kt).
        AdaptiveGrid(count = choices.size, modifier = Modifier.radioList(choices.size)) { index ->
            val choice = choices[index]
            OptionButton(
                label = stringResource(durationLabel(choice)),
                selected = settings.duration == choice,
                onSelect = { actions.setDuration(choice) },
                minHeight = 120.dp,
                stacked = true,
                modifier = Modifier.radioListItem(index),
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
        CustomValue(length.minutes)
        AdaptiveGrid(count = 2) { index ->
            val longer = index == 1
            val enabled = if (longer) !length.isLongest else !length.isShortest
            OutlineButton(
                text = stringResource(if (longer) R.string.custom_longer else R.string.custom_shorter),
                onClick = { actions.stepCustom(longer) },
                content = BeaconColors.White,
                icon = if (longer) BeaconIcons.Plus else BeaconIcons.Minus,
                minHeight = 76.dp,
                style = BeaconType.SmallButton,
                cornerRadius = 20.dp,
                enabled = enabled,
            )
        }
        BeaconText(stringResource(R.string.custom_range), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
    }
}

/**
 * The custom length in large type: a polite live region that TalkBack reads
 * aloud after every Shorter or Longer, such as "15 min".
 *
 * @param minutes the length to show.
 */
@Composable
private fun CustomValue(minutes: Int) {
    // The live region holds the text, as the test overlay's countdown card does, so
    // a change inside it is reported as a change of the live region itself.
    Box(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        // A new text node for each value: its first layout is what tells TalkBack
        // the value changed (see the file comment), even when the new value is
        // exactly as wide as the old one.
        key(minutes) {
            BeaconText(
                text = stringResource(R.string.minutes_short, minutes),
                modifier = Modifier.fillMaxWidth(),
                style = BeaconType.CustomValue.copy(color = BeaconColors.Text, textAlign = TextAlign.Center),
            )
        }
    }
}
