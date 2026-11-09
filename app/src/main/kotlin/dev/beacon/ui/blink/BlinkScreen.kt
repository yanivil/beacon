package dev.beacon.ui.blink

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
 * Why black strips behind the system bars: the window draws edge to edge, so
 * a blink behind the status and navigation bars would put their white icons
 * on the flash colour (about 1.4:1 on Yellow). Hiding the bars instead made
 * Android show its one-time "Viewing full screen" panel over the first alert
 * and the first test (D1 in docs/checks/2026-11-step2-pr2.md). So the bars
 * stay visible in their dark style, the strips behind them stay black (white
 * on black is 21:1), and everything else blinks. The cards keep clear of the
 * bars and of any camera cutout. The keyboard inset is left out: no flash
 * screen has a text field.
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
 * What the blink background paints, in order: black over the whole window, then the
 * current phase's colour over everything except the strips behind the system bars.
 *
 * @param size the window's size, in pixels.
 * @param left the system bars' width at the left edge, in pixels (a side navigation bar).
 * @param top the system bars' height at the top, in pixels (the status bar).
 * @param right the system bars' width at the right edge, in pixels (a side navigation bar).
 * @param bottom the system bars' height at the bottom, in pixels (the navigation bar).
 * @param phaseColour the current phase's colour, from [blinkColour]. It may be [Color.Unspecified] as a
 *   placeholder: the caller then paints that rectangle with the phase colour in its draw step, so the
 *   regions can be worked out once and reused across phases.
 * @return each rectangle with its colour, in painting order; a later one covers an earlier one.
 */
internal fun blinkRegions(
    size: Size,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    phaseColour: Color,
): List<Pair<Rect, Color>> {
    // Bars that meet or cross (a tiny window) leave an empty area, never an inverted one.
    val blinking = Rect(left, top, (size.width - right).coerceAtLeast(left), (size.height - bottom).coerceAtLeast(top))
    return listOf(Rect(Offset.Zero, size) to BeaconColors.Black, blinking to phaseColour)
}

/**
 * A window-filling background that blinks between [colour] and black on
 * [clock]'s schedule, with [content] on top, clear of the system bars and the
 * camera cutout. The strips behind the system bars stay black, so the bars'
 * white icons stay readable.
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
    // Taken here; their sizes are read while drawing, so a rotation or a bar change only redraws.
    val bars = WindowInsets.systemBars
    Box(
        Modifier
            .fillMaxSize()
            // The regions are worked out in the cache, which is rebuilt only when the size or the bars change.
            // The phase is read in the draw block, so a flip only redraws the background, not the cards.
            .drawWithCache {
                val regions = blinkRegions(
                    size,
                    left = bars.getLeft(this, layoutDirection).toFloat(),
                    top = bars.getTop(this).toFloat(),
                    right = bars.getRight(this, layoutDirection).toFloat(),
                    bottom = bars.getBottom(this).toFloat(),
                    // Placeholder: the real phase colour is only known per frame, below.
                    phaseColour = Color.Unspecified,
                )
                onDrawBehind {
                    val phaseColour = blinkColour(clock, colour, now.longValue)
                    regions.forEach { (rect, regionColour) ->
                        // The first region is the black base; the others take the current phase's colour.
                        drawRect(if (regionColour == Color.Unspecified) phaseColour else regionColour, rect.topLeft, rect.size)
                    }
                }
            },
    ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))) { content(readNow) }
    }
}
