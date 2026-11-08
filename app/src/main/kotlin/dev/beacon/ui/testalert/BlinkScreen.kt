package dev.beacon.ui.testalert

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import dev.beacon.alert.BlinkClock
import dev.beacon.settings.FlashColour
import dev.beacon.ui.theme.BeaconColors

/*
 * Why the colour is read from the clock once per frame: the camera light
 * flips on BlinkClock's schedule, and the screen must stay in step with it.
 * Asking the clock "colour or black?" at each frame, instead of keeping a
 * timer here, means the screen flips at the first frame after each
 * half-period boundary: never early, so never faster than the preset, and
 * never more than 3 flashes a second (WCAG 2.3.1). A rebuilt screen (after
 * rotation) asks the same clock and carries on in the same phase.
 *
 * Why the frame time is read from SystemClock.uptimeMillis: BlinkClock and
 * the controller's end use that clock; the frame's own timestamp is on a
 * different time base.
 *
 * Why the colour fills the whole window but the content doesn't: the blink
 * shows behind the status and navigation bars too (the app draws edge to
 * edge), while the cards keep clear of the bars and any camera cutout.
 */

/**
 * The background colour at one moment of an alert.
 *
 * @param clock the alert's timing.
 * @param colour the flash colour.
 * @param nowUptimeMillis the moment, as a `SystemClock.uptimeMillis()` value.
 * @return the flash colour during the clock's colour phase, otherwise black.
 */
internal fun blinkColour(clock: BlinkClock, colour: FlashColour, nowUptimeMillis: Long): Color =
    if (clock.isColourPhase(nowUptimeMillis)) Color(colour.argb) else BeaconColors.Black

/**
 * A window-filling background that blinks between [colour] and black on
 * [clock]'s schedule, with [content] on top, clear of the system bars.
 *
 * @param clock the running alert's timing; the same clock drives the camera light.
 * @param colour the flash colour.
 * @param content what to show over the blink. It receives a reader for the
 *   current frame's uptime, so a countdown uses the same moment as the colour.
 *   Reading it inside a composable recomposes that composable every frame, so
 *   read it through derivedStateOf for anything that changes less often.
 */
@Composable
fun BlinkScreen(clock: BlinkClock, colour: FlashColour, content: @Composable (nowUptimeMillis: () -> Long) -> Unit) {
    // The latest frame's uptime; it starts at "now" so the very first draw already has a phase.
    val now = remember(clock) { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val readNow = remember(now) { { now.longValue } }
    LaunchedEffect(clock) {
        // Ends when the screen leaves the composition; until then, one read of the clock per frame.
        while (true) withFrameMillis { now.longValue = SystemClock.uptimeMillis() }
    }
    Box(
        Modifier
            .fillMaxSize()
            // Read while drawing, so a flip only redraws the background, not the cards.
            .drawBehind { drawRect(blinkColour(clock, colour, now.longValue)) },
    ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { content(readNow) }
    }
}
