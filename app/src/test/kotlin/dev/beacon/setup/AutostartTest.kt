package dev.beacon.setup

import android.app.AppOpsManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Why these tests: after a restart, HyperOS with Autostart off keeps Beacon's
 * listener unbound until Beacon is opened (the phone checks' P5). Xiaomi keeps
 * Autostart in a private app-op, 10008, which an app can only read through a
 * hidden Android method. So the state is ON or OFF when that read works, and
 * UNKNOWN when it doesn't; other phones don't have the setting at all.
 */

/** Tests for reading Xiaomi's Autostart state. */
@RunWith(AndroidJUnit4::class)
class AutostartTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** A phone that isn't a Xiaomi has no Autostart setting, and isn't asked for one. */
    @Test fun otherPhonesHaveNoAutostart() {
        var asked = false
        assertEquals(AutostartState.NOT_XIAOMI, autostartState("Google") { asked = true; AppOpsManager.MODE_ALLOWED })
        assertFalse(asked)
    }

    /** On a Xiaomi, an allowed app-op is ON and an ignored one is OFF, whatever the maker's letter case. */
    @Test fun xiaomiAllowedIsOnIgnoredIsOff() {
        assertEquals(AutostartState.ON, autostartState("Xiaomi") { AppOpsManager.MODE_ALLOWED })
        assertEquals(AutostartState.OFF, autostartState("XIAOMI") { AppOpsManager.MODE_IGNORED })
    }

    /** A mode that can't be read, or one that isn't allowed or ignored, is UNKNOWN. */
    @Test fun unreadableIsUnknown() {
        assertEquals(AutostartState.UNKNOWN, autostartState("Xiaomi") { null })
        assertEquals(AutostartState.UNKNOWN, autostartState("Xiaomi") { AppOpsManager.MODE_DEFAULT })
        assertEquals(AutostartState.UNKNOWN, autostartState("Xiaomi") { AppOpsManager.MODE_ERRORED })
    }

    /** Reading Xiaomi's private app-op never throws, even where it doesn't exist (Robolectric's Android). */
    @Test fun readingNeverThrows() {
        hiddenOpMode(app, MIUI_OP_AUTO_START)
    }
}
