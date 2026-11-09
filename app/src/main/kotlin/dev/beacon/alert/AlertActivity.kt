package dev.beacon.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import dev.beacon.BeaconApp
import dev.beacon.R
import dev.beacon.ui.blink.AlertScreen
import dev.beacon.ui.theme.BeaconTheme

/*
 * Why a separate activity: a real alert must appear over other apps and the
 * lock screen, and turn the screen on, without Beacon's own screens behind it.
 * It lives in its own task with no affinity and no recents entry, so it never
 * drags Beacon's main screen forward. It only mirrors the controller: the
 * controller owns the clock, so a rotation (which recreates this activity)
 * redraws from the same start time and the blink does not restart.
 *
 * Every way out ends the alert itself (Stop, Back, Home, the power button),
 * because a half-ended alert would leave the light blinking with no screen.
 */

/**
 * Shows a running real alert full screen, and closes whenever none is running.
 */
class AlertActivity : ComponentActivity() {
    private val controller: AlertController
        get() = (application as BeaconApp).alertController

    private var screenOffReceiver: BroadcastReceiver? = null

    /**
     * Sets up the window, then either shows the running real alert or closes.
     *
     * @param savedInstanceState the saved state; unused, the controller holds all state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val running = realRunning()
        if (running == null) {
            // Launched with nothing to show (for example a stale intent): leave at once.
            finishAndRemoveTask()
            return
        }
        // The window title is what TalkBack reads when the window opens.
        title = getString(R.string.alert_window_title, getString(running.request.source!!.nameRes))

        // Back ends the alert rather than hiding it behind the lock screen or another app.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = endAndClose()
            },
        )
        registerScreenOffReceiver()

        // Closing must not depend on Compose: collecting from CREATED keeps working while the
        // activity is stopped (for example just after Home), when no frame is composed.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                controller.state.collect { if (realRunning() == null) finishAndRemoveTask() }
            }
        }

        setContent {
            BeaconTheme {
                val state by controller.state.collectAsState()
                val current = (state as? AlertState.Running)?.takeIf { it.request.kind == AlertKind.REAL }
                // When no real alert runs the collector above closes the window; draw nothing meanwhile.
                if (current != null) {
                    // A real alert always has a source; the empty fallback only avoids a crash.
                    val name = current.request.source?.let { getString(it.nameRes) }.orEmpty()
                    AlertScreen(current, name) { endAndClose() }
                }
            }
        }
    }

    /**
     * Ends the alert when the user presses Home.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        endAndClose()
    }

    /**
     * Stops listening for the screen turning off.
     */
    override fun onDestroy() {
        screenOffReceiver?.let { unregisterReceiver(it) }
        screenOffReceiver = null
        super.onDestroy()
    }

    /**
     * Registers a receiver that ends the alert when the screen turns off. This
     * window keeps the screen on, so screen-off means the power button.
     */
    private fun registerScreenOffReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = endAndClose()
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenOffReceiver = receiver
    }

    /**
     * Ends the alert and closes this window at once, but only when a real alert is running, so a
     * lingering or stale instance can never stop a test alert or anything else.
     */
    private fun endAndClose() {
        if (realRunning() != null) controller.stop()
        finishAndRemoveTask()
    }

    /**
     * Reads the controller's state, keeping only a running real alert.
     *
     * @return the running real alert, or null when idle or a test is running.
     */
    private fun realRunning(): AlertState.Running? =
        (controller.state.value as? AlertState.Running)?.takeIf { it.request.kind == AlertKind.REAL }

    companion object {
        /**
         * Builds the intent that opens the alert window from outside an activity.
         *
         * @param context any context.
         * @return an intent with FLAG_ACTIVITY_NEW_TASK, as required from a non-activity context.
         */
        fun intent(context: Context): Intent =
            Intent(context, AlertActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
