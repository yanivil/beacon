package dev.beacon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.ColourMode
import dev.beacon.ui.colourName
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.components.OutlineButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.durationSummary
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why label and value share a wrapping row: at the largest font the value
 * ("Different per app") no longer fits beside its label, so it drops to the
 * next line instead of being cut off. The colour swatch is only a picture;
 * the colour's name beside it carries the meaning, so TalkBack skips it.
 */

/**
 * The "How alerts look" card with its "Change settings" button.
 *
 * @param settings the current settings.
 * @param onChange called when "Change settings" is tapped.
 */
@Composable
fun SummaryCard(settings: BeaconSettings, onChange: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Card, shape)
            .border(2.dp, BeaconColors.Border, shape)
            // The design's 20 / 18 dp padding plus the 2 dp border, which is drawn inside the bounds.
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeading(stringResource(R.string.summary_heading))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryRow(stringResource(R.string.summary_flashes_for), divider = true) {
                BeaconText(durationSummary(settings), style = BeaconType.RowTitle.copy(color = BeaconColors.Text))
            }
            SummaryRow(stringResource(R.string.summary_blink_speed), divider = true) {
                // The speed's name only; the rate is for the Alert settings screen.
                BeaconText(stringResource(settings.speed.nameRes), style = BeaconType.RowTitle.copy(color = BeaconColors.Text))
            }
            SummaryRow(stringResource(R.string.summary_screen_color), divider = false) {
                if (settings.colourMode == ColourMode.SAME_FOR_ALL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val swatchShape = RoundedCornerShape(6.dp)
                        Box(
                            Modifier
                                .clearAndSetSemantics {}
                                .size(24.dp)
                                .background(Color(settings.globalColour.argb), swatchShape)
                                .border(2.dp, BeaconColors.White, swatchShape),
                        )
                        BeaconText(colourName(settings.globalColour), style = BeaconType.RowTitle.copy(color = BeaconColors.Text))
                    }
                } else {
                    BeaconText(stringResource(R.string.summary_per_app), style = BeaconType.RowTitle.copy(color = BeaconColors.Text))
                }
            }
        }
        OutlineButton(
            text = stringResource(R.string.summary_change),
            onClick = onChange,
            content = BeaconColors.White,
            icon = null,
            minHeight = 76.dp,
            style = BeaconType.Button,
            cornerRadius = 18.dp,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = BeaconIcons.Chevron,
        )
    }
}

/**
 * One label and value row: the label at the start, the value at the end,
 * wrapping onto a second line when they do not fit together.
 *
 * @param label the label, in secondary text colour.
 * @param divider true to draw a 2 dp line under the row, with 12 dp above it.
 * @param value the value.
 */
@Composable
private fun SummaryRow(label: String, divider: Boolean, value: @Composable () -> Unit) {
    Column {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            // The end padding keeps a 12 dp gap to the value when both just fit on one line.
            BeaconText(label, Modifier.padding(end = 12.dp), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
            value()
        }
        if (divider) {
            Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(2.dp).background(BeaconColors.Divider))
        }
    }
}
