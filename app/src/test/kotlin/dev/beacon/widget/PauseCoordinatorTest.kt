package dev.beacon.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.Pause
import dev.beacon.settings.SettingsRepository
import dev.beacon.settings.UnreadableDataStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/*
 * Why a real repository: the coordinator's promise is that the stored pause,
 * the resume alarm and the widget agree after every change. Only a real
 * DataStore shows what was stored; the alarm and the refresh are recorded.
 */

/** Tests for [PauseCoordinator] over a real DataStore file, a recording alarm and a settable clock. */
@RunWith(AndroidJUnit4::class)
class PauseCoordinatorTest {
    /** Holds the DataStore file for each test. */
    @get:Rule val tmp = TemporaryFolder()

    /** Runs the DataStore; cancelled after each test so its file is released. */
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Records each alarm request. */
    private val alarm = FakeResumeAlarm()

    /** How many times the coordinator refreshed the widgets. */
    private var refreshes = 0

    /** The coordinator's clock, in epoch milliseconds; every test starts at 1_000 (ruling P2). */
    private var now = 1_000L

    /** The real repository on a temporary file. */
    private lateinit var repo: SettingsRepository

    /** The coordinator under test. */
    private lateinit var c: PauseCoordinator

    /** Builds the repository and the coordinator once the temporary folder exists. */
    @Before fun setUp() {
        repo = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = storeScope) { File(tmp.root, "beacon_settings.preferences_pb") },
        )
        c = PauseCoordinator(repo, alarm, refreshWidgets = { refreshes++ }, now = { now })
    }

    /** Stops the DataStore. */
    @After fun tearDown() = storeScope.cancel()

    /** A tap while ON pauses until an hour from now, arms the alarm for then and refreshes once. */
    @Test fun widgetTapPausesForAnHourAndArmsTheAlarm() = runTest {
        now = 1_000; assertEquals(Pause.Until(3_601_000), c.tapFromWidget())
        assertEquals(listOf<Long?>(3_601_000), alarm.scheduled); assertEquals(1, refreshes)
    }

    /** A second tap turns alerts back ON and cancels the alarm; it never makes a 2-hour pause. */
    @Test fun twoTapsPauseThenResume() = runTest {
        c.tapFromWidget(); assertEquals(Pause.None, c.tapFromWidget()); assertEquals(listOf<Long?>(3_601_000, null), alarm.scheduled)
    }

    /** Pausing from Home during a widget pause stores the open-ended pause and cancels the alarm. */
    @Test fun homePauseCancelsTheAlarm() = runTest {
        c.tapFromWidget(); c.set(Pause.Indefinite)
        assertEquals(Pause.Indefinite, repo.settings.first().pause); assertEquals(null, alarm.scheduled.last())
    }

    /** At its deadline, a timed pause is cleaned up: ON is stored, the alarm cancelled, the widget refreshed. */
    @Test fun catchUpClearsExpiredPause() = runTest {
        repo.setPause(Pause.Until(5_000)); now = 5_000; c.catchUp()
        assertEquals(Pause.None, repo.settings.first().pause); assertEquals(null, alarm.scheduled.last()); assertEquals(1, refreshes)
    }

    /** Before its deadline, a timed pause is kept and the alarm is armed for it again. */
    @Test fun catchUpKeepsARunningPauseAndRearms() = runTest {
        repo.setPause(Pause.Until(5_000)); now = 4_000; c.catchUp()
        assertEquals(Pause.Until(5_000), repo.settings.first().pause); assertEquals(5_000L, alarm.scheduled.last())
    }

    /** A store that can't be written makes set() fail, and neither the alarm nor the widget is touched. */
    @Test fun saveFailureReachesTheCaller() = runTest {
        val broken = PauseCoordinator(SettingsRepository(UnreadableDataStore()), alarm, refreshWidgets = { refreshes++ }, now = { now })
        try {
            broken.set(Pause.Indefinite)
            fail("set() should fail when the settings can't be saved")
        } catch (_: IOException) {
            // Expected: the coordinator lets the save failure through to its caller.
        }
        assertEquals(emptyList<Long?>(), alarm.scheduled); assertEquals(0, refreshes)
    }
}
