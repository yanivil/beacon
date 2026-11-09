package dev.beacon.widget

import dev.beacon.settings.Pause
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for [widgetStateFor]. */
class WidgetStateTest {
    /** The state follows the pause and the clock; a timed pause ends exactly at its deadline. */
    @Test fun stateFollowsTheClock() {
        assertEquals(WidgetState.On, widgetStateFor(Pause.None, 0))
        assertEquals(WidgetState.Paused(null), widgetStateFor(Pause.Indefinite, 0))
        assertEquals(WidgetState.Paused(5_000L), widgetStateFor(Pause.Until(5_000), 4_999))
        assertEquals(WidgetState.On, widgetStateFor(Pause.Until(5_000), 5_000))
    }
}
