package dev.beacon

import android.os.Looper
import java.util.concurrent.TimeUnit
import org.robolectric.Shadows.shadowOf

/*
 * Why a shared helper: Robolectric pauses the main looper, and DataStore runs
 * its edit transform back on the caller's thread (here the main one), so a
 * single idle() races the store. Tests that wait for a store write wait with
 * this bounded loop instead of sleeping a fixed time.
 */

/**
 * Runs the main looper until [done] holds, for at most 5 seconds.
 *
 * @param done the condition to wait for.
 */
internal fun idleMainUntil(done: () -> Boolean) {
    val main = shadowOf(Looper.getMainLooper())
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    while (!done() && System.nanoTime() < deadline) {
        main.idle()
        Thread.onSpinWait()
    }
}
