package dev.beacon.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.beacon.settings.Pause
import dev.beacon.settings.isPausedAt
import dev.beacon.settings.resumeAtMillis
import kotlinx.coroutines.delay

/*
 * Why Home watches the clock itself: the stored timed pause only changes when
 * something writes it (the resume alarm, Beacon coming to the front, the
 * screen turning on). Home may be open and showing PAUSED when the deadline
 * passes, so it waits for the deadline and asks again, which turns the card ON
 * at the resume time without waiting for a write.
 */

/**
 * Whether alerts are paused right now, re-checked when a timed pause ends.
 *
 * @param pause the stored pause.
 * @param nowEpochMillis the clock, in epoch milliseconds (a parameter so tests can move it).
 * @return true while [pause] is in effect at the clock's time.
 */
@Composable
fun pausedNow(pause: Pause, nowEpochMillis: () -> Long = System::currentTimeMillis): Boolean {
    // Read below, so bumping it recomposes the caller and the clock is asked again.
    var recheck by remember { mutableIntStateOf(0) }
    val deadline = pause.resumeAtMillis()
    if (deadline != null) {
        // Restarts when the pause changes; leaves the composition (and so is cancelled) with the screen.
        LaunchedEffect(pause) {
            // A wake-up can come early (the delay is not exact), so wait until the clock really passed it.
            while (nowEpochMillis() < deadline) delay(deadline - nowEpochMillis())
            recheck++
        }
    }
    // Reading recheck here subscribes this composition to it, so the bump above recomposes and re-asks the clock.
    return recheck.let { pause.isPausedAt(nowEpochMillis()) }
}
