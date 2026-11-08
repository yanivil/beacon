package dev.beacon.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.beacon.ui.theme.BeaconTheme

/**
 * The single activity of Beacon; every screen is a Compose destination
 * inside it.
 */
class MainActivity : ComponentActivity() {
    /**
     * Draws edge to edge (the theme's bars are transparent) and shows
     * Beacon's screen.
     *
     * @param savedInstanceState the saved state; Compose restores the saved tab from it.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Scoped to this activity, so it outlives rotation and font-size changes.
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            BeaconTheme { BeaconAppScreen(viewModel) }
        }
    }
}
