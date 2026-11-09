package dev.beacon.ui

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Why Robolectric: the format comes from the phone's 12/24-hour setting,
 * which is a real system setting the test switches.
 */

/** Tests for [formatResumeTime]. */
@RunWith(AndroidJUnit4::class)
class ResumeTimeTest {
    /** The test application. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The zone before the test, restored afterwards. */
    private val savedZone: TimeZone = TimeZone.getDefault()

    /** 14:30 UTC on 2026-11-09. */
    private val at1430Utc = 1_794_234_600_000L

    /** Pins the zone to UTC so 14:30 is 14:30. */
    @Before fun useUtc() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    /** Puts the zone back. */
    @After fun restoreZone() = TimeZone.setDefault(savedZone)

    /** The result follows the 12/24-hour setting. */
    @Test fun resumeTimeFollowsTheClockSetting() {
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "24")
        assertEquals("14:30", plainSpaces(formatResumeTime(app, at1430Utc)))
        Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
        assertEquals("2:30 PM", plainSpaces(formatResumeTime(app, at1430Utc)))
    }

    /** Newer ICU puts a narrow no-break space before "PM"; compare with plain spaces. */
    private fun plainSpaces(text: String): String = text.replace(' ', ' ').replace(' ', ' ')
}
