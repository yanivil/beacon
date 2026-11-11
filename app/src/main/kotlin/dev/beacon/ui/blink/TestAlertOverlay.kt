package dev.beacon.ui.blink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.AlertState
import dev.beacon.alert.BlinkClock
import dev.beacon.alert.TEST_LENGTH_MILLIS
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why everything comes from the running alert: the controller owns the start
 * time, so the blink, the countdown and the end all come from its clock and
 * the overlay keeps no timer. Rebuilding the overlay (rotation, a font
 * change) reads the same clock and carries on, instead of starting again.
 *
 * Why the column is at least as tall as the window and also scrolls: at the
 * default font the four parts spread over the window, as in the design; at
 * the largest font they no longer fit, so the column scrolls instead of
 * cutting off the details or the Stop button.
 *
 * Why the overlay swallows taps: it is drawn over the shell, and Compose
 * passes a tap that no overlay element takes on to the screen below. A tap
 * on the overlay must never pause alerts or switch tabs unseen. The
 * scrolling column already takes taps inside the safe area; an empty
 * handler on the root also covers the strips behind the system bars and any
 * cutout, so the guarantee doesn't depend on how the cards are laid out.
 */

/** The countdown's first number: the test's length in whole seconds. */
private val CountdownMax = (TEST_LENGTH_MILLIS / 1_000).toInt()

/**
 * The whole seconds a test has left, as the countdown shows them.
 *
 * @param clock the test's timing.
 * @param nowUptimeMillis the moment, as a `SystemClock.uptimeMillis()` value.
 * @return the time left rounded up to whole seconds, from 1 to 3. It stays at
 *   1 when no time is left, for the frame before the overlay closes, and at 3
 *   before the start; an alert with no end shows 3.
 */
internal fun countdownSeconds(clock: BlinkClock, nowUptimeMillis: Long): Int {
    // A test always has an end; a clock without one gets the full count rather than a crash.
    val left = clock.remainingMillis(nowUptimeMillis) ?: TEST_LENGTH_MILLIS
    return ((left + 999) / 1_000).toInt().coerceIn(1, CountdownMax)
}

/**
 * The string for the details card's camera row.
 *
 * @param hasLight true when the phone has a camera light.
 * @param lightFlashing true while the light is flashing; false when the heat check has stopped it or it refused.
 * @param lightRefused true when the light stopped because it refused (another app has the camera).
 * @return "Flashing too", "Off while another app uses the camera" (a light that refused), "Off while the
 *   phone is hot" (any other light that has stopped), or "Not on this phone".
 */
@StringRes
internal fun cameraLineRes(hasLight: Boolean, lightFlashing: Boolean, lightRefused: Boolean): Int = when {
    !hasLight -> R.string.test_camera_none
    lightFlashing -> R.string.test_camera_on
    // A refusal isn't heat: saying the phone is hot would be wrong, and a senior might worry.
    lightRefused -> R.string.test_camera_busy
    else -> R.string.test_camera_hot
}

/**
 * The 3-second Test alert: the whole window blinks while cards say what real
 * alerts will look like, with a countdown and a Stop button. TalkBack sees
 * it as a pane named "Test alert" and announces its title.
 *
 * @param running the running test, whose clock drives the blink and the countdown.
 * @param colourLabel the flash colour's name, with the app's name in per-app mode.
 * @param speedLabel the blink speed and rate, such as "Medium, 60 a minute".
 * @param lengthLabel how long real alerts last, such as "1 minute".
 * @param hasLight true when the phone has a camera light that flashes too.
 * @param onStop called by the Stop button, which also takes keyboard focus when the overlay appears.
 */
