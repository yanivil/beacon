package dev.beacon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why one shared button body: every button in Beacon must have a visible text
 * label (no icon-only buttons), be at least 64 × 64 dp, wrap its text instead
 * of cutting it off at large font sizes, and be read by TalkBack as a Button.
 * Filled and outline buttons differ only in colours and border, so both draw
 * through ButtonBody and cannot drift apart on those rules.
 *
 * Why the secondary text is a separate item in a flow row: the design draws
 * "(3 sec)" smaller after "Test alert". As its own item it stays whole,
 * beside the main text while both fit on one line and on the next line
 * otherwise, instead of breaking between "(3" and "sec)".
 */

/** The smallest touch target in either direction (project rule). */
private val MinTouch = 64.dp

/** Test tag on a button's trailing icon, so tests can check which side it is on. */
internal const val TRAILING_ICON_TAG = "button_trailing_icon"

/** Test tag on a button's leading icon, so tests can check that it keeps its size. */
internal const val LEADING_ICON_TAG = "button_leading_icon"

/**
 * A solid button, such as "Pause all alerts", "Test alert" or "Stop".
 *
 * It is only as wide as its content unless the caller passes
 * `Modifier.fillMaxWidth()`.
 *
 * @param text the visible label, which is also TalkBack's label.
 * @param onClick called when the button is tapped.
 * @param background the fill colour.
 * @param content the text and icon colour; the pair must reach 7:1.
 * @param icon an optional decorative icon before the text, or null.
 * @param minHeight the design's height; never less than 64 dp is used.
 * @param style the text style, such as BeaconType.Button.
 * @param modifier the modifier for the button.
 * @param cornerRadius the corner radius (18 dp by default; 20 for Test alert, 24 for Stop).
 * @param border an optional border, such as Stop's 4 dp black one.
 * @param enabled false to ignore taps and have TalkBack say "Disabled".
 * @param secondaryText optional text after [text], in Sub (20 sp, 700) and
 *   the same colour, such as "(3 sec)"; it is part of the spoken label.
 */
@Composable
fun FilledButton(
    text: String,
    onClick: () -> Unit,
    background: Color,
    content: Color,
    icon: ImageVector?,
    minHeight: Dp,
    style: TextStyle,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    secondaryText: String? = null,
) {
    ButtonBody(
        text, onClick, background, content, border, icon, minHeight, style, cornerRadius, modifier, enabled,
        secondaryText = secondaryText,
    )
}

/**
 * A see-through button with a 3 dp border, such as "Change settings" or
 * "Shorter". When disabled, its border, text and icon turn
 * [BeaconColors.Disabled] (7.78:1 on a card, so the text stays readable) and
 * the border turns dashed (10 dp dashes, 6 dp gaps). The dashes carry the
 * state, because the grey alone differs too little from white to show it.
 *
 * @param text the visible label, which is also TalkBack's label.
 * @param onClick called when the button is tapped.
 * @param content the border, text and icon colour while enabled.
 * @param icon an optional decorative icon before the text, or null.
 * @param minHeight the design's height; never less than 64 dp is used.
 * @param style the text style, such as BeaconType.SmallButton.
 * @param modifier the modifier for the button.
 * @param cornerRadius the corner radius (18 dp by default).
 * @param enabled false to grey the button out, dash its border, ignore taps and have TalkBack say "Disabled".
 * @param trailingIcon an optional decorative icon after the text, in the same
 *   colour, or null; an auto-mirrored icon such as the chevron flips in
 *   right-to-left layouts.
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    content: Color,
    icon: ImageVector?,
    minHeight: Dp,
    style: TextStyle,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val colour = if (enabled) content else BeaconColors.Disabled
    ButtonBody(
        text, onClick, Color.Transparent, colour, BorderStroke(3.dp, colour),
        icon, minHeight, style, cornerRadius, modifier, enabled, dashedBorder = !enabled, trailingIcon = trailingIcon,
    )
}

/**
 * The body both button kinds share: shape, colours, the 64 dp floor, the
 * Button role and a centred icon and text with a 12 dp gap. Icons keep their
 * size at any font scale; the label wraps around them instead.
 *
 * @param text the visible label.
 * @param onClick called when tapped.
 * @param background the fill colour.
 * @param content the text and icon colour.
 * @param border the border, or null.
 * @param icon the decorative icon before the text, or null.
 * @param minHeight the requested minimum height.
 * @param style the text style.
 * @param cornerRadius the corner radius.
 * @param modifier the caller's modifier.
 * @param enabled whether taps are accepted.
 * @param dashedBorder true to draw [border] dashed instead of solid (see [drawDashedBorder]).
 * @param secondaryText the smaller text after [text], or null.
 * @param trailingIcon the decorative icon after the text (28 dp), or null.
 */
@Composable
private fun ButtonBody(
    text: String,
    onClick: () -> Unit,
    background: Color,
    content: Color,
    border: BorderStroke?,
    icon: ImageVector?,
    minHeight: Dp,
    style: TextStyle,
    cornerRadius: Dp,
    modifier: Modifier,
    enabled: Boolean,
    dashedBorder: Boolean = false,
    secondaryText: String? = null,
    trailingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    // The border is drawn inside the bounds, so the padding grows by its width
    // to keep the text clear of it, as CSS's border-box does in the design.
    val inset = border?.width ?: 0.dp
    Row(
        modifier = modifier
            .sizeIn(minWidth = MinTouch, minHeight = max(minHeight, MinTouch))
            .clip(shape)
            .background(background)
            .then(
                when {
                    border == null -> Modifier
                    dashedBorder -> Modifier.drawBehind { drawDashedBorder(border, cornerRadius) }
                    else -> Modifier.border(border, shape)
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp + inset, vertical = 12.dp + inset),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A leading icon is measured before the label, so it always gets its full size.
        if (icon != null) BeaconIcon(icon, content, 30.dp, Modifier.testTag(LEADING_ICON_TAG))
        // A Row measures its children in order and the label takes all the width it
        // can, so a trailing icon would get only what is left (about 4 dp at the
        // largest font). Weighting the label makes the Row measure it last, after
        // the icon; fill = false keeps it only as wide as its text, so the group
        // stays centred. The label wraps instead of squeezing the icon.
        val labelModifier = if (trailingIcon != null) Modifier.weight(1f, fill = false) else Modifier
        // No maxLines and no ellipsis: at large font sizes the label wraps and
        // the button grows taller instead of cutting words off.
        val mainText = style.copy(color = content, textAlign = TextAlign.Center)
        if (secondaryText == null) {
            BeaconText(text = text, modifier = labelModifier, style = mainText)
        } else {
            // Each line of the flow is centred, and the smaller text is centred on the main text's line.
            FlowRow(
                modifier = labelModifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                BeaconText(text = text, style = mainText)
                // BeaconText takes the direction from the text's own first letter, so an
                // English "(3 sec)" in a right-to-left layout is laid out as the left-to-right
                // run it was measured as, instead of wrapping as "3)" over "(sec".
                BeaconText(
                    text = secondaryText,
                    style = BeaconType.Sub.copy(color = content, textAlign = TextAlign.Center),
                )
            }
        }
        // Drawn after the text; an auto-mirrored icon flips itself in right-to-left.
        if (trailingIcon != null) BeaconIcon(trailingIcon, content, 28.dp, Modifier.testTag(TRAILING_ICON_TAG))
    }
}
