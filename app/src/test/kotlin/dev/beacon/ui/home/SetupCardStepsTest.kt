package dev.beacon.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.setup.SetupStatus
import dev.beacon.ui.WithFontScale
import dev.beacon.ui.assertNoTextOverflow
import dev.beacon.ui.theme.BeaconTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Why Robolectric at several Android versions: the card reads the phone's
 * own Android version (Build.VERSION.SDK_INT) to pick the battery steps, so
 * each wording is checked on a phone of that version, not with a number
 * passed in. One version per wording keeps the Android jars to download
 * down to four; BatteryStepsTest checks every boundary. The words come from
 * the emulator's screens (Android 16) and from Android's own Settings source
 * for the older versions, which no emulator here runs.
 */

/** Robolectric tests for the battery steps on the Finish setup card, one Android version per wording. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupCardStepsTest {
    /** Hosts the card alone in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /**
     * Shows the setup card with every row OFF, in a scrolling column with the shell's side padding.
     *
     * @param xiaomi whether the pretend phone is a Xiaomi.
     * @param fontScale the font scale to show it at.
     */
    private fun show(xiaomi: Boolean = false, fontScale: Float = 1f) {
        rule.setContent {
            BeaconTheme {
                WithFontScale(fontScale) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        SetupCard(SetupStatus(false, false, false), xiaomi, onOpen = {})
                    }
                }
            }
        }
    }

    /** Android 10 (API 29): Battery sits under Advanced and leads to the Battery optimization list. */
    @Config(sdk = [29])
    @Test fun android10ShowsTheBatteryOptimizationList() {
        show()
        rule.onNodeWithText(STEPS_API29).assertExists()
    }

    /** Android 12 (API 31): Battery, then Unrestricted. */
    @Config(sdk = [31])
    @Test fun android12ShowsBatteryThenUnrestricted() {
        show()
        rule.onNodeWithText(STEPS_API31).assertExists()
    }

    /** Android 13 (API 33): App battery usage, then Unrestricted. */
    @Config(sdk = [33])
    @Test fun android13ShowsAppBatteryUsageThenUnrestricted() {
        show()
        rule.onNodeWithText(STEPS_API33).assertExists()
    }

    /** Android 16 (API 36), as read on the emulator: App battery usage, Allow background usage, then Unrestricted. */
    @Config(sdk = [36])
    @Test fun android16ShowsAllowBackgroundUsage() {
        show()
        rule.onNodeWithText(STEPS_API34).assertExists()
    }

    /** A Xiaomi phone keeps its own steps, even on an Android version that has a wording of its own. */
    @Config(sdk = [29])
    @Test fun xiaomiKeepsItsStepsOnAndroid10() {
        show(xiaomi = true)
        rule.onNodeWithText("Tap Battery saver, then No restrictions.").assertExists()
        rule.onNodeWithText(STEPS_API29).assertDoesNotExist()
    }

    /** The longest steps (Android 10's) wrap at the largest font size with nothing cut off. */
    @Config(sdk = [29])
    @Test fun longestStepsFitAtLargestFont() {
        show(fontScale = 2.0f)
        rule.onNodeWithText(STEPS_API29).assertExists()
        rule.assertNoTextOverflow()
    }

    private companion object {
        /** Android 10 and 11's steps. */
        const val STEPS_API29 = "Tap Advanced, then Battery, then Battery optimization. Choose All apps, " +
            "tap Beacon, then Don’t optimize, then Done."

        /** Android 12 and 12L's steps. */
        const val STEPS_API31 = "Tap Battery, then Unrestricted."

        /** Android 13's steps. */
        const val STEPS_API33 = "Tap App battery usage, then Unrestricted."

        /** Android 14 and later's steps. */
        const val STEPS_API34 = "Tap App battery usage, then Allow background usage, then Unrestricted."
    }
}
