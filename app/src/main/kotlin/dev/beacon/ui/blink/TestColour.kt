package dev.beacon.ui.blink

import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.ColourMode
import dev.beacon.settings.FlashColour

/*
 * Why the test picks one app in per-app mode: a test isn't a message, so it
 * has no app of its own, yet each app has its own colour. It shows the colour
 * the user is most likely thinking about (spec section 8): the app whose
 * colour card is open on Alert settings, else the first app that is ON, else
 * Texts. The overlay names that app, so the colour is never a puzzle.
 */

/**
 * Chooses the colour a Test alert flashes, and the app it stands for.
 *
 * @param s the current settings.
 * @param editingApp the app whose colour card is open on Alert settings, or null.
 * @return the colour, and the app named in the overlay's label; the app is
 *   null in same-colour mode, where the colour belongs to no single app.
 */
fun testColour(s: BeaconSettings, editingApp: MessageSource?): Pair<FlashColour, MessageSource?> {
    if (s.colourMode == ColourMode.SAME_FOR_ALL) return s.globalColour to null
    val app = editingApp
        // The map is public, so a missing entry counts as the app's default, as everywhere else.
        ?: MessageSource.entries.firstOrNull { s.apps[it]?.enabled ?: it.defaultEnabled }
        // Every app OFF: Texts is the first app on Home, so its colour stands in.
        ?: MessageSource.SMS
    return s.colourFor(app) to app
}
