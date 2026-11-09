package dev.beacon.ui.blink

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.alert.BlinkClock
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.FlashColour
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/*
 * Why only the behaviour is checked here: Robolectric doesn't report whether
 * the bars are really hidden, so the swipe behaviour that goes with hiding is
 * the part it can see. Whether the bars are really hidden is checked on the
 * emulator.
 */

/** Robolectric test that BlinkScreen takes control of the system bars. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SystemBarsTest {
    /** Hosts the content in a plain activity, whose window the test can read. */
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** While BlinkScreen is shown, a swipe from the edge brings the bars back for a moment. */
    @Test fun blinkScreenSetsSwipeBehaviour() {
        // BlinkScreen asks for a frame on every frame, so the clock never idles.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            BlinkScreen(BlinkClock(SystemClock.uptimeMillis(), BlinkSpeed.MEDIUM, 60_000L), FlashColour.YELLOW) {}
        }
        rule.mainClock.advanceTimeByFrame()
        val window = rule.activity.window
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior,
        )
    }
}
