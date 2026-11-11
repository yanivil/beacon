package dev.beacon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why the whole row is one switch: a senior should not have to hit a small
 * knob. Tapping anywhere on the row turns that app ON or OFF, and TalkBack
 * reads one control: the app name and its flash line as the label, the Switch
 * role and "On"/"Off" as the state (spec section 11). The icon and the pill
 * are pictures only, so their semantics are cleared and they add nothing to
 * what is read.
 *
 * Why a custom layout: at the largest font size the pill grows, and on a
 * narrow phone the text beside it would get under 80 dp, breaking words such
 * as "WhatsApp" into two-letter lines. So the pill keeps its own size and
 * wraps onto a line of its own, at the end side, whenever keeping it beside
 * the text would split a word. At normal sizes the row is the design's single
 * line.
 */

/** Test tag on an app row's icon tile. */
internal const val APP_ROW_ICON_TAG = "app_row_icon"

/** Test tag on an app row's name text. */
internal const val APP_ROW_NAME_TAG = "app_row_name"

/** The gap between the tile, the text and the pill, and above a wrapped pill. */
internal val RowGap = 12.dp

/**
 * One app's row on Home, such as "Texts (SMS) / Yellow flash / ON".
 *
 * At large font sizes the text takes the width the icon tile and the pill
 * leave and wraps inside it, and the row grows taller. If even a single word
 * would not fit there, the pill moves under the text.
 *
 * @param name the app's name, such as "Texts (SMS)".
 * @param flashLine the line under the name, such as "Yellow flash" or "No flash".
 * @param swatch the flash colour shown before [flashLine], or null for none.
 * @param enabled true when Beacon flashes for this app (the switch is ON).
 * @param onToggle called when the row is tapped, to flip [enabled].
 * @param modifier the modifier for the row.
 * @param icon the app's decorative icon, drawn on the 52 dp tile.
 */
@Composable
fun AppSwitchRow(
    name: String,
    flashLine: String,
    swatch: Color?,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .testTag(APP_ROW_ICON_TAG)
                    .clearAndSetSemantics {}
                    .size(52.dp)
                    .background(BeaconColors.IconTile, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BeaconText(
                    text = name,
                    modifier = Modifier.testTag(APP_ROW_NAME_TAG),
                    style = BeaconType.RowTitle.copy(color = BeaconColors.Text),
                )
                FlashLine(flashLine, swatch)
            }
            OnOffPill(on = enabled)
        },
        modifier = modifier
            .heightIn(min = 100.dp)
            .clip(shape)
            .background(BeaconColors.Card)
            .border(2.dp, BeaconColors.Border, shape)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() })
            // The design's 14/12 dp padding plus the 2 dp border, which is drawn inside the bounds.
            .padding(horizontal = 14.dp, vertical = 16.dp),
        measurePolicy = AppRowMeasurePolicy,
    )
}

/**
 * Lays out the row's parts: an optional tile, then the text and the pill.
 *
 * The tile and the pill keep their own sizes. The text gets the width
 * between them, unless that is narrower than its longest word (its minimum
 * intrinsic width): then the pill wraps to a second line at the end side and
 * the text gets the width beside the tile. Everything is centred vertically,
 * and placeRelative mirrors the row in right-to-left. Shared with the
 * Finish setup rows, which have no tile.
 *
 * @param hasTile true when the first child is a tile (three children), false
 *   when the row is only text and pill (two children).
 * @return the measure policy.
 */
internal fun TextAndPillPolicy(hasTile: Boolean) = MeasurePolicy { measurables, constraints ->
    val gap = RowGap.roundToPx()
    val loose = constraints.copy(minWidth = 0, minHeight = 0)
    val tile = if (hasTile) measurables[0].measure(loose) else null
    val textMeasurable = measurables[if (hasTile) 1 else 0]
    val pill = measurables[if (hasTile) 2 else 1].measure(loose)
    // Without a tile there is no tile gap either.
    val tileSpace = if (tile != null) tile.width + gap else 0
    val width = if (constraints.hasBoundedWidth) {
        constraints.maxWidth
    } else {
        // Fallback for an unbounded parent: as wide as the text on one line.
        tileSpace + textMeasurable.maxIntrinsicWidth(Constraints.Infinity) + gap + pill.width
    }
    val besidePill = width - tileSpace - pill.width - gap
    val wrap = besidePill < textMeasurable.minIntrinsicWidth(Constraints.Infinity)
    // If a word is too long even beside the tile alone, it breaks there: the last resort.
    val textWidth = (if (wrap) width - tileSpace else besidePill).coerceAtLeast(0)
    // The row's height limit is passed on, so text that cannot fit reports an overflow.
    val text = textMeasurable.measure(
        Constraints(minWidth = textWidth, maxWidth = textWidth, maxHeight = constraints.maxHeight),
    )
    val lineHeight = maxOf(tile?.height ?: 0, text.height, if (wrap) 0 else pill.height)
    val contentHeight = if (wrap) lineHeight + gap + pill.height else lineHeight
    val height = constraints.constrainHeight(contentHeight)
    val top = (height - contentHeight) / 2
    layout(width, height) {
        tile?.placeRelative(0, top + (lineHeight - tile.height) / 2)
        text.placeRelative(tileSpace, top + (lineHeight - text.height) / 2)
        val pillTop = if (wrap) top + lineHeight + gap else top + (lineHeight - pill.height) / 2
        pill.placeRelative(width - pill.width, pillTop)
    }
}

/** The app row's policy: tile, text and pill. */
private val AppRowMeasurePolicy = TextAndPillPolicy(hasTile = true)

/**
 * The line under the app name, with an optional colour swatch before it.
 *
 * @param text the line, such as "Yellow flash".
 * @param swatch the flash colour, or null to show the text alone.
 */
@Composable
private fun FlashLine(text: String, swatch: Color?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (swatch != null) {
            val swatchShape = RoundedCornerShape(6.dp)
            Box(
                Modifier
                    .size(20.dp)
                    .background(swatch, swatchShape)
                    .border(2.dp, BeaconColors.White, swatchShape),
            )
        }
        BeaconText(text = text, style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
    }
}
