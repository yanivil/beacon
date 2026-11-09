package dev.beacon.ui.blink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.AlertState

/*
 * Why only a title card and Stop: a real alert shows no message content, so
 * nothing private can appear on a locked screen or be read aloud. A real
 * alert also lasts a minute or more, so the test overlay's countdown and
 * details are left out.
 *
 * Why Stop is read first: Stop sits at the bottom, where TalkBack would reach
 * it last, but it is the one control a person needs at once. A traversal
 * index below the title's, inside one traversal group, puts it first without
 * moving it on screen.
 *
 * Why the column is at least the window's height and also scrolls: as in the
 * test overlay, the two parts spread over the window by default, and at the
 * largest font or in landscape they scroll instead of being cut off.
 */

/**
 * The real alert screen: the window blinks while a card says a message came
 * in and from which app, with a Stop button. It never shows message content.
 *
 * @param running the running alert, whose clock drives the blink.
 * @param appName the name of the app the message came from, such as "WhatsApp messages".
 * @param onStop called by the Stop button.
 */
@Composable
fun AlertScreen(running: AlertState.Running, appName: String, onStop: () -> Unit) {
    BlinkScreen(running.clock, running.request.colour) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp)
                    .semantics { isTraversalGroup = true },
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                TitleCard(label = stringResource(R.string.alert_title), title = appName)
                Spacer(Modifier.height(MinGap))
                StopButton(onStop, Modifier.fillMaxWidth().semantics { traversalIndex = -1f })
            }
        }
    }
}
