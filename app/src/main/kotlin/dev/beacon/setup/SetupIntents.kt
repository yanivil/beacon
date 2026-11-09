package dev.beacon.setup

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.beacon.alert.BeaconListener

/*
 * Why one object for the Settings pages: each "Open settings" button must land
 * on the page where that one grant is switched, with as few taps as Android
 * allows. Phones differ (a maker may remove a page), so opening falls back to
 * Beacon's App info page, and if even that is missing nothing happens rather
 * than a crash.
 */

/** The Settings pages behind the Finish setup buttons. */
object SetupIntents {
    /**
     * The Settings page for one grant.
     *
     * @param item the grant.
     * @param context used for the package name and the listener's name.
     * @param sdkInt the Android version; a parameter so tests can choose it.
     * @return an intent for that page (not yet started).
     */
    // The detail-page constants are inlined strings, and are used only from Android 11 (sdkInt >= 30).
    @SuppressLint("InlinedApi")
    fun intentFor(item: SetupItem, context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Intent = when (item) {
        SetupItem.NOTIFICATIONS ->
            if (sdkInt >= Build.VERSION_CODES.R) {
                // Android 11+: straight to Beacon's own switch.
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, BeaconListener::class.java).flattenToString(),
                )
            } else {
                // Android 10: only the list of all apps with notification access exists.
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
        SetupItem.OVERLAY ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context))
        // Beacon's App info page; the person goes on from there by the steps the row shows above the button.
        SetupItem.BATTERY -> appInfo(context)
    }

    /**
     * Opens the Settings page for one grant. If the phone has no such page
     * it opens Beacon's App info instead, and if that is missing too it does nothing.
     *
     * @param item the grant.
     * @param context where to start the page from.
     */
    fun open(item: SetupItem, context: Context) {
        if (tryStart(context, intentFor(item, context))) return
        // The page is missing on this phone: App info is the next best place to grant it.
        tryStart(context, appInfo(context))
    }

    /**
     * Whether the phone is made by Xiaomi (which shows different battery steps).
     *
     * @param manufacturer the maker's name; defaults to this phone's.
     * @return true for "Xiaomi" in any letter case.
     */
    fun isXiaomi(manufacturer: String = Build.MANUFACTURER): Boolean = manufacturer.equals("xiaomi", ignoreCase = true)

    /**
     * Starts an intent, absorbing a missing or refused page.
     *
     * @param context where to start it from.
     * @param intent what to start.
     * @return true if it started, false if no activity handles it or the phone refused it.
     */
    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        // A non-activity context (such as the application) can only start a page in a new task.
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: RuntimeException) {
        // ActivityNotFoundException, plus SecurityException (some HyperOS settings pages) and
        // IllegalStateException: a setup tap must never crash Beacon, so all fall back.
        false
    }

    /**
     * Beacon's App info intent.
     *
     * @param context used for the package name.
     * @return the intent.
     */
    private fun appInfo(context: Context): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    /**
     * The `package:` address of this app.
     *
     * @param context used for the package name.
     * @return the address.
     */
    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)
}
