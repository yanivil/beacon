package dev.beacon.widget

import kotlin.math.ceil
import kotlin.math.max

/*
 * Why an estimate: Glance cannot measure text, but the widget must never clip
 * its words. So the fit is worked out beforehand from fixed constants: 0.55 em
 * per character, 1.2 line height, 16 dp padding, 4 dp gaps, and an icon as
 * wide as its text plus 8 dp. Lines are dropped in a fixed order (tap hint,
 * title, then the decorative icon). The state word and the resume time are
 * never dropped: the widget's spoken label carries every line anyway.
 */

/** The widget's lines, in the order they are shown. */
enum class WidgetLine { TITLE, STATE, RESUME_TIME, TAP_HINT }

/**
 * One line to be fitted.
 *
 * @property line which line this is.
 * @property text the words on it.
 * @property textSp its text size in sp.
 * @property hasIcon true if an icon sits before the text.
 */
data class LineSpec(val line: WidgetLine, val text: String, val textSp: Float, val hasIcon: Boolean)

/**
 * Which lines to show.
 *
 * @property lines the lines that fit.
 * @property stateIcon true if the state's icon is shown.
 */
data class WidgetFit(val lines: Set<WidgetLine>, val stateIcon: Boolean)

/** Padding on each side of the widget, in dp. */
private const val PADDING_DP = 16f

/** Gap between lines, in dp. */
private const val GAP_DP = 4f

/** Estimated glyph width as a fraction of the text size. */
private const val CHAR_WIDTH_EM = 0.55f

/** Line height as a multiple of the text size. */
private const val LINE_HEIGHT = 1.2f

/** Space between an icon and its text, in dp. */
private const val ICON_GAP_DP = 8f

/**
 * Chooses the lines that fit in the widget.
 *
 * @param lines every line wanted, in display order.
 * @param widthDp the widget's width in dp.
 * @param heightDp the widget's height in dp.
 * @param spToDp converts sp to dp at the user's font size.
 * @return the kept lines and whether the state icon stays.
 */
fun fitLines(lines: List<LineSpec>, widthDp: Float, heightDp: Float, spToDp: (Float) -> Float): WidgetFit {
    val innerW = widthDp - 2 * PADDING_DP
    val innerH = heightDp - 2 * PADDING_DP
    val kept = lines.map { it.line }.toMutableSet()
    var icon = true

    /** Height of one line, with its text wrapped to the width left after its icon. */
    fun height(spec: LineSpec): Float {
        val size = spToDp(spec.textSp)
        val iconW = if (spec.hasIcon && icon) size + ICON_GAP_DP else 0f
        val textW = spec.text.length * CHAR_WIDTH_EM * size
        val rows = max(1, ceil(textW / (innerW - iconW)).toInt())
        return rows * size * LINE_HEIGHT
    }

    /** Height of everything still kept, with the gaps between. */
    fun total(): Float =
        lines.filter { it.line in kept }.sumOf { height(it).toDouble() }.toFloat() + GAP_DP * (kept.size - 1)

    while (total() > innerH) {
        when {
            WidgetLine.TAP_HINT in kept -> kept.remove(WidgetLine.TAP_HINT)
            WidgetLine.TITLE in kept -> kept.remove(WidgetLine.TITLE)
            icon -> icon = false
            // STATE and RESUME_TIME stay even if they still do not fit.
            else -> break
        }
    }
    return WidgetFit(kept, icon)
}
