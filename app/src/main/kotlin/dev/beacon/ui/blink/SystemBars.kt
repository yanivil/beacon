package dev.beacon.ui.blink

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/*
 * Why the bars are hidden while a flash shows: the status bar's white icons
 * sit on the flash colour, and on Yellow they were about 1.4:1, far under the
 * 7:1 the app promises. Hiding the bars removes the problem instead of
 * drawing a patch over it. A swipe from the screen edge brings them back for
 * a moment (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE), so the clock and battery
 * can still be read, and they show again for good when the flash ends.
 */

/**
 * Finds the activity that hosts this context.
 *
 * @return the activity, or null when the context isn't inside one (a preview).
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    // A themed or wrapped context is not itself the activity: look through it.
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Hides the status and navigation bars while this is in the composition, and
 * shows them again when it leaves. A swipe from the edge shows them briefly.
 * It does nothing when the view isn't in an activity's window.
 */
@Composable
fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose {}
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousBehaviour = controller.systemBarsBehavior
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                // Leave the window as it was found, for the screens that come after.
                controller.systemBarsBehavior = previousBehaviour
            }
        }
    }
}
