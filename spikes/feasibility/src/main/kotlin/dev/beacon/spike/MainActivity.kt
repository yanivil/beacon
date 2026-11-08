package dev.beacon.spike

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/*
 * Why permissions are granted by hand here rather than over adb: how a person actually gets through
 * these Android and HyperOS screens is part of what Step 1 has to find out.
 *
 * Why commands also arrive over adb: the timing tests need to start at a known moment and repeat
 * exactly. This is a test app that is removed after Step 1, so the exported activity accepting
 * commands is acceptable here. The real app must not do this.
 */

/** The test app's only ordinary screen: permission status, and buttons to grant and run. */
class MainActivity : Activity() {
    private lateinit var status: TextView

    /**
     * Builds the screen and runs any command it was opened with.
     *
     * @param savedInstanceState Unused.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannels(this)
        setContentView(buildScreen())
        runCommand(intent)
    }

    /**
     * Runs a command sent while the screen is already open.
     *
     * @param intent The new intent.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runCommand(intent)
    }

    /** Refreshes the status, and reconnects the listener if the phone has dropped it. */
    override fun onResume() {
        super.onResume()
        status.text = statusText()
        AlertListener.reconnectIfDropped(this)
    }

    /**
     * Builds the screen in code, since this is a throwaway test app.
     *
     * @return The root view.
     */
    private fun buildScreen(): ScrollView {
        val pad = dp(16)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(pad, pad, pad, pad)
        }
        status = bodyText()
        column.addView(header(R.string.title_status))
        column.addView(status)
        column.addView(header(R.string.title_grant))
        column.addView(button(R.string.button_allow_notifications) { requestNotifications() })
        column.addView(
            button(R.string.button_notification_access) {
                open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
        )
        column.addView(
            button(R.string.button_overlay) {
                open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()))
            },
        )
        // This settings screen only exists on Android 14 and later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            column.addView(
                button(R.string.button_full_screen) {
                    open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()))
                },
            )
        }
        column.addView(button(R.string.button_xiaomi) { openXiaomiPermissions() })
        column.addView(header(R.string.title_run))
        column.addView(button(R.string.button_run60) { startSixtyMinuteRun() })
        column.addView(button(R.string.button_stop_all) { stopAll() })

        return ScrollView(this).apply {
            setBackgroundColor(Palette.INK)
            addView(column)
            // Edge-to-edge is enforced at target SDK 35+. A phone in portrait has no side insets.
            // getInsets is API 30+. On API 29 the app isn't drawn behind the system bars anyway.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setOnApplyWindowInsetsListener { view, insets ->
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    view.setPaddingRelative(0, bars.top, 0, bars.bottom)
                    insets
                }
            }
        }
    }

    /**
     * Builds a section header at the project's minimum header size.
     *
     * @param text The header text.
     * @return The view.
     */
    private fun header(text: Int): TextView = TextView(this).apply {
        setText(text)
        textSize = 28f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        setPaddingRelative(0, dp(24), 0, dp(8))
    }

    /**
     * Builds body text at the project's minimum body size.
     *
     * @return The view.
     */
    private fun bodyText(): TextView = TextView(this).apply {
        textSize = 20f
        setTextColor(Color.WHITE)
    }

    /**
     * Builds a text button that meets the project's minimum touch target.
     *
     * @param text The button label.
     * @param onClick What the button does.
     * @return The view.
     */
    private fun button(text: Int, onClick: () -> Unit): Button = Button(this).apply {
        setText(text)
        isAllCaps = false
        textSize = 22f
        gravity = Gravity.CENTER
        setTextColor(Palette.INK)
        setBackgroundColor(Palette.YELLOW)
        minHeight = dp(72)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(12) }
        setOnClickListener { onClick() }
    }

    /**
     * Converts density-independent pixels to pixels.
     *
     * @param value The size in dp.
     * @return The size in pixels.
     */
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * Builds the package address some settings screens need.
     *
     * @return This app's package URI.
     */
    private fun packageUri(): Uri = Uri.fromParts("package", packageName, null)

    /**
     * Describes each permission and setting the tests depend on.
     *
     * @return The status text.
     */
    private fun statusText(): String {
        val notifications = getSystemService(NotificationManager::class.java)
        val power = getSystemService(PowerManager::class.java)
        val listener = ComponentName(this, AlertListener::class.java)
        val fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            notifications.canUseFullScreenIntent()
        return listOf(
            getString(R.string.status_listener, onOff(notifications.isNotificationListenerAccessGranted(listener))),
            getString(R.string.status_notifications, onOff(notifications.areNotificationsEnabled())),
            getString(R.string.status_overlay, onOff(Settings.canDrawOverlays(this))),
            getString(R.string.status_full_screen, onOff(fullScreen)),
            // Optimisation ON means the app is NOT exempt, which is the condition under test.
            getString(R.string.status_battery, onOff(!power.isIgnoringBatteryOptimizations(packageName))),
            getString(R.string.status_armed, onOff(SpikeConfig.load(this).armed)),
        ).joinToString("\n")
    }

    /**
     * Words a state, so it is never shown by colour alone.
     *
     * @param value The state.
     * @return The ON or OFF text.
     */
    private fun onOff(value: Boolean): String =
        getString(if (value) R.string.state_on else R.string.state_off)

    /** Shows Android's own notification permission prompt. */
    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    /**
     * Opens a settings screen, recording it if this phone doesn't have that screen.
     *
     * @param target The settings intent.
     */
    private fun open(target: Intent) {
        try {
            startActivity(target)
        } catch (e: ActivityNotFoundException) {
            SpikeLog.write(this, "settings.missing", "action=${target.action}")
        }
    }

    /**
     * Opens HyperOS's own permissions page, which holds settings stock Android doesn't have, such
     * as background pop-ups and showing on the lock screen.
     */
    private fun openXiaomiPermissions() {
        val xiaomi = Intent("miui.intent.action.APP_PERM_EDITOR")
            .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            .putExtra("extra_pkgname", packageName)
        try {
            startActivity(xiaomi)
        } catch (e: RuntimeException) {
            // This is a private Xiaomi screen: it can be missing or refuse outside callers.
            SpikeLog.write(this, "settings.xiaomiRefused", "error=${e.javaClass.simpleName}")
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
        }
    }

    /** Starts the 60-minute camera light run that Step 1 requires. */
    private fun startSixtyMinuteRun() {
        BlinkService.start(this, BlinkPreset.FAST, SIXTY_MINUTES_SECONDS, "run60")
    }

    /** Stops any running alert. */
    private fun stopAll() {
        AlertRunner.stop(this, "stopAll")
        stopService(Intent(this, BlinkService::class.java))
    }

    /**
     * Runs a test command sent over adb. A launch with no command does nothing.
     *
     * @param intent The intent that opened this screen.
     */
    private fun runCommand(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_COMMAND)) {
            COMMAND_TORCH -> AlertRunner.start(
                context = this,
                preset = BlinkPreset.fromName(intent.getStringExtra(EXTRA_PRESET)),
                seconds = intent.getIntExtra(EXTRA_SECONDS, 30),
                torch = true,
                reason = "adb",
            )
            COMMAND_CONFIG -> saveConfig(intent)
            COMMAND_RUN60 -> startSixtyMinuteRun()
            COMMAND_STOP -> stopAll()
        }
        // Clear it, so rotating or returning to this screen can't run the command again.
        intent?.removeExtra(EXTRA_COMMAND)
    }

    /**
     * Saves a new test setup, changing only the values given.
     *
     * @param intent Carries the values to change.
     */
    private fun saveConfig(intent: Intent) {
        val current = SpikeConfig.load(this)
        val updated = current.copy(
            armed = intent.getBooleanExtra("armed", current.armed),
            route = Route.entries.firstOrNull { it.name == intent.getStringExtra("route") } ?: current.route,
            host = Host.entries.firstOrNull { it.name == intent.getStringExtra("host") } ?: current.host,
            preset = intent.getStringExtra(EXTRA_PRESET)?.let { BlinkPreset.fromName(it) } ?: current.preset,
            torch = intent.getBooleanExtra("torch", current.torch),
            seconds = intent.getIntExtra(EXTRA_SECONDS, current.seconds),
        )
        updated.save(this)
        SpikeLog.write(this, "config.saved", updated.toString())
        status.text = statusText()
    }

    private companion object {
        const val EXTRA_COMMAND = "cmd"
        const val EXTRA_PRESET = "preset"
        const val EXTRA_SECONDS = "seconds"
        const val COMMAND_TORCH = "torch"
        const val COMMAND_CONFIG = "config"
        const val COMMAND_RUN60 = "run60"
        const val COMMAND_STOP = "stop"
        const val SIXTY_MINUTES_SECONDS = 3600
    }
}

/** The design's colours, as used by the test app's text and blink. */
object Palette {
    /** Page background. White text on it measures 19.66:1. */
    val INK: Int = Color.parseColor("#0B0B0D")

    /** Accent and blink colour. [INK] text on it measures 13.93:1. */
    val YELLOW: Int = Color.parseColor("#FFD60A")
}
