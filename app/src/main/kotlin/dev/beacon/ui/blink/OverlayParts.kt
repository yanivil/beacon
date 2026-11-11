package dev.beacon.ui.blink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.components.FilledButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why these parts are shared: the Test alert overlay and the real alert
 * screen show the same top card and the same Stop button, so a person who
 * has tried the test knows what a real alert looks like. One copy means the
 * two can't drift apart.
 *
 * Why the cards are black with a white border: the background flashes any
 * of five colours, white and yellow included. White text on black cards
 * keeps 21:1 over every one of them, and the border marks each card's edge
 * when the background is black.
 */

/** The width of every card's white border. */
private val CardBorder = 3.dp

/** The smallest gap between an overlay's parts. */
internal val MinGap = 16.dp

/**
 * Draws an overlay card: black, with a 3 dp white border and rounded corners.
 *
 * @param radius the corner radius.
 * @param horizontal the design's padding at the start and end.
 * @param vertical the design's padding at the top and bottom.
 * @return this modifier with the card's background, border and padding.
 */
internal fun Modifier.overlayCard(radius: Dp, horizontal: Dp, vertical: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return background(BeaconColors.Black, shape)
        .border(CardBorder, BeaconColors.White, shape)
        // The border is drawn inside the bounds, so the padding grows by its width, as CSS's border-box does.
        .padding(horizontal = horizontal + CardBorder, vertical = vertical + CardBorder)
}

/**
 * The top card: a small heading and a large one, both headings; TalkBack
 * announces the large one when the screen opens.
 *
 * @param label the small heading, such as "Test alert" or "New message".
 * @param title the large heading, such as the test's title or the app's name.
 */
@Composable
internal fun TitleCard(label: String, title: String) {
    Column(
        Modifier.fillMaxWidth().overlayCard(radius = 24.dp, horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionHeading(label)
        BeaconText(
            text = title,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            style = BeaconType.OverlayTitle.copy(color = BeaconColors.White),
        )
    }
}

/**
 * The Stop button: white, with a 4 dp black border, at least 96 dp tall.
 *
 * @param onStop called when the button is tapped.
 * @param modifier the modifier that places the button; it also carries any
 *   semantics the caller adds, such as a traversal order.
 */
@Composable
internal fun StopButton(onStop: () -> Unit, modifier: Modifier = Modifier) {
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
        modifier = modifier,
    )
}
