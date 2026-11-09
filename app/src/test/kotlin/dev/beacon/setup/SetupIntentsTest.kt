package dev.beacon.setup

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * Why Robolectric: the intents are real framework objects, and the fallback
 * is checked by making the phone refuse every page except App info.
 */

/** Tests for [SetupIntents]. */
@RunWith(AndroidJUnit4::class)
class SetupIntentsTest {
    /** The test application. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Android 11 and later open Beacon's own page; Android 10 opens the list. */
    @Test fun notificationsOnAndroid11Plus() {
        val detail = SetupIntents.intentFor(SetupItem.NOTIFICATIONS, app, sdkInt = 30)
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS, detail.action)
        assertEquals(
            "dev.beacon/dev.beacon.alert.BeaconListener",
            detail.getStringExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME),
        )
        val list = SetupIntents.intentFor(SetupItem.NOTIFICATIONS, app, sdkInt = 29)
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, list.action)
    }

    /** The overlay row opens Beacon's "Display over other apps" page. */
    @Test fun overlayPage() {
        val intent = SetupIntents.intentFor(SetupItem.OVERLAY, app)
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertEquals("package:dev.beacon", intent.data.toString())
    }

    /** The battery row opens Beacon's App info page. */
    @Test fun batteryPage() {
        val intent = SetupIntents.intentFor(SetupItem.BATTERY, app)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:dev.beacon", intent.data.toString())
    }

    /** The maker's name is matched ignoring case. */
    @Test fun xiaomiNames() {
        assertTrue(SetupIntents.isXiaomi("Xiaomi"))
        assertTrue(SetupIntents.isXiaomi("xiaomi"))
        assertFalse(SetupIntents.isXiaomi("Google"))
    }

    /** With only App info available, opening the overlay page starts App info. */
    @Test fun missingPageFallsBackToAppInfo() {
        shadowOf(app).checkActivities(true)
        // Only an App info activity exists (the non-deprecated way: an activity with an intent filter).
        val appInfo = ComponentName("com.android.settings", "com.android.settings.AppInfo")
        val filter = IntentFilter(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addDataScheme("package")
        }
        shadowOf(app.packageManager).addActivityIfNotPresent(appInfo)
        shadowOf(app.packageManager).addIntentFilterForActivity(appInfo, filter)

        SetupIntents.open(SetupItem.OVERLAY, app)

        val started = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
        assertNull(shadowOf(app).nextStartedActivity)
    }

    /** With no page at all, opening does nothing and does not crash. */
    @Test fun nothingResolvableDoesNothing() {
        shadowOf(app).checkActivities(true)
        SetupIntents.open(SetupItem.BATTERY, app)
        assertNull(shadowOf(app).nextStartedActivity)
    }
}
