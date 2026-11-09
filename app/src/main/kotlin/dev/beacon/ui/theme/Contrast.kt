package dev.beacon.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/*
 * Why a hand-written function: the 7:1 rule is checked by a unit test that
 * runs on the plain JVM, so the WCAG 2.x formula lives here with no Android
 * dependency.
 */

/**
 * Relative luminance of a colour, per WCAG 2.x.
 *
 * @param c the colour; its alpha is ignored.
 * @return luminance from 0.0 (black) to 1.0 (white).
 */
private fun luminance(c: Color): Double {
    // Linearise one sRGB channel; the 0.04045 knee avoids a pow() on near-black.
    fun lin(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.04045) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
}

/**
 * WCAG contrast ratio between a text colour and its background.
 *
 * @param text the foreground colour.
 * @param background the background colour.
 * @return the ratio from 1.0 to 21.0; order of the arguments does not matter.
 */
fun contrastRatio(text: Color, background: Color): Double {
    val a = luminance(text)
    val b = luminance(background)
    // The lighter colour always goes on top of the fraction.
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}
