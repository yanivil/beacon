package dev.beacon.setup

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/*
 * Why Beacon reads Xiaomi's Autostart: on HyperOS, with Autostart off, the
 * phone refuses to connect Beacon's notification listener after a restart or
 * an update, so Beacon hears no messages until someone opens it (the phone
 * checks' P5). The owner decided on 11 November 2026 that the Finish setup
 * card gets a fourth row for it on Xiaomi phones.
 *
 * Lint has no check for this reflective call, so nothing is suppressed; the
 * call is on Android's "unsupported" list, which Android 16 still allows
 * (the test phone logged it as `api=unsupported`), but a later Android may
 * block it.
 *
 * Why a hidden method, and why it may fail: Xiaomi keeps Autostart in a
 * private app-op, 10008, with no public name (the test phone's `appops`
 * shows it as "MIUIOP(10008)"). Android's only way to read an app-op by its
 * number is the hidden AppOpsManager.checkOpNoThrow(int, int, String). Android
 * may block hidden methods for apps, so the read is tried by reflection and
 * any failure, or a mode other than allowed or ignored, is UNKNOWN. An
 * UNKNOWN row shows its steps and button but no On/Off, and doesn't keep the
 * card open, because Beacon couldn't tell when it is done.
 */

/** Xiaomi's Background autostart for Beacon, as far as an app can read it. */
enum class AutostartState {
    /** The phone isn't a Xiaomi, so it has no such setting. */
    NOT_XIAOMI,

    /** Autostart is allowed. */
    ON,

    /** Autostart is not allowed. */
    OFF,

    /** A Xiaomi whose Autostart couldn't be read. */
    UNKNOWN,
}

/** Xiaomi's private app-op number for Autostart. */
internal const val MIUI_OP_AUTO_START = 10008

/**
 * Turns the maker and Xiaomi's app-op mode into an Autostart state.
 *
 * @param manufacturer the phone maker's name, as `Build.MANUFACTURER`.
 * @param mode reads the Autostart app-op's mode, or null when it can't; asked only on a Xiaomi.
 * @return [AutostartState.NOT_XIAOMI] on other phones; on a Xiaomi ON for an allowed mode, OFF for an
 *   ignored one, and UNKNOWN otherwise.
 */
internal fun autostartState(manufacturer: String, mode: () -> Int?): AutostartState {
    if (!SetupIntents.isXiaomi(manufacturer)) return AutostartState.NOT_XIAOMI
    return when (mode()) {
        AppOpsManager.MODE_ALLOWED -> AutostartState.ON
        AppOpsManager.MODE_IGNORED -> AutostartState.OFF
        else -> AutostartState.UNKNOWN
    }
}

/**
 * Reads this app's mode for an app-op by its number, through Android's hidden method.
 *
 * @param context any context, for the app-ops service and the package name.
 * @param op the app-op's number.
 * @return the mode, or null if Android blocks or lacks the method, or the read fails in any way.
 */
internal fun hiddenOpMode(context: Context, op: Int): Int? = try {
    val ops = context.getSystemService(AppOpsManager::class.java)
    val method = AppOpsManager::class.java.getMethod(
        "checkOpNoThrow",
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        String::class.java,
    )
    method.invoke(ops, op, Process.myUid(), context.packageName) as? Int
} catch (_: ReflectiveOperationException) {
    // A blocked or missing hidden method (NoSuchMethodException), or anything the method itself throws,
    // such as an op number the phone doesn't know (wrapped in InvocationTargetException).
    null
} catch (_: RuntimeException) {
    // A missing app-ops service (a null receiver) or a refused call: a failed read must never crash Home.
    null
}

/**
 * Reads Beacon's Autostart state on this phone.
 *
 * @param context any context.
 * @return the state; see [autostartState].
 */
fun readAutostart(context: Context): AutostartState =
    autostartState(Build.MANUFACTURER) { hiddenOpMode(context, MIUI_OP_AUTO_START) }
