package dev.beacon.alert

import android.app.Application
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPowerManager
import org.robolectric.shadows.ShadowSettings

/** Robolectric tests for [AndroidAlertPlatform]. */
@RunWith(AndroidJUnit4::class)
class AndroidAlertPlatformTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val platform = AndroidAlertPlatform(app)

    /** Pins that the wake lock is held after a hold and freed after a release. */
    @Test fun wakeLockHeldAndReleased() {
        platform.holdWakeLock(70_000)
        val lock = ShadowPowerManager.getLatestWakeLock()
        assertNotNull(lock); assertTrue(lock.isHeld)
        platform.releaseWakeLock(); assertFalse(lock.isHeld)
    }

    /** Pins that the overlay permission follows the system setting. */
    @Test fun overlayPermissionFollowsSettings() {
        ShadowSettings.setCanDrawOverlays(true); assertTrue(platform.canShowOverOtherApps())
        ShadowSettings.setCanDrawOverlays(false); assertFalse(platform.canShowOverOtherApps())
    }

    /** Pins that only SEVERE or worse counts as too hot. */
    @Test fun tooHotFromSevere() {
        val shadow = shadowOf(app.getSystemService(PowerManager::class.java))
        shadow.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_MODERATE); assertFalse(platform.isTooHot())
        shadow.setCurrentThermalStatus(PowerManager.THERMAL_STATUS_SEVERE); assertTrue(platform.isTooHot())
    }

    /** Pins that the alert window starts in a new task. */
    @Test fun showsAlertActivityInNewTask() {
        platform.showAlertScreen()
        val intent: Intent = shadowOf(app).nextStartedActivity
        assertEquals(AlertActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    /** Pins that Robolectric's refusal of the thermal listener is reported as false, not thrown. */
    @Test fun refusedHeatWatchReturnsFalse() {
        // Robolectric 4.17 has no thermal service, so addThermalStatusListener fails.
        assertFalse(platform.watchHeat {})
        platform.stopWatchingHeat()
    }
}
