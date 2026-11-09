package dev.beacon.ui

import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.alert.AlertController
import dev.beacon.alert.AlertKind
import dev.beacon.alert.AlertRequest
import dev.beacon.alert.AlertState
import dev.beacon.alert.FakeAlertPlatform
import dev.beacon.alert.FakeTorchSwitch
import dev.beacon.alert.MessageSource
import dev.beacon.idleMainUntil
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.ColourMode
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour
import dev.beacon.settings.Pause
import dev.beacon.settings.SettingsRepository
import dev.beacon.settings.UnreadableDataStore
import dev.beacon.setup.SetupStatus
import dev.beacon.widget.FakeResumeAlarm
import dev.beacon.widget.PauseCoordinator
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/*
 * Why Robolectric: the view model needs a real AlertController, which needs
 * Android loopers. The main dispatcher is an unconfined test dispatcher, so
 * each setter's save runs at once, and runTest fails the test if any
 * coroutine lets an exception escape.
 */

/** Robolectric tests for [MainViewModel] over storage that always fails. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MainViewModelTest {
    /** Holds the DataStore file for the test that needs a working store. */
    @get:Rule val tmp = TemporaryFolder()

    /** Runs viewModelScope's coroutines at once on the test thread. */
    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    /** Puts the real main dispatcher back for the next test. */
    @After fun resetMain() = Dispatchers.resetMain()

    /** With unreadable storage the screen opens on the defaults, and every setter's failed save is absorbed. */
    @Test fun failedSavesDontCrash() = runTest {
        val vm = MainViewModel(
            SettingsRepository(UnreadableDataStore()),
            AlertController(FakeTorchSwitch(), Looper.getMainLooper(), Looper.getMainLooper(), FakeAlertPlatform()),
            { NO_SETUP },
            unreadablePauses(),
        )
        assertEquals(BeaconSettings.DEFAULT, vm.settings.value)
        vm.setPause(Pause.Indefinite)
        vm.setAppEnabled(MessageSource.TELEGRAM, true)
        vm.setAppColour(MessageSource.SMS, FlashColour.RED)
        vm.setColourMode(ColourMode.PER_APP)
        vm.setGlobalColour(FlashColour.BLUE)
        vm.setDuration(DurationChoice.FIVE_MINUTES)
        vm.stepCustomLength(longer = true)
        vm.setSpeed(BlinkSpeed.FAST)
        // A lost save leaves the loaded value showing.
        assertEquals(BeaconSettings.DEFAULT, vm.settings.value)
    }

    /** Stop on the test overlay never ends a real alert that has just replaced the test. */
    @Test fun stopTestLeavesRealAlertRunning() = runTest {
        val controller = AlertController(FakeTorchSwitch(), Looper.getMainLooper(), Looper.getMainLooper(), FakeAlertPlatform())
        val vm = MainViewModel(SettingsRepository(UnreadableDataStore()), controller, { NO_SETUP }, unreadablePauses())
        controller.start(AlertRequest(AlertKind.REAL, FlashColour.RED, BlinkSpeed.MEDIUM, 60_000L, MessageSource.SMS))
        vm.stopTest()
        val state = controller.state.value
        assertTrue("real alert still running: $state", state is AlertState.Running && state.request.kind == AlertKind.REAL)
        controller.stop()
    }

    /** The setup status is unknown (null) until the first refresh, then equals what the check returns. */
    @Test fun refreshSetupPublishesStatus() = runTest {
        val status = SetupStatus(notificationAccess = true, displayOverApps = false, batteryUnrestricted = true)
        val controller = AlertController(FakeTorchSwitch(), Looper.getMainLooper(), Looper.getMainLooper(), FakeAlertPlatform())
        val vm = MainViewModel(SettingsRepository(UnreadableDataStore()), controller, { status }, unreadablePauses())
        assertEquals(null, vm.setup.value)
        vm.refreshSetup()
        assertEquals(status, vm.setup.value)
    }

    /**
     * Pause changes go through the coordinator: with a working store, the saved pause is
     * stored, the alarm is cancelled for an open pause and the widget is refreshed once.
     */
    @Test fun setPauseGoesThroughTheCoordinator() = runTest {
        val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repo = SettingsRepository(
                PreferenceDataStoreFactory.create(scope = storeScope) { File(tmp.root, "beacon_settings.preferences_pb") },
            )
            val alarm = FakeResumeAlarm()
            val refreshes = AtomicInteger()
            val controller = AlertController(FakeTorchSwitch(), Looper.getMainLooper(), Looper.getMainLooper(), FakeAlertPlatform())
            val pauses = PauseCoordinator(repo, alarm, refreshWidgets = { refreshes.incrementAndGet() })
            val vm = MainViewModel(repo, controller, { NO_SETUP }, pauses)
            vm.setPause(Pause.Indefinite)
            // The save finishes on the store's thread, so wait for it (bounded) rather than sleep.
            idleMainUntil { refreshes.get() == 1 }
            assertEquals(1, refreshes.get())
            assertEquals(listOf<Long?>(null), alarm.scheduled.toList())
            assertEquals(Pause.Indefinite, repo.settings.first().pause)
        } finally {
            storeScope.cancel()
        }
    }

    /**
     * A coordinator over storage that always fails, for tests that don't look at pausing.
     *
     * @return the coordinator; every change it makes throws IOException.
     */
    private fun unreadablePauses() = PauseCoordinator(SettingsRepository(UnreadableDataStore()), FakeResumeAlarm(), refreshWidgets = {})

    private companion object {
        /** A status for tests that do not look at the setup. */
        val NO_SETUP = SetupStatus(false, false, false)
    }
}
