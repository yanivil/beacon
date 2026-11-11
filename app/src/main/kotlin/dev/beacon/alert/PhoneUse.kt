package dev.beacon.alert

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

/*
 * Why "in use" means a screen that is on and unlocked: the owner decided on
 * 11 November 2026, after the phone checks, that a real alert flashes only
 * when the screen is off or the lock screen is showing. While the person is
 * using the phone the flash was disruptive, and the message app's own banner
 * already shows them the message. A phone left on its lock screen isn't in
 * use, so it still flashes; so does an always-on display, which Android
 * reports as not interactive.
 */

/**
 * Tells whether the person is using the phone right now.
 *
 * @param context any context, for the power and keyguard services.
 * @return true when the screen is on and the lock screen isn't showing.
 */
fun phoneInUse(context: Context): Boolean {
    val power = context.getSystemService(PowerManager::class.java)
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    return power.isInteractive && !keyguard.isKeyguardLocked
}
