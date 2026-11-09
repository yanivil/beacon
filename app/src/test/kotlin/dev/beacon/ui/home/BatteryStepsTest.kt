package dev.beacon.ui.home

import dev.beacon.R
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why a plain test of the choice: the Robolectric tests in SetupCardStepsTest
 * run one Android version per branch, because each version needs its own
 * Android jar. This one checks every version from minSdk up, so each boundary
 * between two wordings is pinned on both sides.
 */

/** Plain JVM tests for which battery steps the Finish setup card shows. */
class BatteryStepsTest {
    /** A Xiaomi phone keeps its own steps on every Android version. */
    @Test fun xiaomiKeepsItsStepsOnEveryVersion() {
        for (sdk in 29..37) {
            assertEquals("API $sdk", R.string.setup_battery_steps_xiaomi, batteryStepsRes(xiaomi = true, sdkInt = sdk))
        }
    }

    /** Other phones get the wording of their Android version's App info page, with each boundary checked. */
    @Test fun otherPhonesFollowTheAndroidVersion() {
        val expected = mapOf(
            // Android 10 and 11: Battery, then the Battery optimization list.
            29 to R.string.setup_battery_steps_api29,
            30 to R.string.setup_battery_steps_api29,
            // Android 12 and 12L: Battery, then Unrestricted.
            31 to R.string.setup_battery_steps_api31,
            32 to R.string.setup_battery_steps_api31,
            // Android 13: App battery usage, then Unrestricted.
            33 to R.string.setup_battery_steps_api33,
            // Android 14 and later: App battery usage, Allow background usage, then Unrestricted.
            34 to R.string.setup_battery_steps_api34,
            35 to R.string.setup_battery_steps_api34,
            36 to R.string.setup_battery_steps_api34,
            // A newer Android than Beacon knows keeps the newest wording.
            37 to R.string.setup_battery_steps_api34,
        )
        expected.forEach { (sdk, res) ->
            assertEquals("API $sdk", res, batteryStepsRes(xiaomi = false, sdkInt = sdk))
        }
    }
}
