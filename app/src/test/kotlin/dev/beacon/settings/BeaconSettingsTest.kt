package dev.beacon.settings

import dev.beacon.alert.MessageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the default settings, colour resolution, alert length and pause rules. */
class BeaconSettingsTest {
    /** The defaults match section 5 of the Step 2 spec. */
    @Test
    fun defaultsMatchSpec() {
        val d = BeaconSettings.DEFAULT
        assertEquals(Pause.None, d.pause)
        assertEquals(AppSetting(true, FlashColour.YELLOW), d.apps[MessageSource.SMS])
        assertEquals(AppSetting(true, FlashColour.GREEN), d.apps[MessageSource.WHATSAPP])
        assertEquals(AppSetting(false, FlashColour.BLUE), d.apps[MessageSource.TELEGRAM])
        assertEquals(ColourMode.SAME_FOR_ALL, d.colourMode)
        assertEquals(FlashColour.YELLOW, d.globalColour)
        assertEquals(DurationChoice.ONE_MINUTE, d.duration)
        assertEquals(10, d.customLength.minutes)
        assertEquals(BlinkSpeed.MEDIUM, d.speed)
    }

    /** The colour comes from the global colour or the app's own, by mode. */
    @Test
    fun colourForFollowsMode() {
        val d = BeaconSettings.DEFAULT
        assertEquals(FlashColour.YELLOW, d.colourFor(MessageSource.WHATSAPP))
        assertEquals(
            FlashColour.GREEN,
            d.copy(colourMode = ColourMode.PER_APP).colourFor(MessageSource.WHATSAPP),
        )
    }

    /** Each duration choice maps to its length in milliseconds, or null if unlimited. */
    @Test
    fun alertLengths() {
        val d = BeaconSettings.DEFAULT
        assertEquals(60_000L, d.alertLengthMillis())
        assertEquals(300_000L, d.copy(duration = DurationChoice.FIVE_MINUTES).alertLengthMillis())
        assertEquals(
            900_000L,
            d.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(15)).alertLengthMillis(),
        )
        assertNull(d.copy(duration = DurationChoice.UNTIL_STOPPED).alertLengthMillis())
    }

    /** A timed pause ends exactly at its deadline; indefinite never ends. */
    @Test
    fun pauseEdges() {
        assertFalse(Pause.None.isPausedAt(5))
        assertTrue(Pause.Indefinite.isPausedAt(5))
        assertTrue(Pause.Until(10).isPausedAt(9))
        assertFalse(Pause.Until(10).isPausedAt(10))
    }
}
