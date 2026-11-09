package dev.beacon.alert

import android.app.NotificationManager
import org.junit.Rule
import dev.beacon.widget.NoWidgetRefreshRule
import android.app.Notification
import android.content.ComponentName
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import dev.beacon.alert.AlertState
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import dev.beacon.BeaconApp
import dev.beacon.idleMainUntil
import dev.beacon.settings.Pause
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    /** Skips the widget refresh the screen-on catch-up starts. */
    @get:Rule val noWidgets = NoWidgetRefreshRule()

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

    /** The screen turning on, while the listener is connected, cleans up an expired timed pause. */
    @Test fun screenOnCatchesUp() {
        val app = context as BeaconApp
        runBlocking { app.settingsRepository.setPause(Pause.Until(1_000)) }
        val listener = Robolectric.buildService(BeaconListener::class.java).create().get()
        listener.onListenerConnected()
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
        idleMainUntil { runBlocking { app.settingsRepository.settings.first() }.pause == Pause.None }
        assertEquals(Pause.None, runBlocking { app.settingsRepository.settings.first() }.pause)
        listener.onListenerDisconnected()
    }

    /** Once the listener is disconnected, the screen-on receiver is gone; destroying it removes it too. */
    @Test fun noScreenOnReceiverAfterDisconnect() {
        val app = context as BeaconApp

        /**
         * Counts the receivers registered for the screen turning on.
         *
         * @return how many there are.
         */
        fun screenOnReceivers() = shadowOf(app).registeredReceivers.count { it.intentFilter.hasAction(Intent.ACTION_SCREEN_ON) }
        val controller = Robolectric.buildService(BeaconListener::class.java).create()
        val listener = controller.get()
        listener.onListenerConnected()
        assertEquals(1, screenOnReceivers())
        listener.onListenerDisconnected()
        assertEquals(0, screenOnReceivers())
        // A repeated disconnect, or a destroy after it, must not throw on the missing receiver.
        listener.onListenerDisconnected()
        controller.destroy()
        assertEquals(0, screenOnReceivers())
    }

    /** A WhatsApp message from another person, posted to the listener, starts a real WhatsApp alert. */
    @Test fun postedNotificationStartsAnAlert() {
        val app = context as BeaconApp
        // The watcher waits for the saved settings; load them first so the decision runs at once.
        app.settingsState
        idleMainUntil { app.settingsState.value != null }
        val me = Person.Builder().setName("Me").setKey("me").build()
        val ann = Person.Builder().setName("Ann").setKey("ann").build()
        val now = System.currentTimeMillis()
        val notification: Notification = NotificationCompat.Builder(context, "c")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(NotificationCompat.MessagingStyle(me).addMessage("hello", now, ann))
            .setWhen(now).setCategory(NotificationCompat.CATEGORY_MESSAGE).build()
        // The only public constructor on API 29+ is the one with a score; it is deprecated but still works.
        @Suppress("DEPRECATION")
        val sbn = StatusBarNotification(
            "com.whatsapp", "com.whatsapp", 1, null, 10_000, 0, 0, notification, Process.myUserHandle(), now,
        )
        val listener = Robolectric.buildService(BeaconListener::class.java).create().get()
        try {
            listener.onNotificationPosted(sbn)
            idleMainUntil { app.alertController.state.value is AlertState.Running }
            val state = app.alertController.state.value
            assertTrue("running: $state", state is AlertState.Running)
            state as AlertState.Running
            assertEquals(AlertKind.REAL, state.request.kind)
            assertEquals(MessageSource.WHATSAPP, state.request.source)
        } finally {
            app.alertController.stop()
        }
    }
}
