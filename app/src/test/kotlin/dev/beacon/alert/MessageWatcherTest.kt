package dev.beacon.alert

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.AppSetting
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.ColourMode
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour
import dev.beacon.settings.Pause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/** Robolectric tests for [MessageWatcher]; the main looper plays every thread. */
@RunWith(AndroidJUnit4::class)
class MessageWatcherTest {
    private val SMS_APP = "com.google.android.apps.messaging"
    private val settings = MutableStateFlow<BeaconSettings?>(BeaconSettings.DEFAULT)
    private val controller = AlertController(
        FakeTorchSwitch(), Looper.getMainLooper(), Looper.getMainLooper(), FakeAlertPlatform(),
    )
    private val logged = mutableListOf<Decision>()
    private val watcher = MessageWatcher(
        settings, controller, CoroutineScope(Dispatchers.Main.immediate), { SMS_APP }, { 0L }, { logged += it },
    )

    /**
     * Builds facts for an incoming message, overriding only what a test cares about.
     *
     * @return the facts.
     */
    private fun facts(pkg: String = SMS_APP, key: String = "k1", time: Long = 1_000) =
        NotificationFacts(pkg, key, false, false, "msg", true, time, false, time)

    /** Moves the paused main looper forward. */
    private fun idle(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    /** Pins that a message waits for saved settings and the saved pause wins over the defaults. */
    @Test fun waitsForSavedSettings() {
        settings.value = null
        watcher.onPosted(facts())
        assertEquals(AlertState.Idle, controller.state.value)
        settings.value = BeaconSettings.DEFAULT.copy(pause = Pause.Indefinite)
        assertEquals(AlertState.Idle, controller.state.value)
        settings.value = BeaconSettings.DEFAULT.copy(pause = Pause.None)
        watcher.onPosted(facts(key = "k2", time = 2_000))
        assertTrue(controller.state.value is AlertState.Running)
    }

    /** Pins that the alert uses the saved colour, speed and length. */
    @Test fun alertUsesTheSavedChoices() {
        settings.value = BeaconSettings.DEFAULT.copy(speed = BlinkSpeed.FAST, duration = DurationChoice.FIVE_MINUTES)
        watcher.onPosted(facts())
        val running = controller.state.value as AlertState.Running
        assertEquals(
            AlertRequest(AlertKind.REAL, FlashColour.YELLOW, BlinkSpeed.FAST, 300_000L, MessageSource.SMS),
            running.request,
        )
    }

    /** Pins that per-app mode uses the app's own colour. */
    @Test fun perAppColourIsUsed() {
        settings.value = BeaconSettings.DEFAULT.copy(
            colourMode = ColourMode.PER_APP,
            apps = BeaconSettings.DEFAULT.apps + (MessageSource.WHATSAPP to AppSetting(true, FlashColour.RED)),
        )
        watcher.onPosted(facts(pkg = "com.whatsapp"))
        assertEquals(FlashColour.RED, (controller.state.value as AlertState.Running).request.colour)
    }

    /** Pins that a busy alert still records the message but does not stack. */
    @Test fun busyAlertRecordsButDoesNotStack() {
        watcher.onPosted(facts(key = "a"))
        val first = controller.state.value
        watcher.onPosted(facts(key = "b"))
        assertEquals(first, controller.state.value)
        controller.stop(); idle(10)
        watcher.onPosted(facts(key = "b"))
        assertEquals(AlertState.Idle, controller.state.value)
    }

    /** Pins that notifications already showing at connect time never flash. */
    @Test fun primedNotificationsDontFlash() {
        watcher.onConnected(listOf(facts()))
        watcher.onPosted(facts())
        assertEquals(AlertState.Idle, controller.state.value)
    }

    /** Pins that a removed notification posted again counts as new. */
    @Test fun removedThenPostedAgainFlashes() {
        watcher.onPosted(facts())
        controller.stop(); idle(10)
        watcher.onRemoved("k1")
        watcher.onPosted(facts())
        assertTrue(controller.state.value is AlertState.Running)
    }

    /** Pins that every decision is passed to the log. */
    @Test fun logsEveryDecision() {
        watcher.onPosted(facts()); watcher.onPosted(facts())
        assertEquals(
            listOf(Decision.Alert(MessageSource.SMS), Decision.Ignore(IgnoreReason.ALREADY_HANDLED)),
            logged,
        )
    }
}
