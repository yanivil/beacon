package dev.beacon.ui

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.alert.AlertController
import dev.beacon.alert.FakeAlertPlatform
import dev.beacon.alert.FakeTorchSwitch
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.ColourMode
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour
import dev.beacon.settings.Pause
import dev.beacon.settings.SettingsRepository
import dev.beacon.settings.UnreadableDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
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
    /** Runs viewModelScope's coroutines at once on the test thread. */
    @Before fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    /** Puts the real main dispatcher back for the next test. */
    @After fun resetMain() = Dispatchers.resetMain()

    /** With unreadable storage the screen opens on the defaults, and every setter's failed save is absorbed. */
    @Test fun failedSavesDontCrash() = runTest {
        val vm = MainViewModel(
            SettingsRepository(UnreadableDataStore()),
            AlertController(FakeTorchSwitch(), Looper.getMainLooper(), Looper.getMainLooper(), FakeAlertPlatform()),
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
}
