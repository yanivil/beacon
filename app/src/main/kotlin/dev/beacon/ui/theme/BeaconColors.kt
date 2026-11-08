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
    /** Used for: the page behind every screen, the Test alert bar, the OFF pill and the Pause button on the ON card. */
    val Page = Color(0xFF0B0B0D)
    /** Used for: main text on the page and on cards; the default text colour. */
    val Text = Color(0xFFFFFFFF)
    /** Used for: secondary text, such as hints and sub-lines, on the page, cards and overlay labels. */
    val TextSecondary = Color(0xFFD9D9DF)
    /** Used for: cards and unselected option buttons. */
    val Card = Color(0xFF1B1B20)
    /** Used for: the tile behind an app's icon in an app row. */
    val IconTile = Color(0xFF2A2A31)
    /** Used for: the bottom tab bar. */
    val TabBar = Color(0xFF141418)
    /** Used for: the borders of cards and unselected option buttons. */
    val Border = Color(0xFFA1A1AA)
    /** Used for: the lines above the Test alert bar and the tab bar, and inside Home's summary card. */
    val Divider = Color(0xFF3A3A42)
    /** Used for: a disabled button's text, icon and dashed border (Shorter/Longer at the ends). */
    val Disabled = Color(0xFFAEAEB6)
    /** Used for: the ON card, selected options and tab, the ON pill and the yellow buttons. */
    val Accent = Color(0xFFFFD60A)
    /** Used for: text and icons on Accent, and on the white Test alert button. */
    val OnAccent = Color(0xFF0B0B0D)
    /** Used for: overlay cards, swatch labels and the dark phase of a blink. */
    val Black = Color(0xFF000000)
    /** Used for: borders, icons and text on black, and the Test alert and Stop buttons. */
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
