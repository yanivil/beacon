package dev.beacon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.resumeAtMillis
import dev.beacon.ui.components.OnOffPill
import dev.beacon.ui.components.TextAndPillPolicy
import dev.beacon.ui.formatResumeTime
import dev.beacon.ui.home.pausedNow
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why a switch at the top of Alert settings: testers asked to turn alerts OFF
 * from the settings with no time limit (the owner's decision, 14 November
 * 2026). OFF is the same open-ended pause as Home's "Pause all alerts", and
 * ON clears any pause, the widget's 1-hour one included, so Home, the widget
 * and this switch always show one state. It sits first because it decides
 * whether any of the settings below matter right now.
 *
 * Why it looks like an app row on Home: the whole card is one Switch, so a
 * senior never has to hit a small knob. TalkBack reads "Visual alerts", the
 * line under it, "Switch" and "On" or "Off"; the pill shows ON or OFF in
 * words, never by colour alone (project rules). The pill wraps under the text
 * at the largest font, as on Home (TextAndPillPolicy).
 *
 * Why ON comes from the clock: as on Home, a timed pause past its deadline is
 * ON even before the stored value is cleaned up (pausedNow).
 */

/**
 * The "Visual alerts" switch card: the title, a line saying what the state means, and the ON/OFF pill.
 *
 * @param settings the current settings, for the pause.
 * @param setAlertsOn called with true to turn alerts ON, or false to turn them OFF until turned back on.
 * @param nowEpochMillis the clock, in epoch milliseconds (a parameter so tests can move it).
 */
@Composable
fun AlertsSwitchSection(settings: BeaconSettings, setAlertsOn: (Boolean) -> Unit, nowEpochMillis: () -> Long) {
    val on = !pausedNow(settings.pause, nowEpochMillis)
    // Only a running timed pause has a time to name; an open-ended one stays OFF until turned ON.
    val resumeAt = settings.pause.resumeAtMillis()
    val line = when {
        on -> stringResource(R.string.alerts_on_body)
        resumeAt != null -> stringResource(R.string.alerts_back_on_at, formatResumeTime(LocalContext.current, resumeAt))
        else -> stringResource(R.string.alerts_paused_body)
    }
    val shape = RoundedCornerShape(20.dp)
    Layout(
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BeaconText(stringResource(R.string.alerts_heading), style = BeaconType.RowTitle.copy(color = BeaconColors.Text))
                BeaconText(line, style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
            }
            OnOffPill(on = on)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(shape)
            .background(BeaconColors.Card)
            .border(2.dp, BeaconColors.Border, shape)
            .toggleable(value = on, role = Role.Switch, onValueChange = setAlertsOn)
            // The app rows' 14/16 dp padding plus the 2 dp border, which is drawn inside the bounds.
            .padding(horizontal = 14.dp, vertical = 16.dp),
        measurePolicy = TextAndPillPolicy(hasTile = false),
    )
}
