package dev.beacon.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.beacon.BeaconApp
import java.io.IOException
import kotlinx.coroutines.launch

/*
 * Why a time-change receiver (follow-up R1): the widget's resume time
 * follows the 12/24-hour setting and the time zone, and its words follow
 * the language. Since ruling F3 the widget is redrawn only when the saved
 * pause changes, and nothing else told Beacon about these changes, so a
 * timed pause kept showing the old format or zone until the next pause
 * change, the alarm or a tap. Android sends TIME_SET (also for the
 * 12/24-hour switch), TIMEZONE_CHANGED and LOCALE_CHANGED to manifest
 * receivers on every version Beacon supports.
 *
 * The receiver first stores the new widget key, so a Glance session that is
 * still running composes again with the new format or zone. Then it always
 * asks the coordinator, even when the key didn't change: if Beacon's
 * process wasn't running, the broadcast started it and onCreate read the
 * key fresh, so "unchanged" says nothing about what the widget last drew.
 * The coordinator redraws at most once, and only when what the widget shows
 * depends on the change. For the same reason this also repairs a language
 * change made while Beacon's process wasn't running.
 */

/** The system broadcasts the receiver handles; the manifest asks for the same three. */
private val TIME_CHANGE_ACTIONS =
    setOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED)

/** Redraws the widget after the clock, the time zone, the 12/24-hour setting or the language changed. */
class TimeChangeReceiver : BroadcastReceiver() {
    /**
     * Updates the widget's key and catches the pause up off the receiver's call, then finishes the
     * broadcast. Any action other than the three system ones is ignored.
     *
     * @param context the app's context.
     * @param intent the system broadcast: TIME_SET, TIMEZONE_CHANGED or LOCALE_CHANGED.
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Not one of the broadcasts the manifest asks for: touch nothing.
        if (intent.action !in TIME_CHANGE_ACTIONS) return
        val languageChanged = intent.action == Intent.ACTION_LOCALE_CHANGED
        val app = context.applicationContext as BeaconApp
        // goAsync keeps the broadcast open while DataStore reads and writes off the main thread.
        val pending = goAsync()
        app.appScope.launch {
            try {
                // First, so a running session composes again with the new format or zone.
                app.updateWidgetConfig()
                app.pauseCoordinator.catchUpAfterTimeChange(languageChanged)
            } catch (_: IOException) {
                // The settings can't be read or saved: leave them. The key change above still redraws a
                // running session, and the next pause change, alarm or tap redraws the widget.
            } finally {
                pending.finish()
            }
        }
    }
}
