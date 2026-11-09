package dev.beacon.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.beacon.BeaconApp
import dev.beacon.alert.AlertKind
import dev.beacon.alert.ListenerConnection
import dev.beacon.ui.theme.BeaconTheme
import java.io.IOException
import kotlinx.coroutines.launch

/*
 * Why the system bars are always dark: Beacon's page is always #0B0B0D,
 * whatever the phone's theme. With the default (automatic) bar style, a phone
 * in its light theme drew dark clock and battery icons on that dark page
 * (about 1:1) and put a light grey scrim behind 3-button navigation with white
 * buttons on it (about 1.2:1). The dark style asks for light icons and buttons
 * on a transparent bar, so the page and the tab bar's own #141418 show behind
 * them, and it turns off the system's contrast scrim.
 *
 * Why the alert rules live in onStart, onResume and onStop: opening Beacon is
 * how a person ends a light-only real alert (no alert window exists to tap),
 * and a test must never outlive the screen that shows it. Rotation stops and
 * recreates the activity too, but the test is meant to carry on through it, so
 * onStop skips a stop caused by a configuration change.
 */

/**
 * The single activity of Beacon; every screen is a Compose destination
 * inside it.
 */
class MainActivity : ComponentActivity() {
    /**
     * Draws edge to edge with light system-bar icons on transparent bars, and
     * shows Beacon's screen.
     *
     * @param savedInstanceState the saved state; Compose restores the saved tab from it.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            // Scoped to this activity, so it outlives rotation and font-size changes.
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            BeaconTheme { BeaconAppScreen(viewModel) }
        }
    }

    /**
     * Ends a real alert: opening Beacon is how a light-only alert is stopped.
     * A test is left alone.
     */
    override fun onStart() {
        super.onStart()
        (application as BeaconApp).alertController.stopIf(AlertKind.REAL)
    }

    /**
     * Re-binds the message listener at once if access is granted but the
     * system dropped it. Right after a cold start this can run before the
     * system has bound the listener, which costs one harmless off-and-on.
     * Also cleans up a timed pause that ended while Beacon was away, and
     * sets the resume alarm and redraws the widget.
     */
    override fun onResume() {
        super.onResume()
        ListenerConnection.reconnectIfDropped(this)
        val app = application as BeaconApp
        // On the app's scope, not the activity's: the cleanup should finish even if the screen closes at once.
        app.appScope.launch {
            try {
                app.pauseCoordinator.catchUp()
            } catch (_: IOException) {
                // The settings can't be read or saved: leave things as they are; every screen checks the clock anyway.
            }
        }
    }

    /**
     * Ends a running test when Beacon's main screen goes away (Home, another
     * app, the screen turning off), but not when the activity is only being
     * recreated for rotation or a font change, so the countdown carries on.
     * A real alert is left running.
     */
    override fun onStop() {
        super.onStop()
        // Rotation stops the old activity before the new one starts; the test must survive it.
        if (!isChangingConfigurations) {
            (application as BeaconApp).alertController.stopIf(AlertKind.TEST)
        }
    }
}
