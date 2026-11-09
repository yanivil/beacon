package dev.beacon.ui.home

import dev.beacon.R
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why plain JUnit for every branch: the choice of steps is a pure function of
 * the maker's name and the Android version, so each branch and each boundary
 * is checked here without starting an Android of that version. Robolectric
 * would need its own Android jar for every version it runs (about 420 MB for
 * Android 10, 12 and 13); the one Robolectric test of the card's text runs at
 * the default version (HomeScreenTest.batteryStepsOnThisPhone).
 */

/** Plain JVM tests for which battery steps the Finish setup card shows. */
class BatteryStepsTest {
    /** A Xiaomi phone keeps its own steps on every Android version, whatever the letter case of its maker's name. */
    @Test fun xiaomiKeepsItsStepsOnEveryVersion() {
        for (maker in listOf("Xiaomi", "xiaomi", "XIAOMI")) {
            for (sdk in 29..37) {
                assertEquals("$maker, API $sdk", R.string.setup_battery_steps_xiaomi, batteryStepsRes(maker, sdk))
            }
        }
    }

    /** Other phones get the wording of their Android version's App info page, with each boundary checked on both sides. */
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
            // Android 14 and later: App battery usage, the words Allow background usage, then Unrestricted.
            34 to R.string.setup_battery_steps_api34,
            35 to R.string.setup_battery_steps_api34,
            36 to R.string.setup_battery_steps_api34,
            // A newer Android than Beacon knows keeps the newest wording.
            37 to R.string.setup_battery_steps_api34,
        )
        // Robolectric's own maker name is one of the "other" phones too.
        for (maker in listOf("Google", "samsung", "robolectric", "")) {
            expected.forEach { (sdk, res) ->
                assertEquals("$maker, API $sdk", res, batteryStepsRes(maker, sdk))
            }
        }
    }

    /** A name that only contains "xiaomi" is not Xiaomi; the match is on the whole name. */
    @Test fun onlyTheExactMakerNameCounts() {
        assertEquals(R.string.setup_battery_steps_api34, batteryStepsRes("NotXiaomi", 36))
    }
}