@Composable
fun TestAlertOverlay(
    running: AlertState.Running,
    colourLabel: String,
    speedLabel: String,
    lengthLabel: String,
    hasLight: Boolean,
    onStop: () -> Unit,
) {
    val paneName = stringResource(R.string.test_label)
    val stopFocus = remember { FocusRequester() }
    Box(
        Modifier
            .fillMaxSize()
            // An empty handler still counts as a hit, so a tap anywhere in the window stops here.
            .pointerInput(Unit) {}
            .semantics { paneTitle = paneName },
    ) {
        BlinkScreen(running.clock, running.request.colour) { now ->
            // Recomposes only when the whole second changes, not on every frame of the blink.
            val seconds by remember(running.clock, now) { derivedStateOf { countdownSeconds(running.clock, now()) } }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
                    // With a MinGap spacer between the parts, SpaceBetween shares the spare
                    // height evenly, like the design's CSS "space-between" with a 16 px gap.
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    TitleCard(label = stringResource(R.string.test_label), title = stringResource(R.string.test_title))
                    Spacer(Modifier.height(MinGap))
                    CountdownCard(seconds, Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(MinGap))
                    DetailsCard(colourLabel, speedLabel, lengthLabel, hasLight, running.lightFlashing, running.lightRefused)
                    Spacer(Modifier.height(MinGap))
                    StopButton(onStop, Modifier.fillMaxWidth().focusRequester(stopFocus))
                    // Why: blocking focus from entering the shell (see BeaconAppScreen) doesn't move focus
                    // that is already there, such as a keyboard user's focus on "Test alert". Taking it for
                    // Stop here makes the overlay own the keyboard from the moment it appears. In touch
                    // mode Compose refuses focus on a button, so a tap-only user sees no change. Only the
                    // test overlay does this; the real alert screen is a window of its own.
                    LaunchedEffect(running.clock) { stopFocus.requestFocus() }
                }
            }
        }
    }
}

/**
 * The countdown card: the seconds left and "seconds left" under it, read by
 * TalkBack as one item and announced again at each new second.
 *
 * @param seconds the whole seconds left, from 1 to 3.
 * @param modifier the modifier that places the card.
 */
@Composable
private fun CountdownCard(seconds: Int, modifier: Modifier) {
    Column(
        modifier
            .overlayCard(radius = 28.dp, horizontal = 36.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val centred = TextAlign.Center
        // A new key each second makes new text nodes inside the same live region, so Compose sends
        // TalkBack a subtree change at every new second, not only when the merged text differs (D4).
        key(seconds) {
            // A bare number, written the same in Hebrew; its words come from the plural below.
            BeaconText(seconds.toString(), style = BeaconType.Countdown.copy(color = BeaconColors.White, textAlign = centred))
            BeaconText(
                text = pluralStringResource(R.plurals.test_seconds_left, seconds),
                style = BeaconType.BodyStrong.copy(color = BeaconColors.White, textAlign = centred),
            )
        }
    }
}

/**
 * The details card: what the test shows and how long a real alert lasts.
 *
 * @param colourLabel the flash colour's name, with the app's name in per-app mode.
 * @param speedLabel the blink speed and rate.
 * @param lengthLabel how long real alerts last.
 * @param hasLight true when the phone has a camera light.
 * @param lightFlashing false when the heat check has stopped the light or it refused.
 * @param lightRefused true when the light stopped because it refused.
 */
@Composable
private fun DetailsCard(
    colourLabel: String,
    speedLabel: String,
    lengthLabel: String,
    hasLight: Boolean,
    lightFlashing: Boolean,
    lightRefused: Boolean,
) {
    Column(
        Modifier.fillMaxWidth().overlayCard(radius = 24.dp, horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailRow(stringResource(R.string.test_colour), colourLabel)
        DetailRow(stringResource(R.string.test_speed), speedLabel)
        DetailRow(
            stringResource(R.string.test_camera),
            stringResource(cameraLineRes(hasLight, lightFlashing, lightRefused)),
        )
        DetailRow(stringResource(R.string.test_real_length), lengthLabel)
    }
}

/**
 * One detail: the label at the start and the value at the end, on their
 * baselines; the value moves to the next line when both don't fit.
 * TalkBack reads the pair as one item, such as "Color, Yellow".
 *
 * @param label the label, in secondary text colour.
 * @param value the value.
 */
@Composable
private fun DetailRow(label: String, value: String) {
    FlowRow(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // The end padding keeps a 12 dp gap to the value when both just fit on one line.
        BeaconText(
            text = label,
            modifier = Modifier.alignByBaseline().padding(end = 12.dp),
            style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
        )
        BeaconText(value, Modifier.alignByBaseline(), style = BeaconType.SmallButton.copy(color = BeaconColors.White))
    }
}
