package dev.beacon

import android.content.res.Configuration
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.widget.NoWidgetRefreshRule
import dev.beacon.widget.WidgetRefresher
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/*
 * Why these tests: after only a font-size change the widget must be fitted
 * again, but a rotation must not start a Glance session (rulings F3 and
 * F6). The app is told of each change through onConfigurationChanged, so
 * the tests call it with changed copies of the current configuration and
 * count the refreshes through WidgetRefresher's seam.
 */

/** Tests for [BeaconApp]'s widget configuration key. */
@RunWith(AndroidJUnit4::class)
class BeaconAppTest {
    /** Puts the real refresh back after each test; the count below replaces it meanwhile. */
    @get:Rule val noWidgets = NoWidgetRefreshRule()

    /** The real app. */
    private val app = ApplicationProvider.getApplicationContext<BeaconApp>()

    /** How many times a refresh was asked for. */
    private var refreshes = 0

    /** Counts refreshes instead of running Glance. */
    @Before fun countRefreshes() {
        WidgetRefresher.update = { refreshes++ }
    }

    /**
     * A copy of the app's current configuration, changed by [change].
     *
     * @param change what to change on the copy.
     * @return the changed copy.
     */
    private fun changed(change: Configuration.() -> Unit) = Configuration(app.resources.configuration).apply(change)

    /** A font-size change updates the key and refreshes the widgets once; the same change again doesn't. */
    @Test fun fontScaleChangeUpdatesTheKeyAndRefreshesOnce() {
        val larger = changed { fontScale = 2.0f }
        app.onConfigurationChanged(larger)
        idleMainUntil { refreshes == 1 }
        assertEquals(2.0f, app.widgetConfig.value.fontScale)
        assertEquals(1, refreshes)
        app.onConfigurationChanged(larger)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, refreshes)
    }

    /** A language change also refreshes: the widget's words change length. */
    @Test fun localeChangeRefreshes() {
        app.onConfigurationChanged(changed { setLocale(Locale.forLanguageTag("he-IL")) })
        idleMainUntil { refreshes == 1 }
        assertEquals("he-IL", app.widgetConfig.value.locales)
        assertEquals(1, refreshes)
    }

    /** A rotation leaves the key as it is and starts no refresh, so no Glance session (ruling F3). */
    @Test fun orientationOnlyChangeDoesNotRefresh() {
        val before = app.widgetConfig.value
        val config = app.resources.configuration
        app.onConfigurationChanged(
            changed {
                orientation = Configuration.ORIENTATION_LANDSCAPE
                screenWidthDp = config.screenHeightDp
                screenHeightDp = config.screenWidthDp
            },
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(before, app.widgetConfig.value)
        assertEquals(0, refreshes)
    }
}
