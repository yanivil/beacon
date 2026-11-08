package dev.beacon.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.beacon.alert.MessageSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests that [SettingsCodec] turns stored preferences into settings, with safe fallbacks. */
class SettingsCodecTest {
    /** An empty store decodes to the first-run defaults. */
    @Test fun emptyIsDefault() {
        assertEquals(BeaconSettings.DEFAULT, SettingsCodec.decode(emptyPreferences()))
    }

    /** Unknown names and invalid numbers fall back to the default value field by field. */
    @Test fun unknownNamesFallBack() {
        val p = preferencesOf(
            stringPreferencesKey("speed") to "WARP",
            stringPreferencesKey("global_colour") to "PINK",
            intPreferencesKey("custom_minutes") to 7,
            stringPreferencesKey("pause_kind") to "SOMETIMES",
        )
        val s = SettingsCodec.decode(p)
        assertEquals(BlinkSpeed.MEDIUM, s.speed)
        assertEquals(FlashColour.YELLOW, s.globalColour)
        assertEquals(10, s.customLength.minutes)
        assertEquals(Pause.None, s.pause)
    }

    /** A timed pause with no stored deadline is treated as no pause. */
    @Test fun untilWithoutTimeIsNone() {
        val p = preferencesOf(stringPreferencesKey("pause_kind") to "UNTIL")
        assertEquals(Pause.None, SettingsCodec.decode(p).pause)
    }

    /** A missing per-app key falls back to that app's own default, not another app's. */
    @Test fun missingAppKeysUseThatAppsDefaults() {
        val p = preferencesOf(stringPreferencesKey("app_SMS_colour") to "RED")
        val s = SettingsCodec.decode(p)
        assertEquals(AppSetting(true, FlashColour.RED), s.apps.getValue(MessageSource.SMS))
        assertEquals(
            AppSetting(MessageSource.TELEGRAM.defaultEnabled, MessageSource.TELEGRAM.defaultColour),
            s.apps.getValue(MessageSource.TELEGRAM),
        )
    }
}
