package dev.beacon.setup

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/*
 * Why three plain yes/no checks: Beacon can only flash for a message when the
 * person has granted three things in Android's own Settings (spec section 9).
 * On Xiaomi phones a fourth, Background autostart, keeps Beacon listening
 * after a restart; its state may be unreadable (see Autostart.kt).
 * Android gives no callback when a grant changes, so Home asks again whenever
 * it comes back into view. Each check is a cheap system call, so asking again
 * on the main thread costs nothing noticeable.
 */

/** The things the person must grant, in the order Home lists them; Autostart only on Xiaomi phones. */
enum class SetupItem {
    /** Notification access: how Beacon learns that a message arrived. */
    NOTIFICATIONS,

    /** Display over other apps: lets the flash cover other apps and the lock screen. */
    OVERLAY,

    /** Battery set to No restrictions, so alerts still arrive while the phone sleeps. */
    BATTERY,

    /** Xiaomi's Background autostart, so Beacon hears messages again after the phone restarts. */
    AUTOSTART,
}

/**
 * Which of the three grants are in place.
 *
 * @property notificationAccess true when Beacon may listen for notifications.
 * @property displayOverApps true when Beacon may draw over other apps.
 * @property batteryUnrestricted true when Android does not restrict Beacon's battery use.
 * @property autostart Xiaomi's Background autostart; [AutostartState.NOT_XIAOMI] on other phones.
 */
data class SetupStatus(
    val notificationAccess: Boolean,
    val displayOverApps: Boolean,
    val batteryUnrestricted: Boolean,
    val autostart: AutostartState = AutostartState.NOT_XIAOMI,
) {
    /**
     * True when every grant Beacon can check is in place, so the Finish setup card is not needed. An
     * Autostart that can't be read doesn't keep the card open: Beacon couldn't tell when it is done.
     */
    val complete: Boolean
        get() = notificationAccess && displayOverApps && batteryUnrestricted && autostart != AutostartState.OFF

    /**
     * Whether one grant is in place.
     *
     * @param item the grant to look at.
     * @return true when it is granted.
     */
    fun isOn(item: SetupItem): Boolean = when (item) {
        SetupItem.NOTIFICATIONS -> notificationAccess
        SetupItem.OVERLAY -> displayOverApps
        SetupItem.BATTERY -> batteryUnrestricted
        SetupItem.AUTOSTART -> autostart == AutostartState.ON
    }

    /**
     * Whether the card has a row for one grant.
     *
     * @param item the grant.
     * @return false only for Autostart on a phone that isn't a Xiaomi.
     */
    fun shows(item: SetupItem): Boolean = item != SetupItem.AUTOSTART || autostart != AutostartState.NOT_XIAOMI

    /**
     * Whether one grant's state is known, so its row shows On or Off.
     *
     * @param item the grant.
     * @return false only for an Autostart that couldn't be read.
     */
    fun stateKnown(item: SetupItem): Boolean = item != SetupItem.AUTOSTART || autostart != AutostartState.UNKNOWN
}

/**
 * Reads the grants from Android.
 *
 * @param context any context; the application context is kept.
 */
class SetupChecker(context: Context) {
    /** The application context, so the checker never holds an activity. */
    private val context: Context = context.applicationContext

    /**
     * Asks Android which grants are in place right now.
     *
     * @return the current status.
     */
    fun check(): SetupStatus {
        val packageName = context.packageName
        val power = context.getSystemService(PowerManager::class.java)
        return SetupStatus(
            notificationAccess = packageName in NotificationManagerCompat.getEnabledListenerPackages(context),
            displayOverApps = Settings.canDrawOverlays(context),
            // A phone with no power service cannot restrict anything we could detect: treat as not granted.
            batteryUnrestricted = power?.isIgnoringBatteryOptimizations(packageName) ?: false,
            autostart = readAutostart(context),
        )
    }
}
