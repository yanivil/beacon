package dev.beacon.alert

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** Robolectric tests for [BeaconListener] and [ListenerConnection]. */
@RunWith(AndroidJUnit4::class)
class BeaconListenerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val component = ComponentName(context, BeaconListener::class.java)

    /** Resets the process-wide flag so tests don't leak into each other. */
    @After fun resetFlag() { ListenerConnection.connected = false }

    /**
     * Reads the listener component's enabled setting.
     *
     * @return one of the PackageManager COMPONENT_ENABLED_STATE values.
     */
    private fun enabledSetting() = context.packageManager.getComponentEnabledSetting(component)

    /** Pins that connect and disconnect set the flag. */
    @Test fun connectFlag() {
        val listener = Robolectric.buildService(BeaconListener::class.java).create().get()
        listener.onListenerConnected()
        assertTrue(ListenerConnection.connected)
        listener.onListenerDisconnected()
        assertFalse(ListenerConnection.connected)
    }

    /** Pins that the reconnect toggle runs only when access is granted and the listener dropped. */
    @Test fun reconnectOnlyWhenGrantedAndDropped() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ListenerConnection.connected = false
        ListenerConnection.reconnectIfDropped(context)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, enabledSetting())

        shadowOf(manager).setNotificationListenerAccessGranted(component, true)
        ListenerConnection.connected = true
        ListenerConnection.reconnectIfDropped(context)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, enabledSetting())

        ListenerConnection.connected = false
        ListenerConnection.reconnectIfDropped(context)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, enabledSetting())
    }
}
