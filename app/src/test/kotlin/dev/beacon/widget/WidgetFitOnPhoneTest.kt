package dev.beacon.widget

import dev.beacon.widget.WidgetLine.RESUME_TIME
import dev.beacon.widget.WidgetLine.STATE
import dev.beacon.widget.WidgetLine.TAP_HINT
import dev.beacon.widget.WidgetLine.TITLE
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why the test phone's own numbers: on the Redmi 15C 5G, at the owner's text
 * size (1.45) and display size, the 4 × 2 widget is 301.83 × 154.69 dp, and
 * it showed only "PAUSED": the resume time didn't fit (the phone checks'
 * P4). The heights below are what the widget's own StaticLayout measure
 * returned on that phone (a diagnostic build logged them), so these cases
 * pin the fit to the phone the owner looked at, not to an estimate.
 */

/** Widget size on the test phone at 4 × 2, in dp. */
private const val PHONE_W = 301.83f
private const val PHONE_H = 154.69f

/**
 * Line heights the test phone measured, in dp, margin included. PAUSED at 52 sp is its drawn box
 * (69.5 dp) plus the measure's 2 dp margin; the resume line wraps to two rows at this width.
 */
private class PhoneMeasure : LineMeasure {
    /**
     * Converts sp to dp at the phone's text size, for the icon's width only.
     *
     * @param sp the text size in sp.
     * @return the size in dp.
     */
    override fun spToDp(sp: Float): Float = sp * 1.45f

    /**
     * Returns the height the phone measured for this line.
     *
     * @param spec the line.
     * @param widthDp unused: the phone's heights are for the widget's own width.
     * @return the height in dp.
     * @throws IllegalArgumentException for a state size the phone didn't measure.
     */
    override fun heightDp(spec: LineSpec, widthDp: Float): Float = when (spec.line) {
        TITLE -> 41.0f
        RESUME_TIME -> 69.1f
        TAP_HINT -> 37.7f
        STATE -> when (spec.textSp) {
            40f -> 55.3f
            48f -> 66.3f
            52f -> 71.5f
            else -> throw IllegalArgumentException("no phone height for ${spec.textSp} sp")
        }
    }
}

/** The fit at the test phone's widget size and text size, with the phone's own line heights. */
class WidgetFitOnPhoneTest {
    /** A timed pause shows the word and the resume time, as the owner chose from the phone (phone checks P4). */
    @Test fun pausedShowsTheResumeTimeOnThePhone() {
        val lines = listOf(
            LineSpec(TITLE, "Visual alerts", 28f, false),
            LineSpec(STATE, "PAUSED", STATE_PAUSED_SP, true),
            LineSpec(RESUME_TIME, "Back ON by itself at 12:54", 22f, false),
            LineSpec(TAP_HINT, "Tap to turn ON now", 22f, false),
        )
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), true), fitLines(lines, PHONE_W, PHONE_H, PhoneMeasure()))
    }

    /** ON shows its title and word, as it did on the phone after the restart. */
    @Test fun onShowsTitleAndWordOnThePhone() {
        val lines = listOf(
            LineSpec(TITLE, "Visual alerts", 28f, false),
            LineSpec(STATE, "ON", STATE_ON_SP, true),
            LineSpec(TAP_HINT, "Tap to pause for 1 hour", 22f, false),
        )
        assertEquals(WidgetFit(setOf(TITLE, STATE), true), fitLines(lines, PHONE_W, PHONE_H, PhoneMeasure()))
    }
}
