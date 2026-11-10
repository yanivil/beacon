package dev.beacon.ui.home

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.setup.SetupIntents
import dev.beacon.setup.SetupItem
import dev.beacon.setup.SetupStatus
import dev.beacon.ui.components.OnOffPill
import dev.beacon.ui.components.OutlineButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.components.TextAndPillPolicy
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType
import dev.beacon.ui.theme.LocalContentColour

/*
 * Why the battery steps depend on the Android version: Beacon can only open
 * its App info page for this grant, and stock Android names the way from
 * there to "Unrestricted" differently in different versions (spec section 9).
 * Steps that name labels the person can't find are worse than none, so the
 * card names the ones their version shows. Where one version changed its
 * wording in an update (Android 13 and 14), the card uses the last one.
 * On Android 14 and later the steps also steer away from the switch in
 * "Allow background usage", which restricts Beacon, and say how to get
 * past the next page's switch when it is already off.
 *
 * Why a dark card with a thick white border: like the PAUSED card, it must
 * stand out from the rest of Home by more than colour, because until these
 * three things are granted Beacon cannot flash at all (spec section 9).
 *
 * Why each row is one merged TalkBack item: a person hears "Notification
 * access, So Beacon knows when a message arrives, Off" in one swipe. The
 * name and reason are the label, On/Off is the state, and the pill is only a
 * picture of it (its own semantics are cleared). The button is its own item,
 * and its spoken label starts with the visible words "Open settings" and then
 * names the row, so a voice user can say what they see (spec section 11).
 */

/** Test tag on each setup row's ON/OFF pill, so a test can find it although its own semantics are cleared. */
internal const val SETUP_PILL_TAG = "setup_pill"

/**
 * The Finish setup card: a heading, a hint and one row per grant.
 *
 * @param status which grants are in place; every row shows its state.
 * @param batterySteps the battery row's steps, as chosen by [batteryStepsRes] for this phone.
 * @param onOpen called with the row whose "Open settings" button was tapped.
 */
@Composable
fun SetupCard(status: SetupStatus, @StringRes batterySteps: Int, onOpen: (SetupItem) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Card, shape)
            .border(4.dp, BeaconColors.White, shape)
            // The design's 16 / 20 dp padding plus the 4 dp border, which is drawn inside the bounds.
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Set explicitly: the heading must be white on the dark card whatever the surrounding colour.
            CompositionLocalProvider(LocalContentColour provides BeaconColors.White) {
                SectionHeading(stringResource(R.string.setup_heading))
            }
            BasicText(
                stringResource(R.string.setup_hint),
                style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
            )
        }
        SetupRow(SetupItem.NOTIFICATIONS, R.string.setup_notifications, R.string.setup_notifications_reason, null, status, onOpen)
        SetupRow(SetupItem.OVERLAY, R.string.setup_overlay, R.string.setup_overlay_reason, null, status, onOpen)
        SetupRow(SetupItem.BATTERY, R.string.setup_battery, R.string.setup_battery_reason, batterySteps, status, onOpen)
    }
}

/**
 * The battery row's steps: the labels on Beacon's App info page, in the order the person taps them.
 *
 * Xiaomi phones keep their own wording on every version. Other phones get the wording of stock
 * Android's Settings for their version: Android 16's was read and followed on the emulator, the
 * older ones come from Android's own Settings source and have not been seen on a screen.
 *
 * It lives here, beside the card that shows it, because it chooses words, not a Settings page;
 * [SetupIntents] keeps only the pages and the maker check. It is a pure function of its two
 * arguments, so plain JUnit tests check every branch without starting an Android of each version.
 *
 * @param manufacturer the phone maker's name, as `Build.MANUFACTURER`; "Xiaomi" in any letter case
 *   gets Xiaomi's steps.
 * @param sdkInt the phone's Android version, as `Build.VERSION.SDK_INT`; a version newer than Beacon
 *   knows gets the newest wording.
 * @return the steps string's resource id.
 */
@StringRes
internal fun batteryStepsRes(manufacturer: String, sdkInt: Int): Int = when {
    SetupIntents.isXiaomi(manufacturer) -> R.string.setup_battery_steps_xiaomi
    // Android 10 and 11: Battery (under Advanced) has no Unrestricted choice, only the Battery optimization list.
    sdkInt <= Build.VERSION_CODES.R -> R.string.setup_battery_steps_api29
    // Android 12 and 12L: the Battery page has Unrestricted on it.
    sdkInt <= Build.VERSION_CODES.S_V2 -> R.string.setup_battery_steps_api31
    // Android 13: an early update renamed the row App battery usage; its page still has Unrestricted on it.
    sdkInt == Build.VERSION_CODES.TIRAMISU -> R.string.setup_battery_steps_api33
    // Android 14 (from a 2024 update) and later, and any newer version: Unrestricted is behind the words
    // Allow background usage, whose switch would restrict Beacon instead.
    else -> R.string.setup_battery_steps_api34
}

/**
 * One grant: its name, reason, ON/OFF pill and "Open settings" button.
 *
 * @param item the grant.
 * @param nameRes the row's name.
 * @param reasonRes the plain-words reason.
 * @param stepsRes the steps line shown under the reason, or null for none.
 * @param status the current grants, for the pill and the spoken state.
 * @param onOpen called when the button is tapped.
 */
@Composable
private fun SetupRow(item: SetupItem, nameRes: Int, reasonRes: Int, stepsRes: Int?, status: SetupStatus, onOpen: (SetupItem) -> Unit) {
    val name = stringResource(nameRes)
    val on = status.isOn(item)
    val state = stringResource(if (on) R.string.setup_state_on else R.string.setup_state_off)
    val openFor = stringResource(R.string.setup_open_for, name)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // The name, reason and pill read as one item with the state; the pill wraps under the text at large fonts.
        Layout(
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(name, style = BeaconType.RowTitle.copy(color = BeaconColors.White))
                    BasicText(stringResource(reasonRes), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
                    if (stepsRes != null) {
                        BasicText(stringResource(stepsRes), style = BeaconType.Body.copy(color = BeaconColors.White))
                    }
                }
                OnOffPill(on = on, modifier = Modifier.testTag(SETUP_PILL_TAG))
            },
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { stateDescription = state },
            measurePolicy = TextAndPillPolicy(hasTile = false),
        )
        OutlineButton(
            text = stringResource(R.string.setup_open),
            onClick = { onOpen(item) },
            content = BeaconColors.White,
            icon = null,
            minHeight = 64.dp,
            style = BeaconType.SmallButton,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = openFor },
        )
    }
}
