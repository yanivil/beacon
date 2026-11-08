package dev.beacon.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.BlinkSpeed
import dev.beacon.settings.ColourMode
import dev.beacon.settings.CustomLength
import dev.beacon.settings.DurationChoice
import dev.beacon.settings.FlashColour
import dev.beacon.ui.WithFontScale
import dev.beacon.ui.assertNoTextOverflow
import dev.beacon.ui.hasRole
import dev.beacon.ui.theme.BeaconTheme
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/*
 * Why these tests: Alert settings is where a senior changes how alerts look,
 * so each group must be a real RadioButton group that reports "Selected", the
 * Custom panel must appear only for Custom, the Shorter/Longer buttons must
 * switch off at the ends, and only one app's colour grid may be open. The
 * fake actions write into state, so the screen redraws as it does in the app.
 */

/** Robolectric Compose tests for the Alert settings screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlertSettingsScreenTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** The settings the screen shows now; the fake actions and tests change it. */
    private var last by mutableStateOf(BeaconSettings.DEFAULT)

    /** The app whose colour grid is open now, or null. */
    private var editing by mutableStateOf<MessageSource?>(null)

    /** Actions that apply each change to [last] and [editing], like the view model does. */
    private val actions = SettingsActions(
        setDuration = { last = last.copy(duration = it) },
        stepCustom = { longer ->
            last = last.copy(customLength = if (longer) last.customLength.longer() else last.customLength.shorter())
        },
        setSpeed = { last = last.copy(speed = it) },
        setColourMode = { last = last.copy(colourMode = it) },
        setGlobalColour = { last = last.copy(globalColour = it) },
        setAppColour = { source, colour ->
            val current = last.apps.getValue(source)
            last = last.copy(apps = last.apps + (source to current.copy(colour = colour)))
        },
        setEditingApp = { editing = it },
    )

    /**
     * Shows the screen.
     *
     * @param settings the settings to start with.
     * @param editingApp the app whose grid starts open.
     * @param fontScale the font scale to show it at.
     */
    private fun show(settings: BeaconSettings = BeaconSettings.DEFAULT, editingApp: MessageSource? = null, fontScale: Float = 1f) {
        last = settings
        editing = editingApp
        rule.setContent {
            BeaconTheme {
                WithFontScale(fontScale) {
                    // Like the app shell: a scrolling column with the shell's side padding.
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        AlertSettingsScreen(last, editing, actions)
                    }
                }
            }
        }
    }

    /** The duration options are RadioButtons, 1 min is chosen by default, and a tap changes it. */
    @Test fun durationRadioGroupDefaultOneMinute() {
        show()
        rule.onNode(hasRole(Role.RadioButton) and hasText("1 min")).assertIsSelected()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Until I stop it")).performScrollTo().performClick()
        assertEquals(DurationChoice.UNTIL_STOPPED, last.duration)
    }

    /** The Custom length panel exists only while Custom is chosen. */
    @Test fun customPanelOnlyWhenCustom() {
        show()
        rule.onNodeWithText("Custom length").assertDoesNotExist()
        last = BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM)
        rule.onNodeWithText("Custom length").assertExists().assert(isHeading())
        // Once in the Custom option's sub-line and once as the panel's big value.
        rule.onAllNodes(hasText("10 min")).assertCountEquals(2)
        rule.onNodeWithText("From 1 to 60 minutes.").assertExists()
    }

    /** Shorter is off at 1 minute and Longer is off at 60, and a step changes the value. */
    @Test fun shorterDisabledAtOneLongerAtSixty() {
        show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(1)))
        rule.onNodeWithText("Shorter").assertIsNotEnabled()
        rule.onNodeWithText("Longer").assertIsEnabled().performScrollTo().performClick()
        assertEquals(2, last.customLength.minutes)
        last = BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(60))
        rule.onNodeWithText("Longer").assertIsNotEnabled()
        rule.onNodeWithText("Shorter").assertIsEnabled()
    }

    /** Medium is chosen by default, Fast carries its note, and a tap picks a speed. */
    @Test fun speedsWithFastNote() {
        show()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Medium", substring = true)).assertIsSelected()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Fast", substring = true))
            .assertTextContains("Highest safe speed", substring = true)
            .assertTextContains("90 blinks a minute", substring = true)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Fast", substring = true)).performScrollTo().performClick()
        assertEquals(BlinkSpeed.FAST, last.speed)
    }

    /** In same-colour mode there is exactly one swatch grid. */
    @Test fun sameColourShowsOneSwatchGrid() {
        show()
        rule.onAllNodes(hasRole(Role.RadioButton) and hasText("Yellow")).assertCountEquals(1)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Red")).performScrollTo().performClick()
        assertEquals(FlashColour.RED, last.globalColour)
    }

    /** The colour-mode options switch the mode. */
    @Test fun colourModeOptionSwitchesMode() {
        show()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Same color for all apps")).assertIsSelected()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Different color for each app")).performScrollTo().performClick()
        assertEquals(ColourMode.PER_APP, last.colourMode)
    }

    /** Per-app cards open one grid at a time, with spoken labels for Change and Done. */
    @Test fun perAppCardsOneOpenAtATime() {
        show(BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP), editingApp = MessageSource.WHATSAPP)
        rule.onNodeWithContentDescription("Done choosing color for WhatsApp messages").assertExists()
        rule.onNodeWithContentDescription("Change color for Telegram").performScrollTo().performClick()
        assertEquals(MessageSource.TELEGRAM, editing)
        rule.onAllNodes(hasRole(Role.RadioButton) and hasText("Red")).assertCountEquals(1)
        rule.onNodeWithContentDescription("Done choosing color for Telegram").performScrollTo().performClick()
        assertEquals(null, editing)
    }

    /** Picking a swatch in an open card sets that app's colour. */
    @Test fun openCardPicksAppColour() {
        show(BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP), editingApp = MessageSource.SMS)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Red")).performScrollTo().performClick()
        assertEquals(FlashColour.RED, last.colourFor(MessageSource.SMS))
    }

    /** Nothing is cut off at the largest font size, with every panel open. */
    @Test fun noOverflowAtLargestFont() {
        show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, colourMode = ColourMode.PER_APP), MessageSource.SMS, fontScale = 2.0f)
        rule.assertNoTextOverflow()
    }

    /** At the largest font each app's button sits below its name, and no name line ends mid-word. */
    @Test fun perAppHeaderStacksAtLargestFont() {
        show(BeaconSettings.DEFAULT.copy(colourMode = ColourMode.PER_APP), fontScale = 2.0f)
        val context = ApplicationProvider.getApplicationContext<Context>()
        MessageSource.entries.forEach { source ->
            val name = rule.onNodeWithText(context.getString(source.nameRes)).fetchSemanticsNode()
            val button = rule.onNodeWithContentDescription(
                context.getString(dev.beacon.R.string.colour_change_for, context.getString(source.nameRes)),
            ).fetchSemanticsNode()
            assertTrue("button below name for $source", button.boundsInRoot.top >= name.boundsInRoot.bottom)
            val results = mutableListOf<TextLayoutResult>()
            name.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            val layout = results.first()
            val text = layout.layoutInput.text.text
            for (line in 0 until layout.lineCount - 1) {
                val end = layout.getLineEnd(line)
                assertTrue("\"$text\" breaks mid-word at line $line", text[end - 1].isWhitespace() || text[end].isWhitespace())
            }
        }
    }
}
