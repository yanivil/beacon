package dev.beacon.widget

/*
 * Why a fit worked out beforehand: a widget can't scroll, and Glance can't
 * ask how tall a line will be drawn. So the lines are measured first, with
 * the widget's real size and the user's font size, and only the lines that
 * fit are drawn. The measuring is passed in: the widget measures the text
 * with StaticLayout as the launcher's TextView will lay it out (ruling F5),
 * and the pure tests pass a simple estimate.
 *
 * Lines are dropped in a fixed order (ruling F4, which amends ruling 7):
 * the tap hint, the title, the decorative state icon, then the resume time.
 * The state word is never dropped, and stays even if it alone doesn't fit.
 * A dropped resume time is still spoken: the widget's one spoken label
 * carries every line.
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

/** How the fit sizes text: sp to dp at the user's font size, and the height a line is drawn at. */
interface LineMeasure {
    /**
     * Converts a text size to dp at the user's font size.
     *
     * @param sp the text size in sp.
     * @return the size in dp; the state icon is drawn this tall and this wide.
     */
    fun spToDp(sp: Float): Float

    /**
     * Measures how tall a line is drawn when its text wraps to a width.
     *
     * @param spec the line.
     * @param widthDp the width its text may take, in dp; may be zero or less when an icon takes it all.
     * @return the height in dp, every wrapped row included.
     */
    fun heightDp(spec: LineSpec, widthDp: Float): Float
}

/** Padding on each side of the widget, in dp; WidgetContent pads by the same. */
internal const val WIDGET_PADDING_DP = 16f

/** Gap between lines, in dp; WidgetContent draws the same gap. */
internal const val LINE_GAP_DP = 4f

/** Space between the state icon and its word, in dp; WidgetContent draws the same space. */
internal const val ICON_GAP_DP = 8f

/**
 * Chooses the lines that fit in the widget.
 *
 * @param lines every line wanted, in display order.
 * @param widthDp the widget's width in dp.
 * @param heightDp the widget's height in dp.
 * @param measure sizes each line's text.
 * @return the kept lines and whether the state icon stays.
 */
fun fitLines(lines: List<LineSpec>, widthDp: Float, heightDp: Float, measure: LineMeasure): WidgetFit {
    val innerW = widthDp - 2 * WIDGET_PADDING_DP
    val innerH = heightDp - 2 * WIDGET_PADDING_DP
    val kept = lines.map { it.line }.toMutableSet()
    var icon = true

    /** Height of one line, with its text wrapped to the width left after its icon. */
    fun height(spec: LineSpec): Float {
        // The icon is as wide as the text is tall (a square), plus the gap before the word.
        val iconW = if (spec.hasIcon && icon) measure.spToDp(spec.textSp) + ICON_GAP_DP else 0f
        return measure.heightDp(spec, innerW - iconW)
    }

    /** Height of everything still kept, with the gaps between. */
    fun total(): Float =
        lines.filter { it.line in kept }.sumOf { height(it).toDouble() }.toFloat() + LINE_GAP_DP * (kept.size - 1)

    while (total() > innerH) {
        when {
            WidgetLine.TAP_HINT in kept -> kept.remove(WidgetLine.TAP_HINT)
            WidgetLine.TITLE in kept -> kept.remove(WidgetLine.TITLE)
            icon -> icon = false
            // Drawn only: the spoken label still names the resume time.
            WidgetLine.RESUME_TIME in kept -> kept.remove(WidgetLine.RESUME_TIME)
            // The state word alone: it stays even if it still doesn't fit.
            else -> break
        }
    }
    return WidgetFit(kept, icon)
}
