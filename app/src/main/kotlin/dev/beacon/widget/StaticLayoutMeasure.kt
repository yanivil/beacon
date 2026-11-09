package dev.beacon.widget

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.TypedValue
import kotlin.math.floor
import kotlin.math.max

/*
 * Why StaticLayout: the launcher draws each of the widget's lines in a
 * TextView, and a TextView lays its text out with StaticLayout. Measuring
 * with the same class, at the same text size, weight and width, gives the
 * height the TextView will take: its font padding, its real line spacing
 * and its real wrapping, including a word broken because it is wider than
 * the line. The fixed estimate this replaces (0.55 em per character) was
 * too small for the bold capitals of "PAUSED" (PR 3's D3, ruling F5).
 */

/** Extra height allowed for each measured line, in dp, against rounding between this measure and the launcher. */
private const val LINE_MARGIN_DP = 2f

/**
 * Measures the widget's lines as the launcher's TextViews will draw them.
 *
 * @param metrics the display metrics, read at each call so the current font size and density are used.
 */
internal class StaticLayoutMeasure(private val metrics: DisplayMetrics) : LineMeasure {
    /**
     * Converts a text size to dp at the phone's current font size.
     *
     * @param sp the text size in sp.
     * @return the size in dp. On Android 14 and later, applyDimension includes the non-linear font scaling.
     */
    override fun spToDp(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics) / metrics.density

    /**
     * Lays the line's text out in [widthDp] and returns its height.
     *
     * @param spec the line; the title and the state word are bold, as the widget draws them.
     * @param widthDp the width its text may take, in dp; zero or less is measured as 1 px.
     * @return the laid-out height in dp, font padding included, plus a small margin.
     */
    override fun heightDp(spec: LineSpec, widthDp: Float): Float {
        val density = metrics.density
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            // The same pixels a TextView gets from a size in sp.
            textSize = spToDp(spec.textSp) * density
            typeface = if (spec.line == WidgetLine.TITLE || spec.line == WidgetLine.STATE) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        // Rounded down, so the measure never has more room than the view. StaticLayout.Builder throws
        // on a width below 1 px, which an icon wider than the space left can cause.
        val widthPx = max(1, floor(widthDp * density).toInt())
        val layout = StaticLayout.Builder.obtain(spec.text, 0, spec.text.length, paint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            // A TextView's defaults: font padding included, high-quality line breaks.
            .setIncludePad(true)
            // LineBreaker's constant (API 29, Beacon's minimum) is what lint expects; it has the same value
            // as Layout.BREAK_STRATEGY_HIGH_QUALITY.
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
            .build()
        return layout.height / density + LINE_MARGIN_DP
    }
}
