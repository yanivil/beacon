package dev.beacon.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test: proves the project, manifest, theme and Robolectric are wired
 * together well enough for the launcher activity to start.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    /** The launcher activity starts and is not finishing itself right away. */
    @Test
    fun launches() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.onActivity { assertFalse(it.isFinishing) }
        }
    }
}
