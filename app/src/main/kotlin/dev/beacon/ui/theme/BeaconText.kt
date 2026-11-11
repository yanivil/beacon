package dev.beacon.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection

/*
 * Why one text function: when a style leaves the text direction unset,
 * Compose lays the text out in the layout's direction. On a phone set to
 * Hebrew that is right to left, so Beacon's English lines read "minute 1"
 * instead of "1 minute" and their full stops moved to the start (the phone
 * checks' defect P1, docs/checks/2026-11-phone-checks.md). Every text is
 * drawn here, so each one gets two fixes at once:
 *
 * - Its direction comes from its own words (TextDirection.Content): English
 *   reads left to right, and a future Hebrew translation right to left.
 * - It sits at the layout's start edge. Compose's TextAlign.Start follows
 *   the text's direction, which would move English lines to the left of a
 *   mirrored right-to-left layout. Compose has no alignment that follows the
 *   layout instead, so Start and End become Left or Right from the layout
 *   direction here, and only here. This is the one place that uses left and
 *   right, and it derives them from the layout direction, so the start/end
 *   rule's purpose, a working right-to-left layout, is kept.
 *
 * BeaconTextTest fails if any other file calls BasicText.
 */

/**
 * Draws one text the Beacon way: in its own words' direction, at the layout's start edge.
 *
 * Every screen and component draws its text with this, not with BasicText.
 *
 * @param text the words to draw.
 * @param modifier the modifier for the text's box.
 * @param style the text's style, usually one of [BeaconType] with a colour added.
 */
@Composable
fun BeaconText(text: String, modifier: Modifier = Modifier, style: TextStyle) {
    BasicText(text, modifier, style.forLayout(LocalLayoutDirection.current))
}

/**
 * Returns this style with its direction taken from the text, and its alignment fixed to the layout's edges.
 *
 * @param layoutDirection the direction of the layout the text sits in.
 * @return a copy with [TextDirection.Content], and with an unset, Start or End alignment turned into
 *   the layout's left or right edge; centred and justified text keep their alignment.
 */
internal fun TextStyle.forLayout(layoutDirection: LayoutDirection): TextStyle {
    val rtl = layoutDirection == LayoutDirection.Rtl
    val align = when (textAlign) {
        // Unset behaves as Start in Compose, so both go to the layout's start edge.
        TextAlign.Unspecified, TextAlign.Start -> if (rtl) TextAlign.Right else TextAlign.Left
        TextAlign.End -> if (rtl) TextAlign.Left else TextAlign.Right
        else -> textAlign
    }
    return copy(textDirection = TextDirection.Content, textAlign = align)
}
