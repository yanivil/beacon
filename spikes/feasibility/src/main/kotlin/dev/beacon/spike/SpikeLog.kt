package dev.beacon.spike

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import java.io.File
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Why a file and not just logcat: a Step 1 result has to be evidence that can be read afterwards.
 * The phone's logger level is set to Warn with a small buffer, and the battery tests run with the
 * cable unplugged, so logcat alone would lose the results. The file is fetched over adb later.
 */

/** Appends timestamped test events to a private file and mirrors them to logcat. */
object SpikeLog {
    private const val TAG = "BeaconSpike"
    private const val FILE_NAME = "spike.log"
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    /**
     * Writes one line to the log.
     *
     * @param context Any context; only the application context is kept for the file path.
     * @param event Short event name such as "torch.stopped".
     * @param details Space-separated key=value details.
     */
    @Synchronized
    fun write(context: Context, event: String, details: String = "") {
        val line = "${LocalTime.now().format(timeFormat)} $event $details".trimEnd()
        // Warn, because the phone's logger level hides anything lower.
        Log.w(TAG, line)
        try {
            File(context.applicationContext.filesDir, FILE_NAME).appendText(line + "\n")
        } catch (e: IOException) {
            // Losing a line must not stop a test that is already running.
            Log.e(TAG, "Could not write the log file", e)
        }
    }
}

/** Reads the device conditions that decide whether an alert result means anything. */
object DeviceState {
    /**
     * Describes battery, heat, screen and power-saving state right now.
     *
     * @param context Any context.
     * @return Space-separated key=value text for the log.
     */
    fun snapshot(context: Context): String {
        val app = context.applicationContext
        val power = app.getSystemService(PowerManager::class.java)
        val keyguard = app.getSystemService(KeyguardManager::class.java)
        // A null receiver reads the last sticky battery broadcast without registering anything.
        val battery = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val tenthsC = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        return "battery=$level tempC=${tenthsC / 10.0} plugged=$plugged " +
            "thermal=${power.currentThermalStatus} screenOn=${power.isInteractive} " +
            "locked=${keyguard.isKeyguardLocked} idle=${power.isDeviceIdleMode} " +
            "powerSave=${power.isPowerSaveMode} " +
            "batteryOptExempt=${power.isIgnoringBatteryOptimizations(app.packageName)}"
    }
}
