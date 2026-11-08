package dev.beacon.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The single activity of Beacon; every screen is a Compose destination
 * inside it. The content is empty until the screens are added.
 */
class MainActivity : ComponentActivity() {
    /**
     * Draws edge to edge (the theme's bars are transparent) and sets the
     * Compose content.
     *
     * @param savedInstanceState the saved state, unused for now.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {}
    }
}
