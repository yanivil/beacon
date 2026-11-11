package dev.beacon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why a pill with words: the project rules say every switch shows ON or OFF
 * in text, never by colour or knob position alone. The two states also
 * differ in fill, border and knob side, so the state reads at a glance.
 * The pill is only a picture of the state: the row that holds it is the
 * Switch TalkBack reads, so the pill's own semantics are cleared.
 */

/**
 * The ON/OFF pill at the end of an app row.
 *
 * ON is yellow with dark text and a filled knob on the end side. OFF is dark
 * with a white border, white text and a hollow knob on the start side. The
 * pill is at least 100 × 60 dp and grows with its text, so the word is never
 * cut off at large font sizes.
 *
 * @param on true for the ON look, false for OFF.
 * @param modifier the modifier for the pill.
 */
@Composable
fun OnOffPill(on: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        modifier = modifier
            .clearAndSetSemantics {}
            .sizeIn(minWidth = 100.dp, minHeight = 60.dp)
            .background(if (on) BeaconColors.Accent else BeaconColors.Page, shape)
            .border(3.dp, if (on) BeaconColors.Accent else BeaconColors.White, shape)
            // 3 dp border plus the design's padding: 12 dp on the text side, 6 dp on the knob side.
            .padding(start = if (on) 15.dp else 9.dp, end = if (on) 9.dp else 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // OFF lays the pill out in reverse: knob first (start), word last (end).
        if (on) {
            PillWord(on)
            Knob(on)
        } else {
            Knob(on)
            PillWord(on)
        }
    }
}

/**
 * The pill's word, "ON" or "OFF", in the small-button style.
 *
 * @param on which word and colour to show.
 */
@Composable
private fun PillWord(on: Boolean) {
    BeaconText(
        text = stringResource(if (on) R.string.switch_on else R.string.switch_off),
        modifier = Modifier.padding(horizontal = 4.dp),
        style = BeaconType.SmallButton.copy(color = if (on) BeaconColors.OnAccent else BeaconColors.White),
    )
}

/**
 * The pill's 42 dp knob: filled dark when ON, a hollow white ring when OFF.
 *
 * @param on which look to draw.
 */
@Composable
private fun Knob(on: Boolean) {
    val look = if (on) {
        Modifier.background(BeaconColors.OnAccent, CircleShape)
    } else {
        Modifier.border(3.dp, BeaconColors.White, CircleShape)
    }
    Box(Modifier.size(42.dp).then(look))
}
