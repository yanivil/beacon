package dev.beacon

import android.app.Application
import dev.beacon.settings.SettingsRepository

/**
 * Application class for Beacon.
 *
 * Why it exists: app-wide singletons (settings store, later alert state) hang
 * off this class so every screen and service shares them.
 */
class BeaconApp : Application() {
    /** The saved settings; created on first use, once per process (one DataStore per file). */
    val settingsRepository: SettingsRepository by lazy { SettingsRepository.create(this) }
}
