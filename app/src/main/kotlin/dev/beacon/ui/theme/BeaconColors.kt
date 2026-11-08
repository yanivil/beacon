package dev.beacon.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Why fixed tokens: Beacon uses the design's own colours (no Material dynamic
 * colour), and every text pair is listed in textPairs so ContrastTest can
 * enforce the 7:1 AAA rule on each one.
 */

/**
 * One text-on-background pair used somewhere in the app.
 *
 * @property text the text colour.
 * @property background the colour behind the text.
 * @property use where the pair appears, for test failure messages.
 */
data class TextPair(val text: Color, val background: Color, val use: String)

/** Beacon's colour tokens (spec section 11). */
object BeaconColors {
    val Page = Color(0xFF0B0B0D)
    val Text = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFD9D9DF)
    val Card = Color(0xFF1B1B20)
    val IconTile = Color(0xFF2A2A31)
    val TabBar = Color(0xFF141418)
    val Border = Color(0xFFA1A1AA)
    val Divider = Color(0xFF3A3A42)
    val Disabled = Color(0xFFAEAEB6)
    val Accent = Color(0xFFFFD60A)
    val OnAccent = Color(0xFF0B0B0D)
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    /** The 11 text pairs that must reach 7:1. */
    val textPairs: List<TextPair> = listOf(
        TextPair(Text, Page, "Text on the page, Pause button, OFF pill, widget PAUSED"),
        TextPair(TextSecondary, Page, "Secondary text on the page"),
        TextPair(Text, Card, "Text on cards"),
        TextPair(TextSecondary, Card, "Secondary text on cards"),
        TextPair(Disabled, Card, "Disabled Shorter/Longer"),
        TextPair(OnAccent, Accent, "ON card, selected options, ON pill, yellow buttons, widget ON"),
        TextPair(White, Black, "Overlay cards, swatch labels"),
        TextPair(TextSecondary, Black, "Overlay labels"),
        TextPair(Text, TabBar, "Tab labels"),
        TextPair(OnAccent, White, "Test alert button"),
        TextPair(Black, White, "Stop button"),
    )
}
