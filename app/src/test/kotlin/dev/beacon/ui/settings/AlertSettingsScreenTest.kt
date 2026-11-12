package dev.beacon.ui.settings

import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import dev.beacon.ui.assertOwnListPositions
import dev.beacon.ui.assertTalkBackReadsList
import dev.beacon.ui.enableAccessibilityService
import dev.beacon.ui.recordAccessibilityEvents
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.app.Application
import org.junit.Assert.assertNull
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

/*
 * Why these tests: Alert settings is where a senior changes how alerts look,
 * so each group must be a real RadioButton group that reports "Selected", the
 * Custom panel must appear only for Custom, the Shorter/Longer buttons must
 * switch off at the ends, and only one app's colour grid may be open. The
 * fake actions write into state, so the screen redraws as it does in the app.
 */

/** The message under the privacy button when the policy can't be opened. */
private const val NO_BROWSER = "Couldn't open a web browser on this phone."

/** Robolectric Compose tests for the Alert settings screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlertSettingsScreenTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** The settings the screen shows now; the fake actions and tests change it. */
    private var last by mutableStateOf(BeaconSettings.DEFAULT)

    /** How many times the screen asked to open the privacy policy. */
    private var policyTaps = 0

    /** What the fake privacy action answers: true as if a browser opened, false as if none could. */
    private var policyOpens = true

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
        openPrivacyPolicy = { policyTaps++; policyOpens },
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

    /** The four duration options, in reading order. */
    private val durationOptions = listOf("1 min", "5 min", "Custom", "Until I stop it").map { hasRole(Role.RadioButton) and hasText(it) }

    /** In the two-column grid of a wide phone, TalkBack reads the duration options as a list of four, "1 of 4" to "4 of 4" (D5). */
    @Config(qualifiers = "w411dp-h914dp")
    @Test fun durationOptionsReadAsAListOfFour() {
        show()
        val one = rule.onNode(durationOptions[0]).getBoundsInRoot()
        val five = rule.onNode(durationOptions[1]).getBoundsInRoot()
        assertEquals("two columns", one.top, five.top)
        rule.assertOwnListPositions(durationOptions)
        rule.assertTalkBackReadsList(durationOptions)
    }

    /** In the one-column grid at 200% font, TalkBack still reads the duration options as a list of four (D5). */
    @Config(qualifiers = "w411dp-h2400dp")
    @Test fun durationOptionsReadAsAListOfFourInOneColumn() {
        show(fontScale = 2.0f)
        val one = rule.onNode(durationOptions[0]).getBoundsInRoot()
        val five = rule.onNode(durationOptions[1]).getBoundsInRoot()
        assertTrue("one column", five.top >= one.bottom)
        rule.assertOwnListPositions(durationOptions)
        rule.assertTalkBackReadsList(durationOptions)
    }

    /** The one-column groups keep reading their own positions: speed "1 of 3" to "3 of 3", colour mode "1 of 2" and "2 of 2". */
    @Config(qualifiers = "w411dp-h2400dp")
    @Test fun singleColumnGroupsReadTheirPositions() {
        show()
        rule.assertTalkBackReadsList(listOf("Slow", "Medium", "Fast").map { hasRole(Role.RadioButton) and hasText(it) })
        rule.assertTalkBackReadsList(
            listOf("Same color for all apps", "Different color for each app").map { hasRole(Role.RadioButton) and hasText(it) },
        )
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

    /**
     * The Custom value stays one polite live region across Longer and Shorter:
     * the same node, read as the new value, with a new text node inside after
     * each step. Each step also sends TalkBack a content-change event from the
     * live region, which is what makes TalkBack read the new value (D4).
     */
    @Test fun customValueStaysOneLiveRegionWithANewTextEachStep() {
        enableAccessibilityService()
        show(BeaconSettings.DEFAULT.copy(duration = DurationChoice.CUSTOM, customLength = CustomLength.of(10)))
        val events = rule.recordAccessibilityEvents()
        val live = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)
        val region = rule.onNode(live).fetchSemanticsNode()
        assertEquals(LiveRegionMode.Polite, region.config[SemanticsProperties.LiveRegion])
        assertTrue("the live region holds its text", region.config.isMergingSemanticsOfDescendants)
        assertEquals(listOf("10 min"), region.config[SemanticsProperties.Text].map { it.text })
        // Longer, then Shorter back: one step each way, 10 -> 15 -> 10 minutes.
        listOf("Longer" to CustomLength.of(10).longer(), "Shorter" to CustomLength.of(10)).forEach { (button, expected) ->
            val textBefore = rule.onNode(live, useUnmergedTree = true).fetchSemanticsNode().children.single().id
            events.clear()
            rule.onNodeWithText(button).performScrollTo().performClick()
            // Compose batches its accessibility events and sends them up to 100 ms after a
            // change, so the test clock moves well past that before the events are checked.
            rule.mainClock.advanceTimeBy(500)
            rule.waitForIdle()
            val after = rule.onNode(live).fetchSemanticsNode()
            val textAfter = rule.onNode(live, useUnmergedTree = true).fetchSemanticsNode().children.single().id
            assertEquals("same live region node after $button", region.id, after.id)
            assertEquals(LiveRegionMode.Polite, after.config[SemanticsProperties.LiveRegion])
            assertEquals(listOf("${expected.minutes} min"), after.config[SemanticsProperties.Text].map { it.text })
            assertTrue("a new text node for the new value after $button", textAfter != textBefore)
            assertTrue(
                "TalkBack gets a content change from the live region (node ${region.id}) after $button; sent: $events",
                events.any { it.type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && it.sourceId == region.id },
            )
        }
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

    /** Each speed shows its own rate line: 30, 60 and 90 blinks a minute. */
    @Test fun everySpeedShowsItsRate() {
        show()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Slow", substring = true))
            .assertTextContains("30 blinks a minute", substring = true)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Medium", substring = true))
            .assertTextContains("60 blinks a minute", substring = true)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Fast", substring = true))
            .assertTextContains("90 blinks a minute", substring = true)
    }

    /**
     * The rate line is a plural, so a translation can change its words with the number (lint's
     * PluralsCandidate). The presets are all "other" in English; "one" is checked so its grammar is right too.
     */
    @Test fun speedRateIsAPlural() {
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        assertEquals("1 blink a minute", res.getQuantityString(dev.beacon.R.plurals.speed_rate, 1, 1))
        BlinkSpeed.entries.forEach { speed ->
            val n = speed.blinksPerMinute
            assertEquals("$n blinks a minute", res.getQuantityString(dev.beacon.R.plurals.speed_rate, n, n))
        }
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

    /*
     * The privacy section (the Play release's in-app privacy link): Google
     * Play's User Data policy wants the privacy policy reachable inside the
     * app too, so it sits at the end of Alert settings with a real button.
     */

    /** The section has a "Privacy" heading, its hint, a "Read the privacy policy" button and the browser note. */
    @Test fun privacySectionShowsHeadingButtonAndNotes() {
        show()
        rule.onNodeWithText("Privacy").performScrollTo().assert(isHeading())
        rule.onNodeWithText("Beacon keeps your settings on this phone and sends nothing anywhere.").performScrollTo()
        rule.onNodeWithText("Read the privacy policy").performScrollTo().assert(hasRole(Role.Button))
        rule.onNodeWithText("Opens in your web browser.").performScrollTo()
    }

    /** A tap asks the shell to open the policy once; the screen itself starts nothing. */
    @Test fun privacyButtonCallsTheAction() {
        show()
        rule.onNodeWithText("Read the privacy policy").performScrollTo().performClick()
        assertEquals(1, policyTaps)
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertNull("the screen starts no activity itself", shadowOf(app).nextStartedActivity)
    }

    /**
     * When the policy can't be opened, the tap shows Beacon's own message under the button,
     * in Beacon's type and colours and announced by TalkBack. A system toast would be small,
     * unchecked for contrast and gone in seconds. A later tap that works clears it.
     */
    @Test fun privacyButtonThatCantOpenShowsAMessage() {
        policyOpens = false
        show()
        rule.onNodeWithText(NO_BROWSER).assertDoesNotExist()
        rule.onNodeWithText("Read the privacy policy").performScrollTo().performClick()
        rule.onNodeWithText(NO_BROWSER, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertNull("no system toast", ShadowToast.getTextOfLatestToast())
        policyOpens = true
        rule.onNodeWithText("Read the privacy policy").performScrollTo().performClick()
        rule.onNodeWithText(NO_BROWSER).assertDoesNotExist()
    }

    /** The button label uses the 24 sp button style, like the other full-width buttons. */
    @Test fun privacyButtonUsesTheButtonStyle() {
        show()
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("Read the privacy policy", useUnmergedTree = true).performScrollTo().fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(24f, results.first().layoutInput.style.fontSize.value)
    }

    /** TalkBack reads "Opens in your web browser." before the button, so the person knows before tapping. */
    @Test fun browserNoteComesBeforeTheButtonForTalkBack() {
        show()
        val button = rule.onNode(hasText("Read the privacy policy") and hasRole(Role.Button)).fetchSemanticsNode()
        val note = rule.onNodeWithText("Opens in your web browser.").fetchSemanticsNode()
        val buttonIndex = button.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f
        val noteIndex = note.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f
        assertTrue("the note ($noteIndex) comes before the button ($buttonIndex)", noteIndex < buttonIndex)
        val parent = button.parent
        assertTrue("the note and the button share a parent", parent != null && parent.id == note.parent?.id)
        assertEquals(true, parent!!.config.getOrNull(SemanticsProperties.IsTraversalGroup))
    }

    /** At the normal font size the button still meets the 64 dp floor. */
    @Test fun privacyButtonIs64DpAtNormalFont() {
        show()
        val button = rule.onNodeWithText("Read the privacy policy").performScrollTo().fetchSemanticsNode()
        assertTrue("64 dp tall", button.boundsInRoot.height >= with(rule.density) { 64.dp.toPx() } - 0.5f)
    }

    /** The button is at least 64 dp tall, and the section isn't cut off at the largest font. */
    @Test fun privacySectionFitsAtLargestFont() {
        show(fontScale = 2.0f)
        val button = rule.onNodeWithText("Read the privacy policy").performScrollTo().fetchSemanticsNode()
        assertTrue("64 dp tall", button.boundsInRoot.height >= with(rule.density) { 64.dp.toPx() } - 0.5f)
        rule.assertNoTextOverflow()
    }
}
