package dev.beacon.alert

import android.app.Application
import android.app.KeyguardManager
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * Why these three cases: the owner decided on 11 November 2026 that a real
 * alert flashes only when the screen is off or the lock screen is showing.
 * Only a screen that is on and unlocked counts as the phone being in use.
 */

/** Robolectric tests for [phoneInUse]. */
@RunWith(AndroidJUnit4::class)
class PhoneUseTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /**
     * Sets the screen and the lock screen as Android would report them.
     *
     * @param screenOn whether the screen is on (interactive).
     * @param locked whether the lock screen is showing.
     */
    private fun phone(screenOn: Boolean, locked: Boolean) {
        shadowOf(app.getSystemService(PowerManager::class.java)).turnScreenOn(screenOn)
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(locked)
    }

    /** Screen on and unlocked: the person is using the phone. */
    @Test fun screenOnAndUnlockedIsInUse() {
        phone(screenOn = true, locked = false)
        assertTrue(phoneInUse(app))
    }

    /** Screen on but showing the lock screen: not in use, so an alert flashes. */
    @Test fun lockScreenIsNotInUse() {
        phone(screenOn = true, locked = true)
        assertFalse(phoneInUse(app))
    }

    /** Screen off: not in use, whatever the lock state. */
    @Test fun screenOffIsNotInUse() {
        phone(screenOn = false, locked = false)
        assertFalse(phoneInUse(app))
        phone(screenOn = false, locked = true)
        assertFalse(phoneInUse(app))
    }
}
