package dev.beacon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import dev.beacon.settings.FlashColour
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why swatches carry their names: no text is ever drawn straight on a flash
 * colour, because red and blue cannot carry dark text at 7:1. Each name sits
 * on a black label instead (21:1). The chosen swatch shows a check in its
 * label and a dark-and-white ring, so the choice never depends on colour
 * alone, and TalkBack reads each swatch as a RadioButton named by its colour,
 * "1 of 5" to "5 of 5", as a list whatever the grid's columns.
 */

/** Corner radius of a swatch. */
private val SwatchRadius = 18.dp

/**
 * The five flash colours as a radio group, in the order the settings define.
 *
 * White takes a row of its own. The grid keeps 8 dp of room around the
 * swatches so the selected ring is never cut off.
 *
 * @param selected the colour that is chosen now.
 * @param onPick called with the colour that was tapped.
 * @param minHeight each swatch's minimum height (96 dp, or 88 dp per app).
 * @param modifier the modifier for the grid.
 */
@Composable
fun SwatchGrid(selected: FlashColour, onPick: (FlashColour) -> Unit, minHeight: Dp, modifier: Modifier = Modifier) {
    val colours = FlashColour.entries
    AdaptiveGrid(
        count = colours.size,
        spansFullRow = { colours[it] == FlashColour.WHITE },
        // A grid, so the group and each swatch state their list positions (see RadioList.kt).
        modifier = modifier.radioList(colours.size).padding(8.dp),
    ) { index ->
        val colour = colours[index]
        Swatch(colour, colour == selected, { onPick(colour) }, minHeight, Modifier.radioListItem(index))
    }
}

/**
 * One colour swatch with its name label at the bottom-start.
 *
 * @param colour the colour to show and name.
 * @param selected true to draw the ring and the check.
 * @param onClick called when the swatch is tapped.
 * @param minHeight the swatch's minimum height; never less than 64 dp is used.
 * @param modifier the modifier for the swatch, such as its list position.
 */
@Composable
private fun Swatch(colour: FlashColour, selected: Boolean, onClick: () -> Unit, minHeight: Dp, modifier: Modifier) {
    val shape = RoundedCornerShape(SwatchRadius)
    Box(
        modifier = modifier
            // The ring is drawn outside the swatch's bounds, into the grid's padding and gaps.
            .then(if (selected) Modifier.drawBehind { drawSelectedRing() } else Modifier)
            .sizeIn(minWidth = 64.dp, minHeight = max(minHeight, 64.dp))
            .clip(shape)
            .background(Color(colour.argb))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            modifier = Modifier
                .background(BeaconColors.Black, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) BeaconIcon(BeaconIcons.Check, BeaconColors.White, 22.dp)
            BeaconText(
                text = stringResource(colour.nameRes),
                style = BeaconType.SmallButton.copy(color = BeaconColors.White),
            )
        }
    }
}

/**
 * Draws the selected ring around the swatch: 4 dp of page colour next to it,
 * then 4 dp of white outside that, following the swatch's rounded corners.
 */
private fun DrawScope.drawSelectedRing() {
    val radius = SwatchRadius.toPx()
    // Outer white band first; the page band and the swatch then cover its middle.
    listOf(8.dp to BeaconColors.White, 4.dp to BeaconColors.Page).forEach { (spread, colour) ->
        val px = spread.toPx()
        drawRoundRect(
            color = colour,
            topLeft = Offset(-px, -px),
            size = Size(size.width + 2 * px, size.height + 2 * px),
            cornerRadius = CornerRadius(radius + px),
        )
    }
}
