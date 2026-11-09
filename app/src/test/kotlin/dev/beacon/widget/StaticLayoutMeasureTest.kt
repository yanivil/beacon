package dev.beacon.widget

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.ui.FontScaleRule
import dev.beacon.widget.WidgetLine.RESUME_TIME
import dev.beacon.widget.WidgetLine.STATE
import dev.beacon.widget.WidgetLine.TAP_HINT
import dev.beacon.widget.WidgetLine.TITLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Why native graphics: Robolectric's default graphics don't measure text.
 * With NATIVE, StaticLayout lays the text out with Android's real fonts, so
 * the widget's measure can be checked through fitLines at the emulator's
 * density (420 dpi) and its two widget sizes: 3 x 2 (266 x 224 dp) and
 * 4 x 2 (360 x 224 dp). At font 1.0 it gives the same heights the emulator's
 * launcher drew (PR 3's checks). The resume time is "12:59 PM", the longest
 * 12-hour time. Each expected fit is the one that draws nothing clipped and
 * no word broken: every kept line fits inside, the state word on one row.
 */

/** The widget's inner size at [widthDp] x [heightDp]: the padding on both sides taken off. */
private fun inner(widthDp: Float, heightDp: Float) = (widthDp - 2 * WIDGET_PADDING_DP) to (heightDp - 2 * WIDGET_PADDING_DP)

/**
 * Checks that the lines [fit] keeps are no taller together than the widget's inside, so nothing is clipped.
 *
 * @param lines every line wanted.
 * @param fit what the fit kept.
 * @param widthDp the widget's width in dp.
 * @param heightDp the widget's height in dp.
 * @param measure the measure the fit used.
 */
private fun assertNothingClipped(lines: List<LineSpec>, fit: WidgetFit, widthDp: Float, heightDp: Float, measure: LineMeasure) {
    val (innerW, innerH) = inner(widthDp, heightDp)
    val kept = lines.filter { it.line in fit.lines }
    val total = kept.sumOf { spec ->
        val iconW = if (spec.hasIcon && fit.stateIcon) measure.spToDp(spec.textSp) + ICON_GAP_DP else 0f
        measure.heightDp(spec, innerW - iconW).toDouble()
    } + LINE_GAP_DP * (kept.size - 1)
    assertTrue("kept lines need $total dp of $innerH dp", total <= innerH)
}

/**
 * Checks that the state word takes one row beside its icon, if any: no "PAUSE" / "D".
 *
 * @param lines every line wanted.
 * @param fit what the fit kept.
 * @param widthDp the widget's width in dp.
 * @param measure the measure the fit used.
 */
private fun assertStateOnOneRow(lines: List<LineSpec>, fit: WidgetFit, widthDp: Float, measure: LineMeasure) {
    val state = lines.single { it.line == STATE }
    val iconW = if (fit.stateIcon) measure.spToDp(state.textSp) + ICON_GAP_DP else 0f
    assertEquals("one row", measure.heightDp(state, 10_000f), measure.heightDp(state, inner(widthDp, 0f).first - iconW), 0.01f)
}

/** Tests for [StaticLayoutMeasure] at font 1.0, through [fitLines], at the emulator's density. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-420dpi")
class StaticLayoutMeasureTest {
    /** The test application, for the strings and the display metrics. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The measure under test. */
    private val measure = StaticLayoutMeasure(app.resources.displayMetrics)

    /** The ON widget's lines. */
    private val on = widgetLines(app, WidgetState.On, null)

    /** The timed-pause widget's lines. */
    private val paused = widgetLines(app, WidgetState.Paused(1L), "12:59 PM")

    /**
     * The heights match what the emulator's launcher drew at 4 x 2, font 1.0, with 2 dp of margin:
     * the title 98 px, "PAUSED" 182 px, the resume line and the tap hint 77 px each, at 2.625 px per dp.
     */
    @Test fun heightsMatchTheEmulator() {
        val (innerW, _) = inner(360f, 224f)
        assertEquals(98 / 2.625f + 2, measure.heightDp(paused[0], innerW), 0.5f)
        assertEquals(182 / 2.625f + 2, measure.heightDp(paused[1], innerW - 52f - ICON_GAP_DP), 0.5f)
        assertEquals(77 / 2.625f + 2, measure.heightDp(paused[2], innerW), 0.5f)
        assertEquals(77 / 2.625f + 2, measure.heightDp(paused[3], innerW), 0.5f)
    }

    /** At 3 x 2, ON keeps every line and the icon. */
    @Test fun on3x2KeepsEverything() {
        val fit = fitLines(on, 266f, 224f, measure)
        assertEquals(WidgetFit(setOf(TITLE, STATE, TAP_HINT), true), fit)
        assertNothingClipped(on, fit, 266f, 224f, measure); assertStateOnOneRow(on, fit, 266f, measure)
    }

    /**
     * At 3 x 2 the timed pause drops the tap hint, the title and the icon, so "PAUSED" takes one row
     * and the resume line wraps whole below it (PR 3's D3).
     */
    @Test fun paused3x2DropsTheIconSoPausedTakesOneRow() {
        val fit = fitLines(paused, 266f, 224f, measure)
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fit)
        assertNothingClipped(paused, fit, 266f, 224f, measure); assertStateOnOneRow(paused, fit, 266f, measure)
    }

    /** At 4 x 2 both states keep every line and the icon. */
    @Test fun bothStates4x2KeepEverything() {
        val onFit = fitLines(on, 360f, 224f, measure)
        assertEquals(WidgetFit(setOf(TITLE, STATE, TAP_HINT), true), onFit)
        assertNothingClipped(on, onFit, 360f, 224f, measure)
        val pausedFit = fitLines(paused, 360f, 224f, measure)
        assertEquals(WidgetFit(setOf(TITLE, STATE, RESUME_TIME, TAP_HINT), true), pausedFit)
        assertNothingClipped(paused, pausedFit, 360f, 224f, measure); assertStateOnOneRow(paused, pausedFit, 360f, measure)
    }

    /** A width of zero or less (an icon wider than the room left) is measured as 1 px instead of throwing. */
    @Test fun aWidthOfZeroOrLessIsMeasured() {
        val oneRow = measure.heightDp(paused[1], 10_000f)
        assertTrue(measure.heightDp(paused[1], 0f) > oneRow)
        assertTrue(measure.heightDp(paused[1], -20f) > oneRow)
    }
}

