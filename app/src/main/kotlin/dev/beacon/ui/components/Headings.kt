package dev.beacon.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.beacon.ui.theme.BeaconType
import dev.beacon.ui.theme.LocalContentColour

/*
 * Why two heading components: TalkBack users move through a screen by
 * heading, so every title and section name must be marked heading(). Putting
 * the mark and the 36 sp / 28 sp bold styles in one place means no screen can
 * show a heading that TalkBack skips or that is under 28 sp.
 */

/**
 * A screen's H1, such as "Beacon" or "Alert settings": 36 sp, weight 800.
 *
 * @param text the visible title.
 * @param modifier the modifier for the text.
 */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier.semantics { heading() },
        style = BeaconType.ScreenTitle.copy(color = LocalContentColour.current),
    )
}

/**
 * A section heading, such as "Blink speed": 28 sp, weight 800.
 *
 * @param text the visible heading.
 * @param modifier the modifier for the text.
 */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier.semantics { heading() },
        style = BeaconType.Heading.copy(color = LocalContentColour.current),
    )
}
