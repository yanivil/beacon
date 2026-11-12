package dev.beacon.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.beacon.R
import dev.beacon.ui.components.OutlineButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why a privacy section in the app: Google Play's User Data policy asks for
 * the privacy policy both in Play Console and within the app itself (the Play
 * release's final review, I1). It sits at the end of Alert settings, where a
 * person looks for how Beacon behaves, and not on Home, which stays about the
 * alerts themselves.
 *
 * Why the browser opens it: Beacon has no internet permission and stays that
 * way. A browsable VIEW intent hands the address to the phone's browser, which
 * loads the page; Beacon itself sends nothing. The intent names the browser,
 * when Beacon can tell which app it is, because the GitHub app claims
 * github.com links and would otherwise take the page, and the screen promises
 * a web browser.
 *
 * Why the "can't open" message is Beacon's own text and not a toast: a system
 * toast is small, its colours aren't checked for 7:1 and it vanishes after a
 * few seconds. Beacon's text under the button keeps the 20 sp and contrast
 * rules, stays until the screen is left, and TalkBack announces it.
 *
 * Why the screen gets a callback: like every other screen, it asks the shell
 * to act (SettingsActions), so its tests need no browser and the shell's
 * tests check the real intent.
 */

/** The published privacy policy: the GitHub page of PRIVACY.md on main, the address Play Console has. */
const val PRIVACY_POLICY_URL = "https://github.com/yanivil/beacon/blob/main/PRIVACY.md"

/**
 * A web address no app claims for itself (example.com is reserved for examples), so only
 * browsers answer it. Asking Android who opens it tells Beacon which apps are browsers.
 */
private const val ANY_WEB_PAGE = "https://example.com"

/**
 * The privacy section: a "Privacy" heading, a one-line summary, a full-width
 * "Read the privacy policy" button and a note that it opens in the browser.
 * If the policy can't be opened, a message appears under the button.
 *
 * @param openPolicy opens the policy; returns false if nothing could open it.
 * @param modifier the modifier for the section.
 */
@Composable
fun PrivacySection(openPolicy: () -> Boolean, modifier: Modifier = Modifier) {
    // Kept across rotation; a later tap that opens a browser clears it.
    var cantOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.privacy_heading))
        BeaconText(stringResource(R.string.privacy_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        // A traversal group, so TalkBack can read the note before the button while the note stays below it.
        Column(
            Modifier.semantics { isTraversalGroup = true },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlineButton(
                text = stringResource(R.string.privacy_button),
                onClick = { cantOpen = !openPolicy() },
                content = BeaconColors.White,
                icon = null,
                minHeight = 64.dp,
                style = BeaconType.Button,
                modifier = Modifier.fillMaxWidth(),
            )
            if (cantOpen) {
                BeaconText(
                    stringResource(R.string.privacy_no_browser),
                    // Polite: TalkBack reads it after the button's own click feedback.
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = BeaconType.BodyStrong.copy(color = BeaconColors.Text),
                )
            }
            BeaconText(
                stringResource(R.string.privacy_opens_in_browser),
                // Read first in this group: the person hears where the button leads before reaching it.
                modifier = Modifier.semantics { traversalIndex = -1f },
                style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
            )
        }
    }
}

/**
 * Opens [PRIVACY_POLICY_URL] in the phone's web browser.
 *
 * @param context where to start the browser from.
 * @return true if the browser was started, false if nothing could open the page; the caller then
 *   shows its "can't open" message.
 */
fun openPrivacyPolicy(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
    browserPackage(context)?.let { intent.setPackage(it) }
    // Only an activity can start another activity in its own task; any other context needs a new one.
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: RuntimeException) {
        // ActivityNotFoundException (no browser) and SecurityException: a tap must never crash Beacon.
        false
    }
}

/**
 * Finds the app that is the phone's web browser.
 *
 * @param context any context, for the package manager.
 * @return the default browser's package; else the only browser's package; or null when there is
 *   none, or several and no default, and Android should choose.
 */
internal fun browserPackage(context: Context): String? {
    val probe = Intent(Intent.ACTION_VIEW, ANY_WEB_PAGE.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
    val pm = context.packageManager
    val browsers = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo.packageName }
        .distinct()
    // With no default set, Android answers with its own chooser, which isn't one of the browsers.
    val default = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    return when {
        default != null && default in browsers -> default
        browsers.size == 1 -> browsers.single()
        else -> null
    }
}
