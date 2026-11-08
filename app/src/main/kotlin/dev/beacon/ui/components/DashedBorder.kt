package dev.beacon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why a disabled outline button is dashed: the project rules say state is
 * never shown by colour alone. White against the disabled grey is only about
 * 2.2:1, below the 3:1 WCAG asks for a non-text change, so a disabled
 * Shorter/Longer button also swaps its solid border for a dashed one. The
 * shape change shows the state even to someone who can't tell the two greys
 * apart.
 */

/** Dash length of a disabled outline button's border. */
private val DashLength = 10.dp

/** Gap between the dashes of a disabled outline button's border. */
private val DashGap = 6.dp

/**
 * Draws [border] as a dashed rounded rectangle inside the bounds, where
 * Modifier.border would draw the solid one, so the button keeps its size.
 *
 * @param border the border's width and colour.
 * @param cornerRadius the button's outer corner radius.
 */
internal fun DrawScope.drawDashedBorder(border: BorderStroke, cornerRadius: Dp) {
    val width = border.width.toPx()
    // A stroke is centred on its path, so the path is inset by half the width
    // (and its corners shrink to match) to keep the whole stroke inside.
    val half = width / 2
    val radius = (cornerRadius.toPx() - half).coerceAtLeast(0f)
    drawRoundRect(
        brush = border.brush,
        topLeft = Offset(half, half),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius(radius),
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashGap.toPx())),
        ),
    )
}
