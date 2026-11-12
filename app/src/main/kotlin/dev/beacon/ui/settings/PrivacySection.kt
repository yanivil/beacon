package dev.beacon.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
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
 * loads the page; Beacon itself sends nothing.
 *
 * Why the "no browser" message is Beacon's own text and not a toast: a system
 * toast is small, its colours aren't checked for 7:1 and it vanishes after a
 * few seconds. Beacon's text under the button keeps the 20 sp and contrast
 * rules, stays until the screen is left, and TalkBack announces it.
 */

/** The published privacy policy: the GitHub page of PRIVACY.md on main, the address Play Console has. */
const val PRIVACY_POLICY_URL = "https://github.com/yanivil/beacon/blob/main/PRIVACY.md"

/**
 * The privacy section: a "Privacy" heading, a one-line summary, a full-width
 * "Read the privacy policy" button and a note that it opens in the browser.
 * If no browser can open the page, a message appears under the button.
 *
 * @param modifier the modifier for the section.
 */
@Composable
fun PrivacySection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Kept across rotation; a later tap that opens a browser clears it.
    var noBrowser by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.privacy_heading))
        BeaconText(stringResource(R.string.privacy_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        OutlineButton(
            text = stringResource(R.string.privacy_button),
            onClick = { noBrowser = !openPrivacyPolicy(context) },
            content = BeaconColors.White,
            icon = null,
            minHeight = 64.dp,
            style = BeaconType.SmallButton,
            modifier = Modifier.fillMaxWidth(),
        )
        if (noBrowser) {
            BeaconText(
                stringResource(R.string.privacy_no_browser),
                // Polite: TalkBack reads it after the button's own click feedback.
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = BeaconType.BodyStrong.copy(color = BeaconColors.Text),
            )
        }
        BeaconText(
            stringResource(R.string.privacy_opens_in_browser),
            style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
        )
    }
}

/**
 * Opens [PRIVACY_POLICY_URL] in the phone's browser.
 *
 * @param context where to start the browser from.
 * @return true if the browser was started, false if nothing could open it; the caller then shows
 *   its "no browser" message.
 */
fun openPrivacyPolicy(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
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
