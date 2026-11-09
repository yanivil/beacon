package dev.beacon.alert

import android.content.Context
import android.os.PowerManager
import android.provider.Settings

/*
 * Why an interface: the controller needs five phone services (a wake lock, the
 * heat check, the "display over other apps" permission and the alert window).
 * Hiding them behind AlertPlatform keeps the Android calls in one place and
 * lets the controller's tests use a fake that records what was asked.
 *
 * Why a wake lock at all: the alert must keep blinking the light and the
 * screen clock while the phone is otherwise asleep. A partial wake lock keeps
 * only the CPU awake and always has a timeout, so a bug can never keep the
 * phone awake for good.
 */

/** The phone services the [AlertController] uses. */
interface AlertPlatform {
    /**
     * Holds the alert's wake lock for [timeoutMillis]; when it is already held, renews it
     * (the lock is not reference counted, so one release always frees it).
     *
     * @param timeoutMillis How long the lock may be held before the system frees it.
     */
    fun holdWakeLock(timeoutMillis: Long)

    /** Releases the wake lock. Safe to call when it isn't held. */
    fun releaseWakeLock()

    /**
     * Whether the phone is already too hot for the camera light.
     *
     * @return True at THERMAL_STATUS_SEVERE or worse.
     */
    fun isTooHot(): Boolean

    /**
     * Starts listening to the heat; replaces an earlier listener.
     *
     * @param onTooHot Called on the main thread each time the status is SEVERE or worse.
     */
    fun watchHeat(onTooHot: () -> Unit)

    /** Stops listening to the heat. Safe to call when not listening. */
    fun stopWatchingHeat()

    /**
     * Whether "Display over other apps" is granted.
     *
     * @return True when the alert window may open over other apps.
     */
    fun canShowOverOtherApps(): Boolean

    /** Opens the alert window. A refused start is absorbed, which leaves a light-only alert. */
    fun showAlertScreen()
}

/**
 * The real phone services.
 *
 * @param context Any context; only the application context is kept.
 */
class AndroidAlertPlatform(context: Context) : AlertPlatform {
    private val app: Context = context.applicationContext
    private val powerManager: PowerManager = app.getSystemService(PowerManager::class.java)
    private val wakeLock: PowerManager.WakeLock =
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "beacon:alert").apply {
            // One release must free it, however many times it was renewed.
            setReferenceCounted(false)
        }
    private var heatListener: PowerManager.OnThermalStatusChangedListener? = null

    /**
     * Acquires the wake lock with a timeout; acquiring again renews the timeout.
     *
     * @param timeoutMillis How long the lock may be held.
     */
    override fun holdWakeLock(timeoutMillis: Long) = wakeLock.acquire(timeoutMillis)

    /** Releases the wake lock if it is held. */
    override fun releaseWakeLock() {
        // release() on a lock that timed out or was never taken would be harmless when not
        // reference counted, but the check keeps it explicit.
        if (wakeLock.isHeld) wakeLock.release()
    }

    /**
     * Reads the current thermal status.
     *
     * @return True at SEVERE or worse.
     */
    override fun isTooHot(): Boolean = powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE

    /**
     * Listens to thermal status on the main thread.
     *
     * @param onTooHot Called when the status is SEVERE or worse.
     */
    override fun watchHeat(onTooHot: () -> Unit) {
        stopWatchingHeat()
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            if (status >= PowerManager.THERMAL_STATUS_SEVERE) onTooHot()
        }
        try {
            powerManager.addThermalStatusListener(app.mainExecutor, listener)
            heatListener = listener
        } catch (e: RuntimeException) {
            // The system refused the listener (the thermal service can be missing or down).
            // The alert must still run, so it goes on without a heat watch.
        }
    }

    /** Removes the thermal listener, if any. */
    override fun stopWatchingHeat() {
        heatListener?.let { powerManager.removeThermalStatusListener(it) }
        heatListener = null
    }

    /**
     * Reads the overlay permission.
     *
     * @return True when "Display over other apps" is on for Beacon.
     */
    override fun canShowOverOtherApps(): Boolean = Settings.canDrawOverlays(app)

    /** Starts [AlertActivity]; the system may refuse (for example without the permission). */
    override fun showAlertScreen() {
        try {
            app.startActivity(AlertActivity.intent(app))
        } catch (e: RuntimeException) {
            // Refused start: the alert carries on as light-only.
        }
    }
}
