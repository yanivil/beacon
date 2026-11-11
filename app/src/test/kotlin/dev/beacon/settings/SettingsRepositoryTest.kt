package dev.beacon.settings

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import dev.beacon.alert.MessageSource
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Tests [SettingsRepository] against a real DataStore file in a temporary folder, and against a store that fails. */
class SettingsRepositoryTest {
    /** Holds the DataStore file for each test. */
    @get:Rule val tmp = TemporaryFolder()

    /**
     * Builds a repository over a DataStore file in [folder].
     *
     * @param folder where the settings file lives.
     * @param scope the scope the DataStore runs in.
     * @return the repository.
     */
    private fun repoIn(folder: TemporaryFolder, scope: CoroutineScope): SettingsRepository =
        SettingsRepository(
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
            ) { File(folder.root, "beacon_settings.preferences_pb") },
        )

    /** Every field written through the repository is read back. */
    @Test fun roundTripEveryField() = runTest {
        val repo = repoIn(tmp, backgroundScope)
        repo.setPause(Pause.Until(1234L)); repo.setAppEnabled(MessageSource.TELEGRAM, true)
        repo.setAppColour(MessageSource.SMS, FlashColour.RED); repo.setColourMode(ColourMode.PER_APP)
        repo.setGlobalColour(FlashColour.WHITE); repo.setDuration(DurationChoice.CUSTOM)
        repo.stepCustomLength(longer = true); repo.setSpeed(BlinkSpeed.FAST)
        val s = repo.settings.first()
        assertEquals(Pause.Until(1234L), s.pause)
        assertTrue(s.apps.getValue(MessageSource.TELEGRAM).enabled)
        assertEquals(FlashColour.RED, s.apps.getValue(MessageSource.SMS).colour)
        assertEquals(ColourMode.PER_APP, s.colourMode)
        assertEquals(FlashColour.WHITE, s.globalColour)
        assertEquals(DurationChoice.CUSTOM, s.duration)
        assertEquals(15, s.customLength.minutes)
        assertEquals(BlinkSpeed.FAST, s.speed)
    }

    /** A damaged settings file is replaced and the defaults are used. */
    @Test fun corruptFileGivesDefaults() = runTest {
        File(tmp.root, "beacon_settings.preferences_pb").writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertEquals(BeaconSettings.DEFAULT, repoIn(tmp, backgroundScope).settings.first())
    }

    /** A settings file that can't be read (an IOException) gives the defaults instead of an error. */
    @Test fun readErrorGivesDefaults() = runTest {
        assertEquals(BeaconSettings.DEFAULT, SettingsRepository(UnreadableDataStore()).settings.first())
    }

    /** Clearing a pause removes the stored deadline so it cannot come back. */
    @Test fun clearingPauseRemovesDeadline() = runTest {
        val repo = repoIn(tmp, backgroundScope)
        repo.setPause(Pause.Until(99L))
        repo.setPause(Pause.Indefinite)
        assertEquals(Pause.Indefinite, repo.settings.first().pause)
        repo.setPause(Pause.None)
        assertEquals(Pause.None, repo.settings.first().pause)
    }

    /** updatePause stores the new pause and returns it. */
    @Test fun updatePauseWritesAndReturns() = runTest {
        val repo = repoIn(tmp, backgroundScope)
        assertEquals(Pause.Until(9), repo.updatePause { Pause.Until(9) })
        assertEquals(Pause.Until(9), repo.settings.first().pause)
    }

    /** The transform receives the pause that is stored, not a default. */
    @Test fun updatePauseSeesTheStoredValue() = runTest {
        val repo = repoIn(tmp, backgroundScope)
        repo.setPause(Pause.Indefinite)
        assertEquals(Pause.None, repo.updatePause { if (it == Pause.Indefinite) Pause.None else it })
    }

    /** A store that can't be read makes updatePause fail, rather than write over unknown data. */
    @Test(expected = IOException::class) fun updatePauseFailsOnUnreadableStore() = runTest {
        // runTest's block returns Unit, so the Pause this would return is dropped. A bare
        // `Unit` after it is an unused expression to Kotlin 2.4, so there is none.
        SettingsRepository(UnreadableDataStore()).updatePause { Pause.None }
    }

    /** Stepping the custom length does not change which duration is chosen. */
    @Test fun steppingLengthKeepsDuration() = runTest {
        val repo = repoIn(tmp, backgroundScope)
        repo.stepCustomLength(longer = false)
        val s = repo.settings.first()
        assertEquals(DurationChoice.ONE_MINUTE, s.duration)
        assertEquals(5, s.customLength.minutes)
    }
}
