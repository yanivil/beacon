package dev.beacon.setup

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/*
 * Why three plain yes/no checks: Beacon can only flash for a message when the
 * person has granted three things in Android's own Settings (spec section 9).
 * Android gives no callback when a grant changes, so Home asks again whenever
 * it comes back into view. Each check is a cheap system call, so asking again
 * on the main thread costs nothing noticeable.
 */

/** The three things the person must grant, in the order Home lists them. */
enum class SetupItem {
    /** Notification access: how Beacon learns that a message arrived. */
    NOTIFICATIONS,

    /** Display over other apps: lets the flash cover other apps and the lock screen. */
    OVERLAY,

    /** Battery set to No restrictions, so alerts still arrive while the phone sleeps. */
    BATTERY,
}

/**
 * Which of the three grants are in place.
 *
 * @property notificationAccess true when Beacon may listen for notifications.
 * @property displayOverApps true when Beacon may draw over other apps.
 * @property batteryUnrestricted true when Android does not restrict Beacon's battery use.
 */
data class SetupStatus(
    val notificationAccess: Boolean,
    val displayOverApps: Boolean,
    val batteryUnrestricted: Boolean,
) {
    /** True when all three grants are in place, so the Finish setup card is not needed. */
    val complete: Boolean get() = notificationAccess && displayOverApps && batteryUnrestricted

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
    }
}

/**
 * Reads the three grants from Android.
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
        )
    }
}
