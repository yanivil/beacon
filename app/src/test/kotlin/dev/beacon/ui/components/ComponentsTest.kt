package dev.beacon.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.FlashColour
import dev.beacon.ui.WithFontScale
import dev.beacon.ui.assertNoTextOverflow
import dev.beacon.ui.assertOwnListPositions
import dev.beacon.ui.assertTalkBackReadsList
import dev.beacon.ui.hasRole
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconTheme
import dev.beacon.ui.theme.BeaconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Why these tests: Home, Alert settings and the Test overlay are built from
 * these components, so the TalkBack roles and states, the ON/OFF words, the
 * minimum sizes, the largest-font layout and right-to-left mirroring are
 * pinned once here. Native graphics give real text measurement, which the
 * overflow and bounds checks need.
 */

/** Robolectric Compose tests for Beacon's shared components. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentsTest {
    /** Hosts each test's content in an empty activity. */
    @get:Rule val rule = createComposeRule()

    /** An app row is one Switch that TalkBack reads On/Off and that shows ON/OFF in words. */
    @Test fun appRowIsASwitchThatSaysOnOff() {
        var on by mutableStateOf(true)
        rule.setContent { BeaconTheme { AppSwitchRow("Texts (SMS)", "Yellow flash", Color.Yellow, on, { on = !on }) {} } }
        rule.onNode(isToggleable()).assert(hasRole(Role.Switch)).assertIsOn().assertTextContains("Texts (SMS)", substring = true)
        // The pill's word is on screen but not in the row's spoken label (its semantics are
        // cleared, so TalkBack reads the Switch state instead), so look in the unmerged tree.
        rule.onNodeWithText("ON", useUnmergedTree = true).assertExists()
        rule.onNode(isToggleable()).performClick().assertIsOff()
        rule.onNodeWithText("OFF", useUnmergedTree = true).assertExists()
        // TalkBack's label stays the name and the flash line only, without the pill's word.
        val label = listOf("Texts (SMS)", "Yellow flash").map { AnnotatedString(it) }
        rule.onNode(isToggleable()).assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, label))
    }

    /** An option is a RadioButton that reports Selected and reads its sub-lines. */
    @Test fun optionIsRadioButton() {
        var picked = false
        rule.setContent {
            BeaconTheme {
                Column(Modifier.selectableGroup()) {
                    OptionButton("Medium", selected = true, onSelect = {}, minHeight = 96.dp, sub = listOf("60 blinks a minute"))
                    OptionButton(
                        "Fast", selected = false, onSelect = { picked = true }, minHeight = 96.dp,
                        sub = listOf("90 blinks a minute", "Highest safe speed"),
                    )
                }
            }
        }
        rule.onNode(hasRole(Role.RadioButton) and hasText("Medium"))
            .assertIsSelected()
            .assertTextContains("60 blinks a minute")
        rule.onNode(hasRole(Role.RadioButton) and hasText("Fast"))
            .assertIsNotSelected()
            .assertTextContains("Highest safe speed")
            .performClick()
        assertTrue(picked)
    }

    /** The label style parameter is applied: a 24 sp style gives a smaller label than the 28 sp default. */
    @Test fun optionLabelStyleIsApplied() {
        rule.setContent {
            BeaconTheme {
                Column(Modifier.selectableGroup()) {
                    OptionButton("Default", selected = false, onSelect = {}, minHeight = 76.dp)
                    OptionButton("Small", selected = false, onSelect = {}, minHeight = 76.dp, labelStyle = BeaconType.Button)
                }
            }
        }
        val sizes = listOf("Default", "Small").map { text ->
            val results = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.first().layoutInput.style.fontSize.value
        }
        assertEquals(listOf(28f, 24f), sizes)
    }

    /** Screen titles and section headings are marked as headings for TalkBack. */
    @Test fun headingsAreHeadings() {
        rule.setContent {
            BeaconTheme {
                Column {
                    ScreenTitle("Title")
                    SectionHeading("X")
                }
            }
        }
        rule.onNodeWithText("Title").assert(isHeading())
        rule.onNodeWithText("X").assert(isHeading())
    }

    /** An outline button with a trailing icon still has its text as the label, and the icon comes after the text. */
    @Test fun outlineButtonTrailingIconKeepsTextLabel() {
        rule.setContent {
            BeaconTheme {
                OutlineButton(
                    text = "Change settings", onClick = {}, content = BeaconColors.Text, icon = null,
                    minHeight = 76.dp, style = BeaconType.Button, trailingIcon = BeaconIcons.Chevron,
                )
            }
        }
        rule.onNodeWithText("Change settings").assert(hasRole(Role.Button))
        val chevron = rule.onNodeWithTag(TRAILING_ICON_TAG, useUnmergedTree = true).getBoundsInRoot()
        val text = rule.onNodeWithText("Change settings", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("chevron ${chevron.left} vs text ${text.right}", chevron.left >= text.right)
    }

    /**
     * At 200% font on a 360 dp phone, a full-width outline button whose label
     * fills the row keeps its 28 dp trailing icon; the label wraps instead (D2).
     */
    @Config(qualifiers = "w360dp-h800dp")
    @Test fun trailingIconKeepsItsSizeAtLargestFont() {
        rule.setContent {
            BeaconTheme {
                WithFontScale(2.0f) {
                    OutlineButton(
                        text = "Change settings", onClick = {}, content = BeaconColors.Text, icon = null,
                        minHeight = 76.dp, style = BeaconType.Button, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = BeaconIcons.Chevron,
                    )
                }
            }
        }
        rule.onNodeWithTag(TRAILING_ICON_TAG, useUnmergedTree = true).assertWidthIsAtLeast(28.dp).assertHeightIsAtLeast(28.dp)
        // The icon still comes after the text, and the text is whole.
        val chevron = rule.onNodeWithTag(TRAILING_ICON_TAG, useUnmergedTree = true).getBoundsInRoot()
        val text = rule.onNodeWithText("Change settings", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("chevron ${chevron.left} vs text ${text.right}", chevron.left >= text.right)
        rule.assertNoTextOverflow()
    }

    /** At 200% font on a 360 dp phone, a long label never squeezes a button's 30 dp leading icon either. */
    @Config(qualifiers = "w360dp-h800dp")
    @Test fun leadingIconKeepsItsSizeAtLargestFont() {
        rule.setContent {
            BeaconTheme {
                WithFontScale(2.0f) {
                    OutlineButton(
                        text = "Change settings", onClick = {}, content = BeaconColors.Text, icon = BeaconIcons.Minus,
                        minHeight = 76.dp, style = BeaconType.Button, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        rule.onNodeWithTag(LEADING_ICON_TAG, useUnmergedTree = true).assertWidthIsAtLeast(30.dp).assertHeightIsAtLeast(30.dp)
        rule.assertNoTextOverflow()
    }

    /** A disabled outline button is a Button that reports Disabled, ignores taps and keeps the 64 dp floor. */
    @Test fun disabledOutlineButtonIsDisabled() {
        var clicks = 0
        rule.setContent {
            BeaconTheme {
                // minHeight 0 dp, so the 64 dp floor itself is what keeps the button tall enough.
                OutlineButton(
                    text = "Shorter", onClick = { clicks++ }, content = BeaconColors.Text,
                    icon = BeaconIcons.Minus, minHeight = 0.dp, style = BeaconType.SmallButton, enabled = false,
                )
            }
        }
        rule.onNodeWithText("Shorter")
            .assert(hasRole(Role.Button))
            .assertIsNotEnabled()
            .assertHeightIsAtLeast(64.dp)
            .assertWidthIsAtLeast(64.dp)
            .performClick()
        assertEquals(0, clicks)
    }

    /** A filled button's secondary text, as in "Test alert (3 sec)", is part of its label and sits after the main text. */
    @Test fun filledButtonReadsSecondaryText() {
        rule.setContent {
            BeaconTheme {
                FilledButton(
                    text = "Test alert", onClick = {}, background = BeaconColors.White, content = BeaconColors.OnAccent,
                    icon = BeaconIcons.Bolt, minHeight = 76.dp, style = BeaconType.TestButton, secondaryText = "(3 sec)",
                )
            }
        }
        rule.onNode(hasRole(Role.Button)).assertTextContains("Test alert").assertTextContains("(3 sec)")
        // The default test window is wide enough for both texts on one line.
        val main = rule.onNodeWithText("Test alert", useUnmergedTree = true).getBoundsInRoot()
        val secondary = rule.onNodeWithText("(3 sec)", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("same line, after the main text", secondary.left >= main.right && secondary.top < main.bottom)
    }

    /** The five swatches are RadioButtons named by colour in one group, and White spans the row. */
    @Test fun swatchesAreRadioButtonsNamedByColour() {
        var picked: FlashColour? = null
        rule.setContent { BeaconTheme { SwatchGrid(FlashColour.YELLOW, { picked = it }, 96.dp) } }
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(5)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Yellow")).assertIsSelected()
        rule.onAllNodes(hasRole(Role.RadioButton) and isNotSelected()).assertCountEquals(4)
        // One group around the five options (its list positions are pinned in swatchesReadAsAListOfFive).
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)).assertExists()
        // The default test window is wide enough for two columns, so White's span shows.
        val white = rule.onNode(hasRole(Role.RadioButton) and hasText("White")).getBoundsInRoot()
        val red = rule.onNode(hasRole(Role.RadioButton) and hasText("Red")).getBoundsInRoot()
        assertTrue(white.right - white.left > red.right - red.left)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Red")).performClick()
        assertEquals(FlashColour.RED, picked)
    }

    /**
     * TalkBack reads the five swatches as a list of five, "1 of 5" to "5 of 5"
     * in colour order, in the two-column grid of a wide phone and in the
     * one-column grid at 200% font (D5).
     */
    @Config(qualifiers = "w411dp-h914dp")
    @Test fun swatchesReadAsAListOfFive() {
        var scale by mutableStateOf(1f)
        rule.setContent { BeaconTheme { WithFontScale(scale) { SwatchGrid(FlashColour.YELLOW, {}, 96.dp) } } }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val swatches = FlashColour.entries.map { hasRole(Role.RadioButton) and hasText(context.getString(it.nameRes)) }
        // Two columns: Red and Yellow share the first row.
        val red = rule.onNode(swatches[0]).getBoundsInRoot()
        val yellow = rule.onNode(swatches[1]).getBoundsInRoot()
        assertEquals("two columns", red.top, yellow.top)
        rule.assertOwnListPositions(swatches)
        rule.assertTalkBackReadsList(swatches)
        scale = 2.0f
        rule.waitForIdle()
        rule.assertOwnListPositions(swatches)
        rule.assertTalkBackReadsList(swatches)
    }

    /** At the largest font the row and its pill wrap and grow instead of clipping. */
    @Test fun pillAndRowDontClipAtLargestFont() {
        rule.setContent {
            BeaconTheme { WithFontScale(2.0f) { AppSwitchRow("WhatsApp messages", "Green flash", Color.Green, false, {}) {} } }
        }
        // The pill's text must be among the texts checked, even though its semantics are cleared.
        rule.onNodeWithText("OFF", useUnmergedTree = true).assertExists()
        rule.assertNoTextOverflow()
    }

    /** On a 360 dp phone the pill sits beside the name at 100% and wraps under it at 200%. */
    @Config(qualifiers = "w360dp-h800dp")
    @Test fun pillWrapsUnderTheTextOnlyWhenWordsWouldBreak() {
        var scale by mutableStateOf(1f)
        rule.setContent {
            BeaconTheme { WithFontScale(scale) { AppSwitchRow("WhatsApp messages", "Green flash", Color.Green, false, {}) {} } }
        }
        var name = rule.onNodeWithTag(APP_ROW_NAME_TAG, useUnmergedTree = true).getBoundsInRoot()
        var pill = rule.onNodeWithText("OFF", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("beside at 100%", pill.left >= name.right && pill.top < name.bottom)
        scale = 2.0f
        name = rule.onNodeWithTag(APP_ROW_NAME_TAG, useUnmergedTree = true).getBoundsInRoot()
        pill = rule.onNodeWithText("OFF", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("under at 200%", pill.top >= name.bottom)
    }

    /** In right-to-left the icon tile sits at the start (right) of the name. */
    @Test fun rowMirrorsInRtl() {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BeaconTheme { AppSwitchRow("Texts (SMS)", "Yellow flash", Color.Yellow, true, {}) {} }
            }
        }
        val icon = rule.onNodeWithTag(APP_ROW_ICON_TAG, useUnmergedTree = true).getBoundsInRoot()
        val name = rule.onNodeWithTag(APP_ROW_NAME_TAG, useUnmergedTree = true).getBoundsInRoot()
        assertTrue("icon ${icon.left} vs name ${name.left}", icon.left > name.left)
        assertTrue("icon starts after the name ends", icon.left >= name.right)
    }

    /** Grid items in one row share its height, and at 200% font each item gets its own row. */
    @Test fun gridStretchesRowsAndFallsBackToOneColumn() {
        var scale by mutableStateOf(1f)
        rule.setContent {
            BeaconTheme {
                WithFontScale(scale) {
                    AdaptiveGrid(2) { i ->
                        OptionButton(
                            listOf("1 min", "Custom")[i], selected = i == 0, onSelect = {}, minHeight = 64.dp,
                            stacked = true, sub = if (i == 1) listOf("10 min") else emptyList(),
                        )
                    }
                }
            }
        }
        var first = rule.onNode(hasText("1 min")).getBoundsInRoot()
        var second = rule.onNode(hasText("Custom")).getBoundsInRoot()
        assertEquals(first.top, second.top)
        assertEquals(first.bottom, second.bottom)
        val twoColumnWidth = first.right - first.left
        scale = 2.0f
        first = rule.onNode(hasText("1 min")).getBoundsInRoot()
        second = rule.onNode(hasText("Custom")).getBoundsInRoot()
        assertTrue("stacked", second.top >= first.bottom)
        assertTrue("full width", first.right - first.left > twoColumnWidth)
        assertEquals(first.right - first.left, second.right - second.left)
    }

    /** Buttons get at least 64 x 64 dp, rows 100 dp and stacked options their minimum height. */
    @Test fun controlsMeetMinimumSizes() {
        rule.setContent {
            BeaconTheme {
                Column {
                    OutlineButton(
                        text = "X", onClick = {}, content = BeaconColors.Text, icon = null,
                        minHeight = 0.dp, style = BeaconType.Button, modifier = Modifier.testTag("button"),
                    )
                    AppSwitchRow("Telegram", "No flash", null, false, {}, Modifier.testTag("row")) {}
                    OptionButton(
                        "Custom", selected = true, onSelect = {}, minHeight = 120.dp, stacked = true,
                        modifier = Modifier.testTag("option"), sub = listOf("10 min"),
                    )
                }
            }
        }
        rule.onNodeWithTag("button").assertHeightIsAtLeast(64.dp).assertWidthIsAtLeast(64.dp)
        rule.onNodeWithTag("row").assertHeightIsAtLeast(100.dp)
        rule.onNodeWithTag("option").assertHeightIsAtLeast(120.dp)
    }
}
