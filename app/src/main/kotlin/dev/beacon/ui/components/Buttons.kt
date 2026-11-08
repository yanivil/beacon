package dev.beacon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import dev.beacon.ui.theme.BeaconColors

/*
 * Why one shared button body: every button in Beacon must have a visible text
 * label (no icon-only buttons), be at least 64 × 64 dp, wrap its text instead
 * of cutting it off at large font sizes, and be read by TalkBack as a Button.
 * Filled and outline buttons differ only in colours and border, so both draw
 * through ButtonBody and cannot drift apart on those rules.
 */

/** The smallest touch target in either direction (project rule). */
private val MinTouch = 64.dp

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
 * @param cornerRadius the corner radius (18 dp by default; 20 for Test alert, 24 for Stop).
 * @param border an optional border, such as Stop's 4 dp black one.
 * @param modifier the modifier for the button.
 * @param enabled false to ignore taps and have TalkBack say "Disabled".
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
    cornerRadius: Dp = 18.dp,
    border: BorderStroke? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ButtonBody(text, onClick, background, content, border, icon, minHeight, style, cornerRadius, modifier, enabled)
}

/**
 * A see-through button with a 3 dp border, such as "Change settings" or
 * "Shorter". When disabled, its border, text and icon turn
 * [BeaconColors.Disabled] (7.78:1 on a card), so the state shows without
 * relying on TalkBack alone.
 *
 * @param text the visible label, which is also TalkBack's label.
 * @param onClick called when the button is tapped.
 * @param content the border, text and icon colour while enabled.
 * @param icon an optional decorative icon before the text, or null.
 * @param minHeight the design's height; never less than 64 dp is used.
 * @param style the text style, such as BeaconType.SmallButton.
 * @param cornerRadius the corner radius (18 dp by default).
 * @param modifier the modifier for the button.
 * @param enabled false to grey the button out, ignore taps and have TalkBack say "Disabled".
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    content: Color,
    icon: ImageVector?,
    minHeight: Dp,
    style: TextStyle,
    cornerRadius: Dp = 18.dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colour = if (enabled) content else BeaconColors.Disabled
    ButtonBody(
        text, onClick, Color.Transparent, colour, BorderStroke(3.dp, colour),
        icon, minHeight, style, cornerRadius, modifier, enabled,
    )
}

/**
 * The body both button kinds share: shape, colours, the 64 dp floor, the
 * Button role and a centred icon and text with a 12 dp gap.
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
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp + inset, vertical = 12.dp + inset),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) BeaconIcon(icon, content, 30.dp)
        // No maxLines and no ellipsis: at large font sizes the label wraps and
        // the button grows taller instead of cutting words off.
        BasicText(text = text, style = style.copy(color = content, textAlign = TextAlign.Center))
    }
}
