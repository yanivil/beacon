package dev.beacon.ui

import android.app.Activity
import org.junit.Rule
import dev.beacon.widget.NoWidgetRefreshRule
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Smoke test: proves the project, manifest, theme and Robolectric are wired
 * together well enough for the launcher activity to start, and that the
 * system bars always match Beacon's dark page.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    /** Skips the widget refresh that opening Beacon starts. */
    @get:Rule val noWidgets = NoWidgetRefreshRule()

    /** The launcher activity starts and is not finishing itself right away. */
    @Test
    fun launches() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * With the phone in its light theme, the status bar icons and the
     * navigation buttons are still light on Beacon's dark page, with no light
     * scrim behind 3-button navigation (D1).
     */
    @Test
    fun systemBarsAreDarkInTheLightTheme() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.onActivity { activity ->
                // Robolectric's default configuration is the light theme; check it, or the test proves nothing.
                assertEquals(Configuration.UI_MODE_NIGHT_NO, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertDarkBars(activity)
            }
        }
    }

    /** With the phone in its dark theme, the bars are the same: Beacon is always dark. */
    @Config(qualifiers = "night")
    @Test
    fun systemBarsAreDarkInTheDarkTheme() {
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            s.onActivity { activity ->
                assertEquals(Configuration.UI_MODE_NIGHT_YES, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertDarkBars(activity)
            }
        }
    }

    /**
     * Asserts that [activity]'s window asks for light status bar icons and
     * light navigation buttons, and draws no contrast scrim of its own behind
     * the navigation bar.
     *
     * @param activity the started activity.
     */
    private fun assertDarkBars(activity: Activity) {
        val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        // "Light appearance" means dark icons for a light bar; Beacon's bars are dark, so it must be off.
        assertFalse("status bar icons must be light", bars.isAppearanceLightStatusBars)
        assertFalse("navigation buttons must be light", bars.isAppearanceLightNavigationBars)
        assertFalse("no light scrim behind 3-button navigation", activity.window.isNavigationBarContrastEnforced)
    }
}
