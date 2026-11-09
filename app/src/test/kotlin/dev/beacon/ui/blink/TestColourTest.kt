package dev.beacon.ui.blink

import dev.beacon.alert.MessageSource
import dev.beacon.settings.AppSetting
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.ColourMode
import dev.beacon.settings.FlashColour
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why these cases: in per-app mode the test must flash the colour of the app
 * the user is looking at (spec section 8). Each fallback step (the open
 * colour card, the first app that is ON, Texts) gets its own case, with
 * settings where the steps would give different answers.
 */

/** Plain JVM tests for [testColour]. */
class TestColourTest {
    /** Per-app mode with the default apps: SMS and WhatsApp ON, Telegram OFF. */
    private val perApp = BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP)

    /** Same-colour mode flashes the global colour and names no app, even while a card is open. */
    @Test fun sameColourMode() {
        assertEquals(FlashColour.YELLOW to null, testColour(BeaconSettings.DEFAULT, MessageSource.WHATSAPP))
    }

    /** Per-app mode uses the app whose colour card is open, even when that app is OFF. */
    @Test fun perAppUsesEditingApp() {
        assertEquals(FlashColour.BLUE to MessageSource.TELEGRAM, testColour(perApp, MessageSource.TELEGRAM))
    }

    /** With no card open, per-app mode uses the first app that is ON. */
    @Test fun perAppFirstOnApp() {
        assertEquals(FlashColour.YELLOW to MessageSource.SMS, testColour(perApp, null))
    }

    /** An app that is OFF is skipped: with Texts OFF, WhatsApp is the first app that is ON. */
    @Test fun perAppSkipsAppsThatAreOff() {
        val smsOff = perApp.copy(apps = perApp.apps + (MessageSource.SMS to AppSetting(false, FlashColour.YELLOW)))
        assertEquals(FlashColour.GREEN to MessageSource.WHATSAPP, testColour(smsOff, null))
    }

    /** With every app OFF and no card open, the test falls back to Texts and its own colour. */
    @Test fun perAppNoneOnFallsBackToSms() {
        // A non-default Texts colour and global colour, so the result can only come from Texts' own setting.
        val noneOn = perApp.copy(
            globalColour = FlashColour.WHITE,
            apps = MessageSource.entries.associateWith { AppSetting(false, it.defaultColour) } +
                (MessageSource.SMS to AppSetting(false, FlashColour.RED)),
        )
        assertEquals(FlashColour.RED to MessageSource.SMS, testColour(noneOn, null))
    }
}
