package dev.beacon.ui.blink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.AlertState
import dev.beacon.alert.BlinkClock
import dev.beacon.alert.TEST_LENGTH_MILLIS
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.components.FilledButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType

/*
 * Why everything comes from the running alert: the controller owns the start
 * time, so the blink, the countdown and the end all come from its clock and
 * the overlay keeps no timer. Rebuilding the overlay (rotation, a font
 * change) reads the same clock and carries on, instead of starting again.
 *
 * Why the cards are black with a white border: the background flashes any
 * of five colours, white and yellow included. White text on black cards
 * keeps 21:1 over every one of them, and the border marks each card's edge
 * when the background is black.
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

/** The width of every card's white border. */
private val CardBorder = 3.dp

/** The smallest gap between the overlay's four parts. */
private val MinGap = 16.dp

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
 * The 3-second Test alert: the whole window blinks while cards say what real
 * alerts will look like, with a countdown and a Stop button. TalkBack sees
 * it as a pane named "Test alert" and announces its title.
 *
 * @param running the running test, whose clock drives the blink and the countdown.
 * @param colourLabel the flash colour's name, with the app's name in per-app mode.
 * @param speedLabel the blink speed and rate, such as "Medium, 60 a minute".
 * @param lengthLabel how long real alerts last, such as "1 minute".
 * @param hasLight true when the phone has a camera light that flashes too.
 * @param onStop called by the Stop button.
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
                    TitleCard()
                    Spacer(Modifier.height(MinGap))
                    CountdownCard(seconds, Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(MinGap))
                    DetailsCard(colourLabel, speedLabel, lengthLabel, hasLight)
                    Spacer(Modifier.height(MinGap))
                    FilledButton(
                        text = stringResource(R.string.stop),
                        onClick = onStop,
                        background = BeaconColors.White,
                        content = BeaconColors.Black,
                        icon = BeaconIcons.Stop,
                        minHeight = 96.dp,
                        style = BeaconType.Stop,
                        cornerRadius = 24.dp,
                        // Black on white: the button's edge shows even while the background flashes white.
                        border = BorderStroke(4.dp, BeaconColors.Black),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** The top card: "Test alert" and the title, both headings; the title is announced when the overlay opens. */
@Composable
private fun TitleCard() {
    Column(
        Modifier.fillMaxWidth().overlayCard(radius = 24.dp, horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionHeading(stringResource(R.string.test_label))
        BasicText(
            text = stringResource(R.string.test_title),
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            style = BeaconType.OverlayTitle.copy(color = BeaconColors.White),
        )
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
        // A bare number, written the same in Hebrew; its words come from the plural below.
        BasicText(seconds.toString(), style = BeaconType.Countdown.copy(color = BeaconColors.White, textAlign = centred))
        BasicText(
            text = pluralStringResource(R.plurals.test_seconds_left, seconds),
            style = BeaconType.BodyStrong.copy(color = BeaconColors.White, textAlign = centred),
        )
    }
}

/**
 * The details card: what the test shows and how long a real alert lasts.
 *
 * @param colourLabel the flash colour's name, with the app's name in per-app mode.
 * @param speedLabel the blink speed and rate.
 * @param lengthLabel how long real alerts last.
 * @param hasLight true when the camera light flashes too.
 */
@Composable
private fun DetailsCard(colourLabel: String, speedLabel: String, lengthLabel: String, hasLight: Boolean) {
    Column(
        Modifier.fillMaxWidth().overlayCard(radius = 24.dp, horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailRow(stringResource(R.string.test_colour), colourLabel)
        DetailRow(stringResource(R.string.test_speed), speedLabel)
        DetailRow(
            stringResource(R.string.test_camera),
            stringResource(if (hasLight) R.string.test_camera_on else R.string.test_camera_none),
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
        BasicText(
            text = label,
            modifier = Modifier.alignByBaseline().padding(end = 12.dp),
            style = BeaconType.Body.copy(color = BeaconColors.TextSecondary),
        )
        BasicText(value, Modifier.alignByBaseline(), style = BeaconType.SmallButton.copy(color = BeaconColors.White))
    }
}

/**
 * Draws an overlay card: black, with a 3 dp white border and rounded corners.
 *
 * @param radius the corner radius.
 * @param horizontal the design's padding at the start and end.
 * @param vertical the design's padding at the top and bottom.
 * @return this modifier with the card's background, border and padding.
 */
private fun Modifier.overlayCard(radius: Dp, horizontal: Dp, vertical: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return background(BeaconColors.Black, shape)
        .border(CardBorder, BeaconColors.White, shape)
        // The border is drawn inside the bounds, so the padding grows by its width, as CSS's border-box does.
        .padding(horizontal = horizontal + CardBorder, vertical = vertical + CardBorder)
}
