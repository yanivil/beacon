package dev.beacon.spike

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/*
 * Why these are switchable: Step 1 has to compare the ways of showing an alert from the background,
 * under the same real trigger, rather than assume which one Android and HyperOS allow. "Armed" stays
 * off by default so the camera doesn't flash for the owner's everyday messages between tests.
 */

/** How the full-screen blink is shown. */
enum class Route {
    /** Camera light only. */
    NONE,

    /** Starts the blink screen directly, which relies on "show over other apps". */
    ACTIVITY,

    /** Asks Android to open the blink screen through a full-screen notification. */
    FULL_SCREEN_INTENT,

    /** Draws a window over other apps. */
    OVERLAY,
}

/** Where the camera light's code runs. */
enum class Host {
    /** Inside the notification listener, which needs no foreground-service start. */
    LISTENER,

    /** In a foreground service, which Android may refuse to start from the background. */
    SERVICE,
}

/**
 * The current test setup, kept between runs so a test can change one thing at a time.
 *
 * @property armed True when a notification should fire the alert.
 * @property route How the full-screen blink is shown.
 * @property host Where the camera light's code runs.
 * @property preset The blink speed.
 * @property torch True to blink the camera light.
 * @property seconds How long an alert runs.
 */
data class SpikeConfig(
    val armed: Boolean,
    val route: Route,
    val host: Host,
    val preset: BlinkPreset,
    val torch: Boolean,
    val seconds: Int,
) {
    /**
     * Applies a setup carried by an adb test notification. Android won't deliver a command to the
     * main screen while the phone is locked, and a lock-screen test can't unlock a phone with a PIN,
     * so the notification that triggers the test also carries its setup. Real notifications never
     * come through here, so "armed" stays off for them.
     *
     * @param text Space-separated key=value pairs. Unknown keys and values are ignored.
     * @return This setup with the given values changed.
     */
    fun withOverrides(text: String): SpikeConfig {
        val values = text.split(' ').filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        return copy(
            armed = values[ARMED]?.toBooleanStrictOrNull() ?: armed,
            route = Route.entries.firstOrNull { it.name == values[ROUTE] } ?: route,
            host = Host.entries.firstOrNull { it.name == values[HOST] } ?: host,
            // Goes through the preset type, so a test notification can't set a raw blink rate.
            preset = values[PRESET]?.let { BlinkPreset.fromName(it) } ?: preset,
            torch = values[TORCH]?.toBooleanStrictOrNull() ?: torch,
            seconds = values[SECONDS]?.toIntOrNull() ?: seconds,
        )
    }

    /**
     * Saves this setup.
     *
     * @param context Any context.
     */
    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(ARMED, armed)
            .putString(ROUTE, route.name)
            .putString(HOST, host.name)
            .putString(PRESET, preset.name)
            .putBoolean(TORCH, torch)
            .putInt(SECONDS, seconds)
            .apply()
    }

    companion object {
        private const val PREFS = "spike"
        private const val ARMED = "armed"
        private const val ROUTE = "route"
        private const val HOST = "host"
        private const val PRESET = "preset"
        private const val TORCH = "torch"
        private const val SECONDS = "seconds"

        /**
         * Loads the saved setup, or the safe default of not armed.
         *
         * @param context Any context.
         * @return The setup.
         */
        fun load(context: Context): SpikeConfig {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return SpikeConfig(
                armed = prefs.getBoolean(ARMED, false),
                route = Route.entries.firstOrNull { it.name == prefs.getString(ROUTE, null) } ?: Route.NONE,
                host = Host.entries.firstOrNull { it.name == prefs.getString(HOST, null) } ?: Host.LISTENER,
                preset = BlinkPreset.fromName(prefs.getString(PRESET, null)),
                torch = prefs.getBoolean(TORCH, true),
                seconds = prefs.getInt(SECONDS, 10),
            )
        }
    }
}

/** Fires one test alert according to the current setup. */
object AlertTrigger {
    /**
     * Fires the alert for a detected notification.
     *
     * @param context The listener's context.
     * @param source What was detected, for the log.
     * @param overrides Setup carried by an adb test notification, or null for a real notification.
     */
    fun fire(context: Context, source: AlertSource, overrides: String? = null) {
        val saved = SpikeConfig.load(context)
        val config = if (overrides != null) saved.withOverrides(overrides) else saved
        if (!config.armed) {
            SpikeLog.write(context, "alert.notArmed", "source=$source")
            return
        }
        if (AlertRunner.isRunning) {
            SpikeLog.write(context, "alert.alreadyRunning", "source=$source")
            return
        }
        SpikeLog.write(context, "alert.fired", "source=$source $config")
        startHost(context, config, source)
        showRoute(context, config)
    }

    /**
     * Starts the camera-light part of the alert.
     *
     * @param context The listener's context.
     * @param config The current setup.
     * @param source What was detected, for the log.
     */
    private fun startHost(context: Context, config: SpikeConfig, source: AlertSource) {
        val reason = "notification:$source"
        when (config.host) {
            Host.LISTENER -> AlertRunner.start(context, config.preset, config.seconds, config.torch, reason)
            Host.SERVICE -> BlinkService.start(context, config.preset, config.seconds, reason)
        }
    }

    /**
     * Shows the full-screen part of the alert.
     *
     * @param context The listener's context.
     * @param config The current setup.
     */
    private fun showRoute(context: Context, config: SpikeConfig) {
        when (config.route) {
            Route.NONE -> Unit
            Route.ACTIVITY -> context.startActivity(
                BlinkActivity.intent(context, config.preset, config.seconds, Route.ACTIVITY),
            )
            Route.FULL_SCREEN_INTENT -> postFullScreen(context, config)
            Route.OVERLAY -> OverlayBlink.show(context, config.preset, config.seconds)
        }
    }

    /**
     * Posts a notification that asks Android to open the blink screen.
     *
     * @param context The listener's context.
     * @param config The current setup.
     */
    private fun postFullScreen(context: Context, config: SpikeConfig) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Android 14+ can withhold this permission, so the log records what it says at the time.
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            manager.canUseFullScreenIntent()
        SpikeLog.write(context, "fullScreen.posting", "allowed=$allowed")
        val open = PendingIntent.getActivity(
            context,
            0,
            BlinkActivity.intent(context, config.preset, config.seconds, Route.FULL_SCREEN_INTENT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, Notifications.CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_spike)
            .setContentTitle(context.getString(R.string.notification_alert))
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .build()
        manager.notify(Notifications.ID_ALERT, notification)
    }
}
