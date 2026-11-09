package dev.beacon.widget

import kotlin.math.ceil
import kotlin.math.max

/*
 * Why an estimate in the tests: the fit's own rules (the drop order, the
 * gaps, the icon, the kept state word) are tested on the JVM without
 * Android's text layout. This is the estimate the fit used before ruling F5,
 * so the cases written for it keep their numbers. The widget itself measures
 * with StaticLayoutMeasure, tested in StaticLayoutMeasureTest.
 */

/** Estimated glyph width as a fraction of the text size. */
private const val CHAR_WIDTH_EM = 0.55f

/** Estimated line height as a multiple of the text size. */
private const val LINE_HEIGHT = 1.2f

/**
 * Sizes lines with fixed constants: 0.55 em per character and a 1.2 line height.
 *
 * @param scale converts sp to dp: `{ it }` is font scale 1.0, `{ it * 2 }` a linear 2.0.
 */
internal class EstimateMeasure(private val scale: (Float) -> Float) : LineMeasure {
    /**
     * Converts sp to dp with the given scale.
     *
     * @param sp the text size in sp.
     * @return the size in dp.
     */
    override fun spToDp(sp: Float): Float = scale(sp)

    /**
     * Estimates the line's rows from its character count and multiplies by the line height.
     *
     * @param spec the line.
     * @param widthDp the width its text may take, in dp.
     * @return the estimated height in dp.
     */
    override fun heightDp(spec: LineSpec, widthDp: Float): Float {
        val size = scale(spec.textSp)
        val textW = spec.text.length * CHAR_WIDTH_EM * size
        val rows = max(1, ceil(textW / widthDp).toInt())
        return rows * size * LINE_HEIGHT
    }
}
