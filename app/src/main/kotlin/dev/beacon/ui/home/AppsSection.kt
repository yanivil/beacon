package dev.beacon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.ui.colourName
import dev.beacon.ui.components.AppSwitchRow
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType

/*
 * Why one row per app, each a single switch: the senior decides per app
 * whether a message should flash, and the row states the result in words
 * (the colour that will flash, or "No flash"), so nothing depends on colour
 * alone. While paused a banner explains why nothing flashes even for apps
 * that are ON.
 */

/**
 * The "Flash for these apps" section: heading, hint, the paused banner when
 * paused, then one switch row per message app.
 *
 * @param settings the current settings.
 * @param paused whether alerts are paused right now.
 * @param onToggle called with the app whose row was tapped.
 */
@Composable
fun AppsSection(settings: BeaconSettings, paused: Boolean, onToggle: (MessageSource) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeading(stringResource(R.string.apps_heading))
            BasicText(stringResource(R.string.apps_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        }
        if (paused) PausedBanner()
        // Declaration order is the design's order: SMS, WhatsApp, Telegram.
        MessageSource.entries.forEach { source ->
            val on = settings.apps[source]?.enabled ?: source.defaultEnabled
            val colour = settings.colourFor(source)
            AppSwitchRow(
                name = stringResource(source.nameRes),
                flashLine = if (on) stringResource(R.string.app_flash_line, colourName(colour)) else stringResource(R.string.app_no_flash),
                // OFF shows no swatch: nothing will flash.
                swatch = if (on) Color(colour.argb) else null,
                enabled = on,
                onToggle = { onToggle(source) },
            ) { AppIcon(source) }
        }
    }
}
