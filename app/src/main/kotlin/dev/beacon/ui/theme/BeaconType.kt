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

@OptIn(ExperimentalTextApi::class)
/** The bundled font at weights 400, 700 and 800. */
val BeaconFontFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_next, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.atkinson_hyperlegible_next, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.atkinson_hyperlegible_next, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Beacon's type scale (spec section 11). */
object BeaconType {
    private fun style(size: Int, weight: FontWeight, lineHeight: Double) = TextStyle(
        fontFamily = BeaconFontFamily,
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = lineHeight.em,
    )

    val ScreenTitle = style(36, FontWeight.ExtraBold, 1.15)
    val State = style(56, FontWeight.ExtraBold, 1.05)
    val Heading = style(28, FontWeight.ExtraBold, 1.2)
    val OptionLabel = style(28, FontWeight.ExtraBold, 1.15)
    val RowTitle = style(24, FontWeight.ExtraBold, 1.2)
    val Button = style(24, FontWeight.ExtraBold, 1.2)
    val SmallButton = style(22, FontWeight.ExtraBold, 1.2)
    val BodyStrong = style(22, FontWeight.Bold, 1.4)
    val Body = style(20, FontWeight.Normal, 1.4)
    val Sub = style(20, FontWeight.Bold, 1.4)
    val TestButton = style(26, FontWeight.ExtraBold, 1.2)
    val Stop = style(32, FontWeight.ExtraBold, 1.2)
    val OverlayTitle = style(34, FontWeight.ExtraBold, 1.15)
    val CustomValue = style(40, FontWeight.ExtraBold, 1.1)
    val Countdown = style(96, FontWeight.ExtraBold, 1.0)
}
