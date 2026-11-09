package dev.beacon.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the plain pause rules: the widget tap, expiry and the resume time. */
class PauseTest {
    /** A tap while alerts are on pauses them for an hour. */
    @Test fun widgetTapWhileOnPausesForAnHour() =
        assertEquals(Pause.Until(1_000 + 3_600_000), pauseAfterWidgetTap(Pause.None, 1_000))

    /** A tap while a pause is in effect turns alerts back on. */
    @Test fun widgetTapWhilePausedTurnsOn() {
        assertEquals(Pause.None, pauseAfterWidgetTap(Pause.Indefinite, 1_000))
        assertEquals(Pause.None, pauseAfterWidgetTap(Pause.Until(5_000), 4_999))
    }

    /** An expired timed pause counts as ON, so a tap pauses again (spec section 5). */
    @Test fun widgetTapAfterTheDeadlinePausesAgain() =
        assertEquals(Pause.Until(5_000 + 3_600_000), pauseAfterWidgetTap(Pause.Until(5_000), 5_000))

    /** A timed pause is expired exactly at its deadline; other pauses never expire. */
    @Test fun expiryEdge() {
        assertFalse(Pause.Until(5_000).isExpiredAt(4_999))
        assertTrue(Pause.Until(5_000).isExpiredAt(5_000))
        assertFalse(Pause.Indefinite.isExpiredAt(Long.MAX_VALUE))
    }

    /** Only a timed pause has a resume time. */
    @Test fun resumeAt() {
        assertEquals(5_000L, Pause.Until(5_000).resumeAtMillis())
        assertNull(Pause.Indefinite.resumeAtMillis())
        assertNull(Pause.None.resumeAtMillis())
    }
}
