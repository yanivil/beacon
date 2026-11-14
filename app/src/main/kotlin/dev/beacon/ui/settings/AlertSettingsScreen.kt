package dev.beacon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.ColourMode
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour
import dev.beacon.settings.WidgetTap
import dev.beacon.ui.components.ScreenTitle

/*
 * Why the screen only wires sections together: each section (the alerts
 * switch, duration, speed, colour, widget, privacy) owns its layout and its
 * radio group, so the screen holds just the title and the 40 dp rhythm
 * between sections, as the design does. The privacy section is the Play
 * release's addition, and the alerts switch and the widget section the
 * owner's of 14 November 2026; none of the three is in the design.
 * The actions are plain callbacks so the screen can be tested without a
 * view model.
 */

/**
 * What the Alert settings screen can ask for. The shell connects each
 * callback to the view model.
 *
 * @property setDuration chooses how long to flash.
 * @property stepCustom moves the custom length one step; true for longer.
 * @property setSpeed chooses a blink-speed preset (never a raw number).
 * @property setColourMode chooses one colour for all apps or one per app.
 * @property setGlobalColour sets the colour used for all apps.
 * @property setAppColour sets one app's own colour.
 * @property setEditingApp opens one app's colour grid, or closes it with null.
 * @property openPrivacyPolicy opens the privacy policy in the browser; returns false if nothing
 *   could open it, so the screen can say so.
 * @property setAlertsOn turns alerts ON (true), or OFF until turned back on (false).
 * @property setWidgetTap chooses what one widget tap does while alerts are ON.
 */
data class SettingsActions(
    val setDuration: (DurationChoice) -> Unit,
    val stepCustom: (longer: Boolean) -> Unit,
    val setSpeed: (BlinkSpeed) -> Unit,
    val setColourMode: (ColourMode) -> Unit,
    val setGlobalColour: (FlashColour) -> Unit,
    val setAppColour: (MessageSource, FlashColour) -> Unit,
    val setEditingApp: (MessageSource?) -> Unit,
    val openPrivacyPolicy: () -> Boolean,
    val setAlertsOn: (Boolean) -> Unit,
    val setWidgetTap: (WidgetTap) -> Unit,
)

/**
 * The Alert settings tab: the title, then the "Visual alerts" switch, "How long to flash", "Blink
 * speed", "Screen flash color", "Widget" and "Privacy".
 *
 * @param settings the current settings.
 * @param editingApp the app whose colour grid is open in per-app mode, or null.
 * @param actions the changes the screen can ask for.
 * @param nowEpochMillis the clock, in epoch milliseconds, for whether a timed pause has ended.
 */
@Composable
fun AlertSettingsScreen(
    settings: BeaconSettings,
    editingApp: MessageSource?,
    actions: SettingsActions,
    nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
        ScreenTitle(stringResource(R.string.settings_title))
        AlertsSwitchSection(settings, actions.setAlertsOn, nowEpochMillis)
        DurationSection(settings, actions)
        SpeedSection(settings, actions)
        ColourSection(settings, editingApp, actions)
        WidgetSection(settings, actions)
        PrivacySection(actions.openPrivacyPolicy)
    }
}
