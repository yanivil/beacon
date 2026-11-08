package dev.beacon

import android.app.Application

/**
 * Application class for Beacon.
 *
 * Why it exists: later tasks hang app-wide singletons (settings store,
 * alert state) off this class so every screen and service shares them.
 * It is empty for now.
 */
class BeaconApp : Application()
