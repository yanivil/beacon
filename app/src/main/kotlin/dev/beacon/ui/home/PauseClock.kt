package dev.beacon.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.beacon.settings.Pause
import dev.beacon.settings.isPausedAt
import dev.beacon.settings.resumeAtMillis
import kotlinx.coroutines.delay

/*
 * Why Home watches the clock itself (and again on every return to the front): the stored timed pause only changes when
 * something writes it (the resume alarm, Beacon coming to the front, the
 * screen turning on). Home may be open and showing PAUSED when the deadline
 * passes, so it waits for the deadline and asks again, which turns the card ON
 * at the resume time without waiting for a write.
 */

/**
 * The longest single wait before the wall clock is read again, in milliseconds.
 *
 * Why: delay() runs on a clock that stops during deep sleep, so one long wait for the deadline would wake
 * late by about the time the phone slept, and Home would keep saying PAUSED after the resume time.
 * Re-reading the wall clock at least once a minute while awake keeps that error under a minute.
 */
private const val RECHECK_STEP_MILLIS = 60_000L

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
            // Wait in short steps and re-read the wall clock each time (see RECHECK_STEP_MILLIS).
            while (nowEpochMillis() < deadline) delay(minOf(deadline - nowEpochMillis(), RECHECK_STEP_MILLIS))
            recheck++
        }
        // Coming back to the front: re-read now, so Home is right the moment Beacon is opened.
        LifecycleResumeEffect(pause) {
            recheck++
            onPauseOrDispose {}
        }
    }
    // Reading recheck here subscribes this composition to it, so the bump above recomposes and re-asks the clock.
    return recheck.let { pause.isPausedAt(nowEpochMillis()) }
}
