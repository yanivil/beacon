package dev.beacon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.Pause
import dev.beacon.ui.components.ScreenTitle

/*
 * Why Home takes settings and a bundle of actions instead of the view model:
 * the screen is then plain data in, plain callbacks out, so tests drive it
 * with recorded lambdas and the shell decides what each action does.
 *
 * Why "paused" here means any pause: a timed pause is not offered yet (a
 * later task), and the stored deadline is not compared with the clock here,
 * so the card follows the stored value. Any pause shows the PAUSED card.
 */

/**
 * What Home can ask for.
 *
 * @property pauseAll pause all alerts until the user turns them back on.
 * @property resume turn alerts back on.
 * @property toggleApp flip one app's ON/OFF switch.
 * @property openSettings go to the Alert settings tab.
 */
data class HomeActions(
    val pauseAll: () -> Unit,
    val resume: () -> Unit,
    val toggleApp: (MessageSource) -> Unit,
    val openSettings: () -> Unit,
)

/**
 * The Home tab: title, alerts card, the app switches and the summary.
 *
 * @param settings the current settings.
 * @param actions what the screen's controls do.
 */
@Composable
fun HomeScreen(settings: BeaconSettings, actions: HomeActions) {
    // Any pause that is stored counts; the 'Back ON by itself' line comes with timed pauses later.
    val paused = settings.pause != Pause.None
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        ScreenTitle(stringResource(R.string.home_title))
        AlertsCard(paused, onPause = actions.pauseAll, onResume = actions.resume)
        AppsSection(settings, paused, actions.toggleApp)
        SummaryCard(settings, actions.openSettings)
    }
}
