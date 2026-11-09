package dev.beacon.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.ColourMode
import dev.beacon.settings.Pause
import dev.beacon.setup.SetupItem
import dev.beacon.setup.SetupStatus
import dev.beacon.ui.WithFontScale
import dev.beacon.ui.formatResumeTime
import dev.beacon.ui.assertNoTextOverflow
import dev.beacon.ui.components.TRAILING_ICON_TAG
import dev.beacon.ui.theme.BeaconTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/*
 * Why these tests: Home is the first screen a senior sees, so its two alert
 * states, the three switches (order, ON/OFF, flash lines), the summary and
 * the largest-font and right-to-left layouts are pinned here. The actions
 * record their names so each tap is checked to reach the right callback.
 */

/** Robolectric Compose tests for the Home screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** The names of the actions called so far, in order. */
    private val calls = mutableListOf<String>()

    /** The settings Home shows; changing it recomposes the screen. */
    private var settings by mutableStateOf(BeaconSettings.DEFAULT)

    /** The clock Home reads, which a test can move. */
    private var clockMillis = System.currentTimeMillis()

    /** The setup status Home shows; null means not read yet. */
    private var setup by mutableStateOf<SetupStatus?>(null)

    /** Actions that only record their own names (and the app, for a toggle). */
    private val actions = HomeActions(
        pauseAll = { calls += "pauseAll" },
        resume = { calls += "resume" },
        toggleApp = { calls += "toggle:${it.name}" },
        openSettings = { calls += "openSettings" },
        openSetup = { calls += "openSetup:${it.name}" },
    )

    /**
     * Shows Home with [initial] settings.
     *
     * @param initial the settings to start with.
     * @param fontScale the font scale to show it at.
     * @param direction the layout direction.
     */
    private fun show(
        initial: BeaconSettings = BeaconSettings.DEFAULT,
        fontScale: Float = 1f,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        settings = initial
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BeaconTheme {
                    WithFontScale(fontScale) {
                        // Like the app shell: a scrolling column with the shell's side padding, so nothing is squeezed into the window height.
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                            HomeScreen(settings, setup, actions) { clockMillis }
                        }
                    }
                }
            }
        }
    }

    /** The H1 says Beacon and is a heading for TalkBack. */
    @Test fun titleIsBeaconAndHeading() {
        show()
        rule.onNodeWithText("Beacon").assert(isHeading())
    }

    /** The ON card's button pauses alerts without a deadline. */
    @Test fun onCardPausesIndefinitely() {
        show()
        rule.onNodeWithText("Pause all alerts").performClick()
        assertEquals(listOf("pauseAll"), calls)
    }

    /** While paused, the PAUSED card and the banner show and the button turns alerts back on. */
    @Test fun pausedCardAndBanner() {
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Indefinite))
        rule.onNodeWithText("PAUSED").assertExists()
        rule.onNodeWithText("All alerts are paused right now.").assertExists()
        rule.onNodeWithText("Visual alerts").assert(isHeading())
        rule.onNodeWithText("Turn alerts back ON").performClick()
        assertEquals(listOf("resume"), calls)
    }

    /** The banner is only there while paused. */
    @Test fun bannerHiddenWhenOn() {
        show()
        rule.onNodeWithText("All alerts are paused right now.").assertDoesNotExist()
    }

    /** There are three switches, in the design's order, with the right state and flash line. */
    @Test fun threeAppSwitchesInOrder() {
        show()
        val rows = rule.onAllNodes(isToggleable())
        rows.assertCountEquals(3)
        rows[0].assertTextContains("Texts (SMS)", substring = true).assertIsOn()
        rows[1].assertTextContains("WhatsApp messages", substring = true).assertIsOn()
        rows[2].assertTextContains("Telegram", substring = true).assertIsOff().assertTextContains("No flash", substring = true)
    }

    /** Tapping a row toggles that app. */
    @Test fun tappingARowToggles() {
        show()
        rule.onAllNodes(isToggleable())[2].performScrollTo().performClick()
        assertEquals(listOf("toggle:${MessageSource.TELEGRAM.name}"), calls)
    }

    /** The flash line names the global colour, or each app's own colour in per-app mode. */
    @Test fun flashLineFollowsColourMode() {
        show()
        rule.onAllNodes(isToggleable())[1].assertTextContains("Yellow flash", substring = true)
        settings = BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP)
        rule.onAllNodes(isToggleable())[1].assertTextContains("Green flash", substring = true)
    }

    /** The summary shows duration, speed and colour, or "Different per app". */
    @Test fun summaryRows() {
        show()
        rule.onNodeWithText("1 minute").assertExists()
        rule.onNodeWithText("Medium").assertExists()
        rule.onNodeWithText("Yellow").assertExists()
        settings = BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP)
        rule.onNodeWithText("Different per app").assertExists()
    }

    /** "Change settings" asks to open the Alert settings tab. */
    @Test fun changeSettingsOpensSettings() {
        show()
        rule.onNodeWithText("Change settings").performScrollTo().performClick()
        assertEquals(listOf("openSettings"), calls)
    }

    /** With no message app installed (as in Robolectric), the design's glyph is shown. */
    @Test fun glyphWhenAppNotInstalled() {
        show()
        rule.onNodeWithTag("glyph_TELEGRAM", useUnmergedTree = true).assertExists()
    }

    /** Installs [packageName] with [icon] as its launcher icon in Robolectric's package manager. */
    private fun install(packageName: String, icon: Drawable) {
        val pm = ApplicationProvider.getApplicationContext<Context>().packageManager
        val info = PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        }
        shadowOf(pm).installPackage(info)
        shadowOf(pm).setApplicationIcon(packageName, icon)
    }

    /** An installed app with a real icon shows that icon, not the glyph. */
    @Test fun installedAppShowsItsOwnIcon() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        install("com.whatsapp", BitmapDrawable(resources, Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)))
        show()
        // The icon is loaded off the main thread, so the glyph shows until it is ready.
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("glyph_WHATSAPP", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("glyph_WHATSAPP", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("glyph_TELEGRAM", useUnmergedTree = true).assertExists()
    }

    /** An icon with no usable size falls back to the glyph and does not crash Home. */
    @Test fun zeroSizeIconFallsBackToGlyph() {
        // A ColorDrawable has intrinsic size -1 x -1, which toBitmap() cannot turn into a bitmap.
        install("com.whatsapp", ColorDrawable(Color.RED))
        show()
        rule.onNodeWithTag("glyph_WHATSAPP", useUnmergedTree = true).assertExists()
    }

    /** Nothing is cut off at the largest font size, in the ON state. */
    @Test fun noOverflowAtLargestFont() {
        show(fontScale = 2.0f)
        rule.assertNoTextOverflow()
    }

    /** Nothing is cut off at the largest font size, in the PAUSED state. */
    @Test fun noOverflowAtLargestFontPaused() {
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Indefinite), fontScale = 2.0f)
        rule.assertNoTextOverflow()
    }

    /** In right-to-left the chevron sits at the left, after the text (which is on the right). */
    @Test fun chevronMirrorsInRtl() {
        show(direction = LayoutDirection.Rtl)
        val chevron = rule.onNodeWithTag(TRAILING_ICON_TAG, useUnmergedTree = true).getBoundsInRoot()
        val text = rule.onNodeWithText("Change settings", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("chevron ${chevron.right} vs text ${text.left}", chevron.right <= text.left)
    }

    /** Matches a node that carries a state description (a setup row's merged item). */
    private val hasState = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

    /** All three grants are in place: no card. */
    @Test fun setupCardHiddenWhenComplete() {
        setup = SetupStatus(true, true, true)
        show()
        rule.onNodeWithText("Finish setup").assertDoesNotExist()
    }

    /** Before the first check there is no card either. */
    @Test fun setupCardHiddenUntilChecked() {
        show()
        rule.onNodeWithText("Finish setup").assertDoesNotExist()
    }

    /** One grant missing shows the whole card: a heading, three rows, each with its state. */
    @Test fun setupCardShowsAllRowsWhenOneIsOff() {
        setup = SetupStatus(notificationAccess = false, displayOverApps = true, batteryUnrestricted = true)
        show()
        rule.onNodeWithText("Finish setup").assert(isHeading())
        rule.onNodeWithText("Beacon needs these to flash for your messages.").assertExists()
        rule.onAllNodes(hasState).assertCountEquals(3)
        rule.onNode(hasText("Notification access") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off")).assertExists()
        rule.onNode(hasText("Display over other apps") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On")).assertExists()
        rule.onNode(hasText("Battery: No restrictions") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On")).assertExists()
    }

    /** Each button names its row for TalkBack, still containing its visible words, and opens that row. */
    @Test fun openSettingsSpeaksTheRow() {
        setup = SetupStatus(false, false, false)
        show()
        rule.onNodeWithContentDescription("Open settings for Display over other apps").performScrollTo().performClick()
        assertEquals(listOf("openSetup:OVERLAY"), calls)
        rule.onNodeWithContentDescription("Open settings for Notification access").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Open settings for Battery: No restrictions").performScrollTo().performClick()
        assertEquals(listOf("openSetup:OVERLAY", "openSetup:NOTIFICATIONS", "openSetup:BATTERY"), calls)
    }

    /** The battery steps follow the maker. */
    @Test fun batteryStepsFollowTheMaker() {
        var xiaomi by mutableStateOf(true)
        rule.setContent {
            BeaconTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SetupCard(SetupStatus(false, false, false), xiaomi, onOpen = {})
                }
            }
        }
        rule.onNodeWithText("Tap Battery saver, then No restrictions.").assertExists()
        rule.onNodeWithText("Tap Battery, then Unrestricted.").assertDoesNotExist()
        xiaomi = false
        rule.onNodeWithText("Tap Battery, then Unrestricted.").assertExists()
        rule.onNodeWithText("Tap Battery saver, then No restrictions.").assertDoesNotExist()
    }

    /** Nothing is cut off at the largest font size with the card shown. */
    @Test fun setupCardNoOverflowAtLargestFont() {
        setup = SetupStatus(false, false, false)
        show(fontScale = 2.0f)
        rule.assertNoTextOverflow()
    }

    /** The card sits between the title and the alerts card. */
    @Test fun setupCardIsAboveTheAlertsCard() {
        setup = SetupStatus(false, true, true)
        show()
        // Positions in the scrolled column: read through the column, which keeps every item at its place.
        val title = rule.onNodeWithText("Beacon").getUnclippedBoundsInRoot().top
        val card = rule.onNodeWithText("Finish setup").getUnclippedBoundsInRoot().top
        val alerts = rule.onNodeWithText("Visual alerts").getUnclippedBoundsInRoot().top
        assertTrue("title $title, card $card, alerts $alerts", title < card && card < alerts)
    }

    /** The text of the resume line for a resume time. */
    private fun resumeLine(resumeAt: Long) =
        "Back ON by itself at ${formatResumeTime(ApplicationProvider.getApplicationContext(), resumeAt)}."

    /** A timed pause says when alerts come back on by themselves. */
    @Test fun timedPauseShowsTheResumeLine() {
        val resumeAt = clockMillis + 3_600_000
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Until(resumeAt)))
        rule.onNodeWithText("PAUSED").assertExists()
        rule.onNodeWithText(resumeLine(resumeAt)).assertExists()
    }

    /** A pause with no end has no resume line. */
    @Test fun openPauseHasNoResumeLine() {
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Indefinite))
        rule.onNodeWithText("PAUSED").assertExists()
        rule.onAllNodes(hasText("Back ON", substring = true)).assertCountEquals(0)
    }

    /** A stored timed pause whose time has passed shows the ON card and no banner. */
    @Test fun homeShowsOnAfterExpiry() {
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Until(clockMillis - 1_000)))
        rule.onNodeWithText("ON").assertExists()
        rule.onNodeWithText("PAUSED").assertDoesNotExist()
        rule.onNodeWithText("All alerts are paused right now.").assertDoesNotExist()
    }

    /** The card turns ON by itself when the clock reaches the deadline, with no new settings. */
    @Test fun cardTurnsOnAtTheDeadline() {
        // Manual time, so the wait is advanced only by this test.
        rule.mainClock.autoAdvance = false
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Until(clockMillis + 2_000)))
        rule.onNodeWithText("PAUSED").assertExists()
        clockMillis += 2_000
        rule.mainClock.advanceTimeBy(2_000)
        // One more frame draws the recomposition the deadline caused.
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("ON").assertExists()
        rule.onNodeWithText("PAUSED").assertDoesNotExist()
        rule.onNodeWithText("All alerts are paused right now.").assertDoesNotExist()
    }

    /** Nothing is cut off at the largest font size with the resume line shown. */
    @Test fun resumeLineNoOverflowAtLargestFont() {
        show(BeaconSettings.DEFAULT.copy(pause = Pause.Until(clockMillis + 3_600_000)), fontScale = 2.0f)
        rule.onNodeWithText("Back ON by itself at", substring = true).assertExists()
        rule.assertNoTextOverflow()
    }
}
