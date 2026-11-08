package dev.beacon.alert

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Why a wrapped context: a phone can lack the camera service, and then
 * getSystemService returns null for it. A ContextWrapper that answers null
 * for the camera service reproduces that through the switch's own
 * constructor, so the main code needs no extra seam for the test.
 */

/** Robolectric tests for [CameraTorchSwitch] when the phone has no camera service. */
@RunWith(AndroidJUnit4::class)
class CameraTorchSwitchTest {
    /**
     * A context with no camera service. It is its own application context,
     * because the switch looks the service up on the application context.
     *
     * @param base The real test context every other call goes to.
     */
    private class NoCameraContext(base: Context) : ContextWrapper(base) {
        /**
         * Returns this context, so the switch's lookup reaches [getSystemService] below.
         *
         * @return This context.
         */
        override fun getApplicationContext(): Context = this

        /**
         * Answers null for the camera service, as a phone without one does.
         *
         * @param name The service name.
         * @return Null for the camera service; the real service otherwise.
         */
        override fun getSystemService(name: String): Any? =
            if (name == Context.CAMERA_SERVICE) null else super.getSystemService(name)
    }

    /** Pins that a missing camera service means no light and a refused switch, not a crash. */
    @Test fun noCameraServiceMeansNoLight() {
        val torch = CameraTorchSwitch(NoCameraContext(ApplicationProvider.getApplicationContext()))
        assertFalse(torch.available)
        assertFalse(torch.set(true))
    }
}
