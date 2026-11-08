package dev.beacon.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.beacon.R

/*
 * Why one bundled variable font: Atkinson Hyperlegible Next is designed for
 * low vision. The one file is used at three weights through variation
 * settings. All sizes are in sp so text follows the system font size; the
 * smallest is 20 sp, as the project rules require.
 */

/** The bundled font at weights 400, 700 and 800. */
@OptIn(ExperimentalTextApi::class)
val BeaconFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_next, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.atkinson_hyperlegible_next, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.atkinson_hyperlegible_next, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Beacon's type scale (spec section 11). */
object BeaconType {
    /**
     * Builds one style of the scale in the bundled font.
     *
     * @param size The text size in sp.
     * @param weight The font weight: Normal (400), Bold (700) or ExtraBold (800).
     * @param lineHeight The line height as a multiple of the text size.
     * @return The text style; screens add the colour with copy().
     */
    private fun style(size: Int, weight: FontWeight, lineHeight: Double) = TextStyle(
        fontFamily = BeaconFontFamily,
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = lineHeight.em,
    )

    /** Used for: the screen title (H1) at the top of each tab, 36 sp. */
    val ScreenTitle = style(36, FontWeight.ExtraBold, 1.15)
    /** Used for: the ON / PAUSED word on Home's alerts card, 56 sp. */
    val State = style(56, FontWeight.ExtraBold, 1.05)
    /** Used for: section headings, 28 sp bold. */
    val Heading = style(28, FontWeight.ExtraBold, 1.2)
    /** Used for: the main label of an option button, such as "Fast", 28 sp. */
    val OptionLabel = style(28, FontWeight.ExtraBold, 1.15)
    /** Used for: app names in the app rows and the values in Home's summary card, 24 sp. */
    val RowTitle = style(24, FontWeight.ExtraBold, 1.2)
    /** Used for: button labels, such as "Pause all alerts" and "Change settings", 24 sp. */
    val Button = style(24, FontWeight.ExtraBold, 1.2)
    /** Used for: Change/Done, Shorter/Longer, the ON/OFF pills, swatch labels and overlay values, 22 sp. */
    val SmallButton = style(22, FontWeight.ExtraBold, 1.2)
    /** Used for: card copy, such as the line under ON or PAUSED, and the countdown's words, 22 sp bold. */
    val BodyStrong = style(22, FontWeight.Bold, 1.4)
    /** Used for: explanations and hints; also the default text style, 20 sp. */
    val Body = style(20, FontWeight.Normal, 1.4)
    /** Used for: option sub-lines, tab labels and a button's smaller second text, 20 sp bold. */
    val Sub = style(20, FontWeight.Bold, 1.4)
    /** Used for: the "Test alert" button label, 26 sp. */
    val TestButton = style(26, FontWeight.ExtraBold, 1.2)
    /** Used for: the overlay's Stop button label, 32 sp. */
    val Stop = style(32, FontWeight.ExtraBold, 1.2)
    /** Used for: the Test alert overlay's title, 34 sp. */
    val OverlayTitle = style(34, FontWeight.ExtraBold, 1.15)
    /** Used for: the Custom length value, such as "10 min", 40 sp. */
    val CustomValue = style(40, FontWeight.ExtraBold, 1.1)
    /** Used for: the overlay's seconds-left number, 96 sp. */
    val Countdown = style(96, FontWeight.ExtraBold, 1.0)
}
