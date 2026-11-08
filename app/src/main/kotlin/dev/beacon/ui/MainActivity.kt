package dev.beacon.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.beacon.ui.theme.BeaconTheme

/*
 * Why the system bars are always dark: Beacon's page is always #0B0B0D,
 * whatever the phone's theme. With the default (automatic) bar style, a phone
 * in its light theme drew dark clock and battery icons on that dark page
 * (about 1:1) and put a light grey scrim behind 3-button navigation with white
 * buttons on it (about 1.2:1). The dark style asks for light icons and buttons
 * on a transparent bar, so the page and the tab bar's own #141418 show behind
 * them, and it turns off the system's contrast scrim.
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
}
