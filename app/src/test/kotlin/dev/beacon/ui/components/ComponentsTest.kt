package dev.beacon.ui.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.FlashColour
import dev.beacon.ui.WithFontScale
import dev.beacon.ui.assertNoTextOverflow
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

    /** A disabled outline button is a Button that reports Disabled and ignores taps. */
    @Test fun disabledOutlineButtonIsDisabled() {
        var clicks = 0
        rule.setContent {
            BeaconTheme {
                OutlineButton(
                    text = "Shorter", onClick = { clicks++ }, content = BeaconColors.Text,
                    icon = BeaconIcons.Minus, minHeight = 76.dp, style = BeaconType.SmallButton, enabled = false,
                )
            }
        }
        rule.onNodeWithText("Shorter").assert(hasRole(Role.Button)).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    /** The five swatches are RadioButtons named by colour in one group, and White spans the row. */
    @Test fun swatchesAreRadioButtonsNamedByColour() {
        var picked: FlashColour? = null
        rule.setContent { BeaconTheme { SwatchGrid(FlashColour.YELLOW, { picked = it }, 96.dp) } }
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(5)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Yellow")).assertIsSelected()
        rule.onAllNodes(hasRole(Role.RadioButton) and isNotSelected()).assertCountEquals(4)
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertExists()
        // The default test window is wide enough for two columns, so White's span shows.
        val white = rule.onNode(hasRole(Role.RadioButton) and hasText("White")).getBoundsInRoot()
        val red = rule.onNode(hasRole(Role.RadioButton) and hasText("Red")).getBoundsInRoot()
        assertTrue(white.right - white.left > red.right - red.left)
        rule.onNode(hasRole(Role.RadioButton) and hasText("Red")).performClick()
        assertEquals(FlashColour.RED, picked)
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
