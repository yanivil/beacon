package dev.beacon.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.beacon.R
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why a version line: on 15 November 2026 the owner couldn't tell from the
 * app which Beacon a phone had, and testers will be asked the same, so the
 * owner asked for the version inside the app. It sits at the end of Alert
 * settings, after Privacy, for the reason Privacy does: that is where a
 * person looks for how Beacon behaves, and Home stays about the alerts.
 *
 * Why the caller passes the name: the app passes BuildConfig.VERSION_NAME,
 * the versionName in app/build.gradle.kts, so the number is never typed a
 * second time and can't fall behind a release.
 *
 * Why plain body text: it is information, not a control, so it has no role
 * and TalkBack reads it as it reads the other notes. The secondary colour on
 * the page is a pair already checked for 7:1 (BeaconColors).
 */

/**
 * The last line of Alert settings: "Beacon version" followed by the version name.
 *
 * @param versionName the version to show; the app passes BuildConfig.VERSION_NAME.
 * @param modifier the modifier for the line.
 */
@Composable
fun VersionLine(versionName: String, modifier: Modifier = Modifier) {
    BeaconText(
        stringResource(R.string.settings_version, versionName),
        modifier = modifier,
        style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
    )
}
