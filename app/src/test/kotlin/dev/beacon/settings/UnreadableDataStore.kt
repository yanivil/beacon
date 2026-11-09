package dev.beacon.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/*
 * Why a shared fake: both the repository's read fallback and the view
 * model's handling of failed saves are tested against storage that is
 * broken, which a real DataStore file can't be made to be reliably.
 */

/** A store whose file can never be read or written, as when storage fails. */
internal class UnreadableDataStore : DataStore<Preferences> {
    /** Fails on the first read, as DataStore does when the file can't be read. */
    override val data: Flow<Preferences> = flow { throw IOException("unreadable") }

    /**
     * Fails every write, as DataStore does when it can't read the file before writing.
     *
     * @param transform the edit, never run.
     * @return never returns.
     * @throws IOException always.
     */
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("unwritable")
}
