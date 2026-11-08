package dev.beacon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment

/*
 * Why shared helpers: the largest-font rule ("nothing clipped or truncated")
 * and the TalkBack roles are checked on every screen, so the screen tests of
 * Tasks 8 to 11 all use the same overflow check, font-scale switches and role
 * matcher instead of each writing their own.
 */

/**
 * Asserts that no text on screen overflows its box.
 *
 * It walks the unmerged tree, so it reaches every text, including texts
 * inside merged controls and inside decorative parts whose semantics are
 * cleared (the unmerged tree keeps those).
 *
 * @throws AssertionError if no text is found at all (the check would prove
 *   nothing), or if any text is cut off, naming that text.
 */
fun ComposeContentTestRule.assertNoTextOverflow() {
    val nodes = onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
        useUnmergedTree = true,
    ).fetchSemanticsNodes()
    assertTrue("No text found to check for overflow", nodes.isNotEmpty())
    nodes.forEach { node ->
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        results.forEach { layout ->
            assertFalse("Text overflows: \"${layout.layoutInput.text}\"", layout.isCutOff())
        }
    }
}

/**
 * Whether this text is clipped or truncated in its box.
 *
 * Why not just hasVisualOverflow: for a plain-String BasicText, Compose 1.11
 * rebuilds the layout result for semantics at the slot's full maximum width,
 * so its width half (box width < paragraph width) is true for every text
 * narrower than its slot, such as "OFF" (66 px wide in a 154 px slot). The
 * height half is reliable and also covers maxLines truncation; the width is
 * checked line by line instead, against the real box.
 *
 * Each line's width is the smaller of two measures, because each one is
 * too big in a different case. For centred text, Android rounds the line's
 * left down and its right up, so right minus left can be up to 2 px more
 * than the real width ("Test alert": 170.0 against 168.3 in a 169 px box).
 * The line width is exact but counts trailing spaces, which Android lets
 * hang past the box. The smaller one is never less than the width of the
 * visible glyphs, so a real overflow is still caught.
 *
 * @return true if the text is too tall for its box, a line is wider than the
 *   box, or a line is ellipsized.
 */
private fun TextLayoutResult.isCutOff(): Boolean {
    // Half a pixel of slack for float rounding: the box width is the rounded-up paragraph width.
    val lineTooWide = (0 until lineCount).any { line ->
        minOf(getLineRight(line) - getLineLeft(line), multiParagraph.getLineWidth(line)) > size.width + 0.5f
    }
    val ellipsized = (0 until lineCount).any { isLineEllipsized(it) }
    return didOverflowHeight || lineTooWide || ellipsized
}

/**
 * Shows [content] with the text scaled by [scale], keeping the screen density.
 *
 * The scaling is linear, which is stricter than Android's non-linear font
 * scaling at 200%, so a layout that passes here also passes on the phone.
 * Use it for tests of single components and screens.
 *
 * @param scale the font scale, such as 2.0f for the largest system size.
 * @param content the UI to show at that scale.
 */
@Composable
fun WithFontScale(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    CompositionLocalProvider(LocalDensity provides Density(density, scale), content = content)
}

/**
 * Sets Robolectric's system font scale for one test, then resets it to 1.
 *
 * Apply it as the outer rule of a `RuleChain` around
 * `createAndroidComposeRule`, so the activity launches already scaled. Use
 * it for whole-activity tests.
 *
 * @property scale the font scale to set before the test.
 */
class FontScaleRule(private val scale: Float) : TestRule {
    /**
     * Wraps the test so the scale is set before it and reset after it.
     *
     * @param base the test, or the inner rules and the test.
     * @param description the test's description, unused.
     * @return a statement that runs [base] at [scale].
     */
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        /**
         * Runs the test at the chosen scale.
         *
         * @throws Throwable whatever the test throws, after the scale is reset.
         */
        override fun evaluate() {
            RuntimeEnvironment.setFontScale(scale)
            try {
                base.evaluate()
            } finally {
                // Reset even when the test fails, so later tests start at 100%.
                RuntimeEnvironment.setFontScale(1f)
            }
        }
    }
}

/**
 * Matches nodes whose TalkBack role is [role].
 *
 * @param role the expected role, such as Role.Switch.
 * @return a matcher for nodes with exactly that role.
 */
fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
