package dev.beacon.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.Pause
import dev.beacon.settings.SettingsRepository
import dev.beacon.settings.UnreadableDataStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 * Two tests hold the change half-way with a gate and cancel the caller
 * there, as when the person leaves Beacon right after a tap (ruling F1).
 * The catch-up after a time, zone or language change redraws for a reason
 * other than a changed pause, so its tests count the redraws per state
 * (follow-up R1).
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

    /** Half an hour, in milliseconds. */
    private val halfHour = 30 * 60_000L

    /** The real DataStore on a temporary file. */
    private lateinit var realStore: DataStore<Preferences>

    /** The real repository over [realStore]. */
    private lateinit var repo: SettingsRepository

    /** The coordinator under test. */
    private lateinit var c: PauseCoordinator

    /** Builds the repository and the coordinator once the temporary folder exists. */
    @Before fun setUp() {
        realStore = PreferenceDataStoreFactory.create(scope = storeScope) { File(tmp.root, "beacon_settings.preferences_pb") }
        repo = SettingsRepository(realStore)
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

    /** Before its deadline, a timed pause is kept and the alarm is armed for it again; the widget isn't redrawn. */
    @Test fun catchUpKeepsARunningPauseAndRearms() = runTest {
        repo.setPause(Pause.Until(5_000)); now = 4_000; c.catchUp()
        assertEquals(Pause.Until(5_000), repo.settings.first().pause); assertEquals(5_000L, alarm.scheduled.last())
        assertEquals(0, refreshes)
    }

    /** With alerts ON and nothing to clean up, a catch-up cancels the alarm and doesn't redraw the widget (ruling F3). */
    @Test fun catchUpWithNothingToDoDoesNotRefresh() = runTest {
        repo.setPause(Pause.None); c.catchUp()
        assertEquals(Pause.None, repo.settings.first().pause)
        assertEquals(listOf<Long?>(null), alarm.scheduled); assertEquals(0, refreshes)
    }

    /** An open-ended pause has no deadline, so a catch-up keeps it, cancels the alarm and doesn't redraw. */
    @Test fun catchUpKeepsIndefinite() = runTest {
        repo.setPause(Pause.Indefinite); now = 10_000_000; c.catchUp()
        assertEquals(Pause.Indefinite, repo.settings.first().pause)
        assertEquals(listOf<Long?>(null), alarm.scheduled); assertEquals(0, refreshes)
    }

    /** Setting the pause that is already stored re-arms the alarm but doesn't redraw the widget. */
    @Test fun settingTheSamePauseDoesNotRefresh() = runTest {
        c.tapFromWidget(); c.set(Pause.Until(3_601_000))
        assertEquals(listOf<Long?>(3_601_000, 3_601_000), alarm.scheduled); assertEquals(1, refreshes)
    }

    /**
     * After a time, zone or 12/24-hour change, a running timed pause is redrawn once, since the widget
     * shows its time; the pause is kept and the alarm armed for its deadline (follow-up R1).
     */
    @Test fun timeChangeRedrawsARunningTimedPause() = runTest {
        repo.setPause(Pause.Until(now + halfHour)); c.catchUpAfterTimeChange(languageChanged = false)
        assertEquals(1, refreshes)
        assertEquals(Pause.Until(1_000 + halfHour), repo.settings.first().pause)
        assertEquals(listOf<Long?>(1_000 + halfHour), alarm.scheduled)
    }

    /** ON and the open-ended pause show no time, so a time change redraws neither; their alarm is cancelled. */
    @Test fun timeChangeLeavesOnAndIndefiniteAlone() = runTest {
        repo.setPause(Pause.None); c.catchUpAfterTimeChange(languageChanged = false)
        assertEquals(0, refreshes)
        repo.setPause(Pause.Indefinite); c.catchUpAfterTimeChange(languageChanged = false)
        assertEquals(0, refreshes)
        assertEquals(listOf<Long?>(null, null), alarm.scheduled)
    }

    /** Every state shows words, so a language change redraws ON and the open-ended pause once each. */
    @Test fun languageChangeRedrawsEveryState() = runTest {
        repo.setPause(Pause.None); c.catchUpAfterTimeChange(languageChanged = true)
        assertEquals(1, refreshes)
        repo.setPause(Pause.Indefinite); c.catchUpAfterTimeChange(languageChanged = true)
        assertEquals(2, refreshes)
        assertEquals(Pause.Indefinite, repo.settings.first().pause)
    }

    /** A timed pause that a time change has ended is cleaned up to ON with exactly one redraw, and its alarm is cancelled. */
    @Test fun timeChangeEndsAnExpiredPauseWithOneRedraw() = runTest {
        repo.setPause(Pause.Until(now - 1)); c.catchUpAfterTimeChange(languageChanged = false)
        assertEquals(Pause.None, repo.settings.first().pause)
        assertEquals(1, refreshes)
        assertEquals(listOf<Long?>(null), alarm.scheduled)
    }

    /** Two reasons at once still give one redraw: an ended pause, or a running one, after a language change. */
    @Test fun twoReasonsStillRedrawOnce() = runTest {
        repo.setPause(Pause.Until(now - 1)); c.catchUpAfterTimeChange(languageChanged = true)
        assertEquals(1, refreshes)
        repo.setPause(Pause.Until(now + halfHour)); c.catchUpAfterTimeChange(languageChanged = true)
        assertEquals(2, refreshes)
    }

    /**
     * A caller cancelled while the widget refresh is under way (Beacon closed right after a tap)
     * doesn't stop the change: the refresh still finishes, after the save and the alarm (ruling F1).
     */
    @Test fun aCancelledCallerStillFinishesTheRefresh() = runTest {
        val refreshStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val gated = PauseCoordinator(
            repo, alarm,
            refreshWidgets = { refreshStarted.complete(Unit); gate.await(); refreshes++ },
            now = { now },
        )
        val caller = launch { gated.tapFromWidget() }
        refreshStarted.await()
        caller.cancel()
        gate.complete(Unit)
        caller.join()
        assertTrue("the caller was cancelled", caller.isCancelled)
        assertEquals(1, refreshes)
        assertEquals(listOf<Long?>(3_601_000), alarm.scheduled)
        assertEquals(Pause.Until(3_601_000), repo.settings.first().pause)
    }

    /**
     * A caller cancelled while the save is under way doesn't stop the change either: once the
     * save completes, the alarm is set and the widget refreshed (ruling F1).
     */
    @Test fun aCancelledCallerStillFinishesAfterTheSave() = runTest {
        val saveStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val gatedRepo = SettingsRepository(GatedStore(realStore, saveStarted, gate))
        val gated = PauseCoordinator(gatedRepo, alarm, refreshWidgets = { refreshes++ }, now = { now })
        val caller = launch { gated.set(Pause.Indefinite) }
        saveStarted.await()
        caller.cancel()
        gate.complete(Unit)
        caller.join()
        assertTrue("the caller was cancelled", caller.isCancelled)
        assertEquals(Pause.Indefinite, repo.settings.first().pause)
        assertEquals(listOf<Long?>(null), alarm.scheduled); assertEquals(1, refreshes)
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

/**
 * A store that holds each write at a gate until the test opens it, so a test can cancel the
 * caller while the save is under way.
 *
 * @param inner the real store that reads and writes.
 * @param started completed when a write reaches the gate.
 * @param gate the write waits for this before it goes on.
 */
private class GatedStore(
    private val inner: DataStore<Preferences>,
    private val started: CompletableDeferred<Unit>,
    private val gate: CompletableDeferred<Unit>,
) : DataStore<Preferences> {
    /** The real store's data. */
    override val data: Flow<Preferences> = inner.data

    /**
     * Waits at the gate, then writes through the real store.
     *
     * @param transform the edit.
     * @return the stored preferences after the edit.
     * @throws IOException if the real store can't read or write its file.
     */
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        started.complete(Unit)
        gate.await()
        return inner.updateData(transform)
    }
}
