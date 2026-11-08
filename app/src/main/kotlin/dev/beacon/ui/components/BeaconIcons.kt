package dev.beacon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.beacon.ui.theme.BeaconColors

/*
 * Why vectors built in code: the design's icons are inline SVGs on a 24-unit
 * view box. Their path data is copied here exactly from Main.dc.html and
 * Widget.dc.html, with the design's stroke widths, so no drawable files have
 * to be kept in step with the design. Single-colour icons are drawn white and
 * tinted where they are used. Every icon is decorative (aria-hidden in the
 * design): the text next to it is the label, so icons never get a content
 * description.
 */

/** Beacon's icons, from the design's SVG path data. */
object BeaconIcons {
    /** The lightning bolt of "ON" and the Test alert button: filled and stroked 2. */
    val Bolt: ImageVector by lazy {
        icon("Bolt") { strokedPath("M13 2L3 14h9l-1 8 10-12h-9l1-8z", 2f, filled = true) }
    }

    /** Two filled bars, for "Pause all alerts" and "PAUSED". */
    val Pause: ImageVector by lazy {
        icon("Pause") { filledPath("M6 4h4v16H6z M14 4h4v16h-4z") }
    }

    /** The speech bubble glyph for Texts (SMS). */
    val Sms: ImageVector by lazy {
        icon("Sms") {
            strokedPath("M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z M7 8h10 M7 12h6", 2.2f)
        }
    }

    /** The round chat bubble glyph for WhatsApp messages. */
    val WhatsApp: ImageVector by lazy {
        icon("WhatsApp") {
            strokedPath(
                "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7" +
                    "a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z",
                2.2f,
            )
        }
    }

    /** The paper plane glyph for Telegram. */
    val Telegram: ImageVector by lazy {
        icon("Telegram") { strokedPath("M22 2L11 13 M22 2l-7 20-4-9-9-4 20-7z", 2.2f) }
    }

    /** The house of the Home tab. */
    val Home: ImageVector by lazy {
        icon("Home") { strokedPath("M3 10l9-7 9 7v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V13h6v9", 2.5f) }
    }

    /** The three sliders of the Alert settings tab (a picture only; Beacon has no sliders). */
    val Sliders: ImageVector by lazy {
        icon("Sliders") {
            strokedPath("M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6", 2.5f)
        }
    }

    /** The plus of "Longer". */
    val Plus: ImageVector by lazy {
        icon("Plus") { strokedPath("M12 5v14 M5 12h14", 3.5f) }
    }

    /** The minus of "Shorter". */
    val Minus: ImageVector by lazy {
        icon("Minus") { strokedPath("M5 12h14", 3.5f) }
    }

    /** The rounded square of the Stop button. */
    val Stop: ImageVector by lazy {
        // The design's <rect x="4" y="4" width="16" height="16" rx="3">, written as a path.
        icon("Stop") { filledPath("M7 4h10a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z") }
    }

    /** The tick shown in a selected swatch's label. */
    val Check: ImageVector by lazy {
        icon("Check") { strokedPath("M20 6L9 17l-5-5", 3.5f) }
    }

    /** The "go on" arrow of "Change settings"; it points the other way in right-to-left. */
    val Chevron: ImageVector by lazy {
        icon("Chevron", autoMirror = true) { strokedPath("M9 18l6-6-6-6", 3f) }
    }

    /** A selected option's mark: a near-black circle with a yellow tick. Keeps its own colours. */
    val SelectedCircle: ImageVector by lazy {
        icon("SelectedCircle") {
            // The design's <circle cx="12" cy="12" r="11">, written as two arcs.
            addPath(addPathNodes("M1 12a11 11 0 1 0 22 0a11 11 0 1 0-22 0z"), fill = SolidColor(BeaconColors.OnAccent))
            strokedPath("M7 12.5l3.2 3.2L17 9", 2.8f, colour = BeaconColors.Accent)
        }
    }

    /** An unselected option's mark: a white ring. Keeps its own colour. */
    val EmptyCircle: ImageVector by lazy {
        // The design's <circle cx="12" cy="12" r="10">, written as two arcs.
        icon("EmptyCircle") { strokedPath("M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0z", 2.5f) }
    }

    /**
     * Builds one icon on the design's 24 × 24 view box.
     *
     * @param name the vector's name, for debugging.
     * @param autoMirror true to flip the icon in right-to-left layouts.
     * @param paths adds the icon's paths to the builder.
     * @return the finished vector.
     */
    private fun icon(name: String, autoMirror: Boolean = false, paths: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).apply(paths).build()

    /**
     * Adds a stroked path with round caps and joins, as every design glyph has.
     *
     * @param pathData the SVG path data, copied from the design.
     * @param width the stroke width in view-box units.
     * @param filled true to also fill the shape (the bolt).
     * @param colour the stroke (and fill) colour; white unless the icon keeps its own colours.
     * @return this builder.
     */
    private fun ImageVector.Builder.strokedPath(
        pathData: String,
        width: Float,
        filled: Boolean = false,
        colour: Color = Color.White,
    ): ImageVector.Builder = addPath(
        pathData = addPathNodes(pathData),
        fill = if (filled) SolidColor(colour) else null,
        stroke = SolidColor(colour),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

    /**
     * Adds a filled white path with no stroke.
     *
     * @param pathData the SVG path data, copied from the design.
     * @return this builder.
     */
    private fun ImageVector.Builder.filledPath(pathData: String): ImageVector.Builder =
        addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.White))
}

/**
 * Draws one decorative icon.
 *
 * It has no content description, because the text beside it is the label.
 *
 * @param vector the icon, usually from [BeaconIcons].
 * @param tint the colour to paint a single-colour icon; [Color.Unspecified]
 *   keeps the icon's own colours (for [BeaconIcons.SelectedCircle]).
 * @param size the icon's width and height.
 * @param modifier the modifier for the icon's box.
 */
@Composable
fun BeaconIcon(vector: ImageVector, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = if (tint.isSpecified) ColorFilter.tint(tint) else null,
    )
}
