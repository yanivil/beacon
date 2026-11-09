package dev.beacon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.ui.components.BeaconIcon
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.components.FilledButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType
import dev.beacon.ui.theme.LocalContentColour

/*
 * Why two different cards: ON and PAUSED must differ by more than colour. ON
 * is the yellow card with a bolt and the word ON; PAUSED is a dark card with
 * a thick white border, a pause icon and the word PAUSED. Each carries the
 * one button that changes the state, with a label that says what it does.
 */

/**
 * The alerts card: the ON card when [paused] is false, the PAUSED card when true.
 *
 * @param paused whether alerts are paused.
 * @param resumeTime the formatted time a timed pause ends, or null for an open pause (and on the ON card).
 * @param onPause called by the ON card's "Pause all alerts" button.
 * @param onResume called by the PAUSED card's "Turn alerts back ON" button.
 */
@Composable
fun AlertsCard(paused: Boolean, resumeTime: String?, onPause: () -> Unit, onResume: () -> Unit) {
    if (paused) PausedCard(resumeTime, onResume) else OnCard(onPause)
}

/**
 * The yellow ON card.
 *
 * @param onPause called when "Pause all alerts" is tapped.
 */
@Composable
private fun OnCard(onPause: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Accent, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // The heading takes its colour from the local content colour: dark on yellow (14:1).
            CompositionLocalProvider(LocalContentColour provides BeaconColors.OnAccent) {
                SectionHeading(stringResource(R.string.alerts_heading))
            }
            StateLine(BeaconIcons.Bolt, BeaconColors.OnAccent, stringResource(R.string.alerts_on))
        }
        BasicText(
            stringResource(R.string.alerts_on_body),
            style = BeaconType.BodyStrong.copy(color = BeaconColors.OnAccent),
        )
        FilledButton(
            text = stringResource(R.string.alerts_pause_button),
            onClick = onPause,
            background = BeaconColors.Page,
            content = BeaconColors.White,
            icon = BeaconIcons.Pause,
            minHeight = 80.dp,
            style = BeaconType.Button,
            cornerRadius = 18.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The dark PAUSED card with a 4 dp white border.
 *
 * @param resumeTime the formatted time the pause ends, or null when it has no end.
 * @param onResume called when "Turn alerts back ON" is tapped.
 */
@Composable
private fun PausedCard(resumeTime: String?, onResume: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Card, shape)
            .border(4.dp, BeaconColors.White, shape)
            // The design's 16 / 20 dp padding plus the 4 dp border, which is drawn inside the bounds.
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Set explicitly: the heading must be white on the dark card whatever the surrounding colour.
            CompositionLocalProvider(LocalContentColour provides BeaconColors.White) {
                SectionHeading(stringResource(R.string.alerts_heading))
            }
            StateLine(BeaconIcons.Pause, BeaconColors.White, stringResource(R.string.alerts_paused))
        }
        BasicText(
            stringResource(R.string.alerts_paused_body),
            style = BeaconType.BodyStrong.copy(color = BeaconColors.White),
        )
        // Only a timed pause comes back by itself; an open one stays paused until the user acts.
        if (resumeTime != null) {
            BasicText(
                stringResource(R.string.alerts_back_on_at, resumeTime),
                style = BeaconType.BodyStrong.copy(color = BeaconColors.White),
            )
        }
        FilledButton(
            text = stringResource(R.string.alerts_resume_button),
            onClick = onResume,
            background = BeaconColors.Accent,
            content = BeaconColors.OnAccent,
            icon = BeaconIcons.Bolt,
            minHeight = 80.dp,
            style = BeaconType.Button,
            cornerRadius = 18.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The big state word with its 48 dp icon, such as bolt and "ON". At the
 * largest font the 56 sp word wraps below the icon rather than being cut off.
 *
 * @param icon the decorative icon.
 * @param colour the icon and text colour.
 * @param word the state word.
 */
@Composable
private fun StateLine(icon: ImageVector, colour: Color, word: String) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        BeaconIcon(icon, colour, 48.dp)
        BasicText(word, style = BeaconType.State.copy(color = colour))
    }
}

/**
 * The banner shown above the app rows while alerts are paused: a 3 dp white
 * border, a pause icon and a sentence, so the pause is visible even to
 * someone who has scrolled past the alerts card.
 */
@Composable
fun PausedBanner() {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .border(3.dp, BeaconColors.White, shape)
            // The design's 16 / 12 dp padding plus the 3 dp border, which is drawn inside the bounds.
            .padding(horizontal = 19.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BeaconIcon(BeaconIcons.Pause, BeaconColors.White, 24.dp)
        BasicText(
            stringResource(R.string.apps_paused_banner),
            modifier = Modifier.weight(1f),
            style = BeaconType.Body.copy(color = BeaconColors.White, fontWeight = FontWeight.ExtraBold),
        )
    }
}
