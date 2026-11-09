package dev.beacon.ui.home

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.resumeAtMillis
import dev.beacon.setup.SetupItem
import dev.beacon.setup.SetupStatus
import dev.beacon.ui.components.ScreenTitle
import dev.beacon.ui.formatResumeTime

/*
 * Why Home takes settings and a bundle of actions instead of the view model:
 * the screen is then plain data in, plain callbacks out, so tests drive it
 * with recorded lambdas and the shell decides what each action does.
 *
 * Why "paused" comes from the clock: a timed pause ends by itself, and the
 * stored value is only cleaned up a little later. Home asks pausedNow, which
 * compares the deadline with the clock and turns ON at the resume time, so a
 * stale stored pause never shows PAUSED.
 */

/**
 * What Home can ask for.
 *
 * @property pauseAll pause all alerts until the user turns them back on.
 * @property resume turn alerts back on.
 * @property toggleApp flip one app's ON/OFF switch.
 * @property openSettings go to the Alert settings tab.
 * @property openSetup open the Android Settings page for one Finish setup row.
 */
data class HomeActions(
    val pauseAll: () -> Unit,
    val resume: () -> Unit,
    val toggleApp: (MessageSource) -> Unit,
    val openSettings: () -> Unit,
    val openSetup: (SetupItem) -> Unit,
)

/**
 * The Home tab: title, the Finish setup card while something is still off,
 * the alerts card, the app switches and the summary.
 *
 * @param settings the current settings.
 * @param setup which grants are in place, or null before the first check
 *   (no card is shown then, so it cannot flash in and out).
 * @param actions what the screen's controls do.
 * @param nowEpochMillis the clock, in epoch milliseconds (a parameter so tests can move it).
 */
@Composable
fun HomeScreen(
    settings: BeaconSettings,
    setup: SetupStatus?,
    actions: HomeActions,
    nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    val paused = pausedNow(settings.pause, nowEpochMillis)
    // Only a running timed pause has a resume time to show; an open pause has none.
    val resumeAt = settings.pause.resumeAtMillis()
    val resumeTime = if (paused && resumeAt != null) formatResumeTime(LocalContext.current, resumeAt) else null
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        ScreenTitle(stringResource(R.string.home_title))
        if (setup != null && !setup.complete) {
            // The maker and the Android version never change while Beacon runs, so the steps are chosen once.
            val batterySteps = remember { batteryStepsRes(Build.MANUFACTURER, Build.VERSION.SDK_INT) }
            SetupCard(setup, batterySteps, actions.openSetup)
        }
        AlertsCard(paused, resumeTime, onPause = actions.pauseAll, onResume = actions.resume)
        AppsSection(settings, paused, actions.toggleApp)
        SummaryCard(settings, actions.openSettings)
    }
}
