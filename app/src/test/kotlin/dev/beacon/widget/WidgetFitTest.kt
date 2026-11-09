package dev.beacon.widget

import dev.beacon.widget.WidgetLine.RESUME_TIME
import dev.beacon.widget.WidgetLine.STATE
import dev.beacon.widget.WidgetLine.TAP_HINT
import dev.beacon.widget.WidgetLine.TITLE
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for [fitLines]. `spToDp = { it }` is font scale 1.0; `{ it * 2 }` is a linear 2.0. */
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
        assertEquals(WidgetFit(setOf(TITLE, STATE, TAP_HINT), true), fitLines(on(), 350f, 200f) { it })
        assertEquals(
            WidgetFit(setOf(TITLE, STATE, RESUME_TIME, TAP_HINT), true),
            fitLines(pausedTimed(), 350f, 200f) { it },
        )
    }

    /** A little less height drops the tap hint first. */
    @Test fun fitDropsTheTapHintFirst() =
        assertEquals(WidgetFit(setOf(TITLE, STATE, RESUME_TIME), true), fitLines(pausedTimed(), 350f, 180f) { it })

    /** At the largest font only the state word is kept. */
    @Test fun fitAtLargestFontKeepsOnlyTheState() =
        assertEquals(WidgetFit(setOf(STATE), true), fitLines(on(), 350f, 200f) { it * 2 })

    /** The icon goes before the state word would wrap. */
    @Test fun fitDropsTheIconBeforeTheStateWraps() =
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fitLines(pausedTimed(), 232f, 160f) { it })

    /** The state and the resume time stay even when they cannot fit. */
    @Test fun fitNeverDropsStateOrResumeTime() =
        assertEquals(WidgetFit(setOf(STATE, RESUME_TIME), false), fitLines(pausedTimed(), 250f, 110f) { it })
}
