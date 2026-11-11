package dev.beacon.widget

import dev.beacon.widget.WidgetLine.RESUME_TIME
import dev.beacon.widget.WidgetLine.STATE
import dev.beacon.widget.WidgetLine.TAP_HINT
import dev.beacon.widget.WidgetLine.TITLE
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why an estimate here: these cases test the fit's rules (the drop order,
 * the icon, the kept state word) with the fixed estimate the fit used before
 * ruling F5, so their numbers stay as they were. The widget's real measure
 * is tested at the emulator's sizes in StaticLayoutMeasureTest. The lines
 * here keep the design's 64 and 52 sp state words on purpose: they test the
 * rules, not the widget's current sizes (WidgetFitOnPhoneTest does that), and
 * the numbers in the comments below are for those sizes.
 */

/** Tests for [fitLines] with [EstimateMeasure]. `{ it }` is font scale 1.0; `{ it * 2 }` is a linear 2.0. */
class WidgetFitTest {
    /** The ON widget's lines. */
    private fun on() = listOf(
        LineSpec(TITLE, "Visual alerts", 28f, false),
        LineSpec(STATE, "ON", 64f, true),
        LineSpec(TAP_HINT, "Tap to pause for 1 hour", 22f, false),
    )

    /** The timed-pause widget's lines. */
    private fun pausedTimed() = listOf(
        LineSpec(TITLE, "Visual alerts", 28f, false),
        LineSpec(STATE, "PAUSED", 52f, true),
        LineSpec(RESUME_TIME, "Back ON by itself at 10:30", 22f, false),
        LineSpec(TAP_HINT, "Tap to turn ON now", 22f, false),
    )

    /** At the default size and font, every line and the icon fit. */
    @Test fun fitDefaultSizeShowsEverything() {
        assertEquals(WidgetFit(setOf(TITLE, STATE, TAP_HINT), true), fitLines(on(), 350f, 200f, EstimateMeasure { it }))
        assertEquals(
            WidgetFit(setOf(TITLE, STATE, RESUME_TIME, TAP_HINT), true),
            fitLines(pausedTimed(), 350f, 200f, EstimateMeasure { it }),
        )
    }

    /** A little less height drops the tap hint first. */
    @Test fun fitDropsTheTapHintFirst() =
        assertEquals(WidgetFit(setOf(TITLE, STATE, RESUME_TIME), true), fitLines(pausedTimed(), 350f, 180f, EstimateMeasure { it }))

    /** At the largest font only the state word is kept. */
    @Test fun fitAtLargestFontKeepsOnlyTheState() =
        assertEquals(WidgetFit(setOf(STATE), true), fitLines(on(), 350f, 200f, EstimateMeasure { it * 2 }))

    /** The icon goes before the state word would wrap. */
    @Test fun fitDropsTheIconBeforeTheStateWraps() =
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fitLines(pausedTimed(), 232f, 160f, EstimateMeasure { it }))

    /**
     * At the smallest height the resume time goes last, after the icon, and the state word stays
     * (ruling F4): 62.4 dp of "PAUSED" and 52.8 dp of resume line don't fit together in 78 dp.
     */
    @Test fun fitDropsTheResumeTimeLastAndKeepsTheState() =
        assertEquals(WidgetFit(setOf(STATE), false), fitLines(pausedTimed(), 250f, 110f, EstimateMeasure { it }))

    /** With room for the state word and the resume time together, the resume time stays once the icon has gone. */
    @Test fun fitKeepsTheResumeTimeWhenItFitsBesideTheState() =
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fitLines(pausedTimed(), 250f, 152f, EstimateMeasure { it }))

    /** The state word is never dropped, even when it alone doesn't fit. */
    @Test fun fitKeepsTheStateEvenWhenItAloneDoesNotFit() =
        assertEquals(WidgetFit(setOf(STATE), false), fitLines(pausedTimed(), 250f, 60f, EstimateMeasure { it }))
}
