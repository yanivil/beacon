package dev.beacon.setup

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSettings

/*
 * Why Robolectric: the three checks are real system calls (secure settings,
 * the overlay permission, the power manager), which Robolectric's shadows let
 * a test switch on one at a time.
 */

/** Tests for [SetupChecker] and [SetupStatus]. */
@RunWith(AndroidJUnit4::class)
class SetupCheckerTest {
    /** The test application. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Nothing granted yet: all three are off. */
    @Test fun allOffAtFirst() {
        assertEquals(SetupStatus(false, false, false), SetupChecker(app).check())
    }

    /** Each grant turns on its own field only, and all three make the setup complete. */
    @Test fun readsEachGrant() {
        val checker = SetupChecker(app)

        Settings.Secure.putString(
            app.contentResolver,
            "enabled_notification_listeners",
            "dev.beacon/dev.beacon.alert.BeaconListener",
        )
        assertEquals(SetupStatus(true, false, false), checker.check())

        ShadowSettings.setCanDrawOverlays(true)
        assertEquals(SetupStatus(true, true, false), checker.check())

        val power = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIgnoringBatteryOptimizations("dev.beacon", true)
        val all = checker.check()
        assertEquals(SetupStatus(true, true, true), all)
        assertTrue(all.complete)
    }

    /** Each grant alone leaves the setup incomplete, and isOn reads the matching field. */
    @Test fun isOnFollowsEachField() {
        val status = SetupStatus(notificationAccess = true, displayOverApps = false, batteryUnrestricted = true)
        assertTrue(status.isOn(SetupItem.NOTIFICATIONS))
        assertFalse(status.isOn(SetupItem.OVERLAY))
        assertTrue(status.isOn(SetupItem.BATTERY))
        assertFalse(status.complete)
    }

    /** On a Xiaomi, Autostart OFF keeps the card open; ON, an unknown state and other phones don't. */
    @Test fun autostartOffKeepsTheCardOpen() {
        val granted = SetupStatus(true, true, true)
        assertTrue(granted.complete)
        assertFalse(granted.copy(autostart = AutostartState.OFF).complete)
        assertTrue(granted.copy(autostart = AutostartState.ON).complete)
        assertTrue(granted.copy(autostart = AutostartState.UNKNOWN).complete)
    }

    /** isOn reads Autostart, and the row is shown only on a Xiaomi. */
    @Test fun autostartRowOnlyOnXiaomi() {
        val status = SetupStatus(false, false, false)
        assertFalse(status.shows(SetupItem.AUTOSTART))
        assertTrue(status.shows(SetupItem.BATTERY))
        assertTrue(status.copy(autostart = AutostartState.OFF).shows(SetupItem.AUTOSTART))
        assertTrue(status.copy(autostart = AutostartState.UNKNOWN).shows(SetupItem.AUTOSTART))
        assertTrue(status.copy(autostart = AutostartState.ON).isOn(SetupItem.AUTOSTART))
        assertFalse(status.copy(autostart = AutostartState.OFF).isOn(SetupItem.AUTOSTART))
    }

    /** The checker on a phone that isn't a Xiaomi (Robolectric's) reports no Autostart. */
    @Test fun checkerReportsNoAutostartOnOtherPhones() {
        assertEquals(AutostartState.NOT_XIAOMI, SetupChecker(app).check().autostart)
    }
}
