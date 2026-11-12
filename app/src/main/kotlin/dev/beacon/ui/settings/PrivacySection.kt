package dev.beacon.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 */

/** The published privacy policy: the GitHub page of PRIVACY.md on main, the address Play Console has. */
const val PRIVACY_POLICY_URL = "https://github.com/yanivil/beacon/blob/main/PRIVACY.md"

/**
 * The privacy section: a "Privacy" heading, a one-line summary, a full-width
 * "Read the privacy policy" button and a note that it opens in the browser.
 *
 * @param modifier the modifier for the section.
 */
@Composable
fun PrivacySection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.privacy_heading))
        BeaconText(stringResource(R.string.privacy_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        OutlineButton(
            text = stringResource(R.string.privacy_button),
            onClick = { openPrivacyPolicy(context) },
            content = BeaconColors.White,
            icon = null,
            minHeight = 64.dp,
            style = BeaconType.SmallButton,
            modifier = Modifier.fillMaxWidth(),
        )
        BeaconText(
            stringResource(R.string.privacy_opens_in_browser),
            style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
        )
    }
}

/**
 * Opens [PRIVACY_POLICY_URL] in the phone's browser. If nothing can open a
 * web page, it shows a short message instead.
 *
 * @param context where to start the browser from.
 * @return true if the browser was started, false if the message was shown instead.
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
        Toast.makeText(context, R.string.privacy_no_browser, Toast.LENGTH_LONG).show()
        false
    }
}