/** Tests for [StaticLayoutMeasure] at the largest font (2.0, non-linear on Android 14 and later), through [fitLines]. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-420dpi")
class StaticLayoutMeasureLargestFontTest {
    /** Sets the 200% font scale for each test, and resets it after. */
    @get:Rule val font = FontScaleRule(2.0f)

    /** The test application, for the strings and the display metrics. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The measure under test, built inside each test so it reads the 2.0 metrics. */
    private fun measure() = StaticLayoutMeasure(app.resources.displayMetrics)

    /** The 2.0 scale is non-linear: 64 sp is about 68 dp, not 128 dp. */
    @Test fun theScaleIsNonLinear() = assertEquals(68.1f, measure().spToDp(64f), 0.1f)

    /** At 3 x 2 and 4 x 2, ON drops the tap hint and keeps the title and the bolt with "ON". */
    @Test fun onDropsTheTapHint() {
        val m = measure()
        val on = widgetLines(app, WidgetState.On, null)
        for (w in listOf(266f, 360f)) {
            val fit = fitLines(on, w, 224f, m)
            assertEquals("at $w dp", WidgetFit(setOf(TITLE, STATE), true), fit)
            assertNothingClipped(on, fit, w, 224f, m); assertStateOnOneRow(on, fit, w, m)
        }
    }

    /** At 4 x 2 the timed pause keeps the icon with "PAUSED" and the resume line on two rows. */
    @Test fun paused4x2KeepsTheResumeLine() {
        val m = measure()
        val paused = widgetLines(app, WidgetState.Paused(1L), "12:59 PM")
        val fit = fitLines(paused, 360f, 224f, m)
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), true), fit)
        assertNothingClipped(paused, fit, 360f, 224f, m); assertStateOnOneRow(paused, fit, 360f, m)
    }

    /**
     * At 3 x 2 the resume line can't fit beside "PAUSED", so it goes after the icon and only the state
     * word is drawn (ruling F4); the spoken label still names the time.
     */
    @Test fun paused3x2KeepsOnlyTheState() {
        val m = measure()
        val paused = widgetLines(app, WidgetState.Paused(1L), "12:59 PM")
        val fit = fitLines(paused, 266f, 224f, m)
        assertEquals(WidgetFit(setOf(STATE), false), fit)
        assertNothingClipped(paused, fit, 266f, 224f, m); assertStateOnOneRow(paused, fit, 266f, m)
    }
}
