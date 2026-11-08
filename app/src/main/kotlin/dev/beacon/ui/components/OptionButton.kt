package dev.beacon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType

/*
 * Why a radio option instead of a toggle button: the design marks options
 * with aria-pressed, but the project rules need platform roles, so each
 * option is a RadioButton that TalkBack reads as "Selected" or "Not
 * selected" (spec section 3, row 8). The state never depends on colour
 * alone: a selected option also swaps its empty ring for a ticked circle.
 */

/**
 * One choice in a group, such as a duration, a blink speed or a colour mode.
 *
 * The caller puts `Modifier.selectableGroup()` on the layout that holds the
 * group's options. TalkBack reads the label, then each sub-line.
 *
 * @param label the option's name, in 28 sp.
 * @param selected true when this is the chosen option.
 * @param onSelect called when the option is tapped.
 * @param minHeight the design's height; never less than 64 dp is used.
 * @param stacked true for the duration grid's layout (circle on top, text
 *   below); false for a row with the circle on the start side.
 * @param modifier the modifier for the option.
 * @param sub the lines under the label, such as "90 blinks a minute", in 20 sp.
 */
@Composable
fun OptionButton(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    minHeight: Dp,
    stacked: Boolean = false,
    modifier: Modifier = Modifier,
    sub: List<String> = emptyList(),
) {
    val shape = RoundedCornerShape(20.dp)
    val frame = modifier
        .sizeIn(minWidth = 64.dp, minHeight = max(minHeight, 64.dp))
        .clip(shape)
        .background(if (selected) BeaconColors.Accent else BeaconColors.Card)
        .border(3.dp, if (selected) BeaconColors.Accent else BeaconColors.Border, shape)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
    val mark = if (selected) BeaconIcons.SelectedCircle else BeaconIcons.EmptyCircle
    // Paddings below are the design's plus the 3 dp border, which is drawn inside the bounds.
    if (stacked) {
        Column(
            modifier = frame.padding(start = 17.dp, end = 17.dp, top = 17.dp, bottom = 19.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BeaconIcon(mark, Color.Unspecified, 32.dp)
            // The top padding keeps a 10 dp gap when the option is no taller than its content.
            OptionTexts(label, sub, selected, Modifier.padding(top = 10.dp))
        }
    } else {
        Row(
            modifier = frame.padding(horizontal = 19.dp, vertical = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The design draws the circle larger beside options that have sub-lines.
            BeaconIcon(mark, Color.Unspecified, if (sub.isEmpty()) 32.dp else 36.dp)
            OptionTexts(label, sub, selected, Modifier.weight(1f))
        }
    }
}

/**
 * The label and its sub-lines, coloured for the selected or unselected look.
 *
 * @param label the option's name.
 * @param sub the lines under the label.
 * @param selected true for dark text on yellow, false for light text on a card.
 * @param modifier the modifier for the text column.
 */
@Composable
private fun OptionTexts(label: String, sub: List<String>, selected: Boolean, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BasicText(
            text = label,
            style = BeaconType.OptionLabel.copy(color = if (selected) BeaconColors.OnAccent else BeaconColors.Text),
        )
        val subColour = if (selected) BeaconColors.OnAccent else BeaconColors.TextSecondary
        sub.forEach { line -> BasicText(text = line, style = BeaconType.Sub.copy(color = subColour)) }
    }
}
