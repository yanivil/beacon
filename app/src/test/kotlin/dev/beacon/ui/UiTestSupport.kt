package dev.beacon.ui

import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment

/*
 * Why shared helpers: the largest-font rule ("nothing clipped or truncated")
 * and the TalkBack roles are checked on every screen, so the screen tests of
 * Tasks 8 to 11 all use the same overflow check, font-scale switches and role
 * matcher instead of each writing their own. The list-position check reads
 * the node information TalkBack itself receives, for the same reason.
 */

/**
 * Asserts that no text on screen overflows its box.
 *
 * It walks the unmerged tree, so it reaches every text, including texts
 * inside merged controls and inside decorative parts whose semantics are
 * cleared (the unmerged tree keeps those).
 *
 * It checks each text against its own box only. Clipping by an ancestor is
 * not detected: a clipToBounds, or a fixed-size parent that cuts off a child
 * measured larger than itself, hides text while the text's own box still
 * fits it. UiTestSupportTest shows which cases it does catch.
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
 * The node information TalkBack gets for [node], built by Compose's own
 * accessibility delegate through the view's public node provider.
 *
 * Why not just the semantics: Compose can rewrite some semantics on the way
 * to TalkBack (it recomputes a selectable group's list positions, for
 * example), so checks of what TalkBack reads go through the real node info.
 *
 * @param node a semantics node from the merged tree, not the root.
 * @return the node information TalkBack would receive.
 * @throws AssertionError if the node has no host view, no node provider, or no info.
 */
fun accessibilityNodeInfo(node: SemanticsNode): AccessibilityNodeInfo {
    val view = (node.root as? ViewRootForTest)?.view
    assertNotNull("node ${node.id} has no host view", view)
    val provider = view!!.accessibilityNodeProvider
    assertNotNull("the host view has no accessibility node provider", provider)
    // Compose uses the semantics id as the virtual view id for every node but the root.
    val info = provider!!.createAccessibilityNodeInfo(node.id)
    assertNotNull("no node info for node ${node.id}", info)
    return info!!
}

/**
 * Asserts that each of [options] carries its own list position in its
 * semantics: option i is row i of column 0, one row and one column wide.
 *
 * @param options one matcher per option, in reading order.
 * @throws AssertionError if an option has no position or another one.
 */
fun ComposeContentTestRule.assertOwnListPositions(options: List<SemanticsMatcher>) {
    options.forEachIndexed { index, option ->
        val item = onNode(option).fetchSemanticsNode().config.getOrNull(SemanticsProperties.CollectionItemInfo)
        assertNotNull("option $index has no CollectionItemInfo", item)
        assertEquals("option $index's own row", index, item!!.rowIndex)
        assertEquals("option $index's own column", 0, item.columnIndex)
        assertEquals("option $index's row span", 1, item.rowSpan)
        assertEquals("option $index's column span", 1, item.columnSpan)
    }
}

/**
 * Asserts that TalkBack reads [options] as a one-column list in this order:
 * the group they share reports one row per option and one column, and
 * option i reports row i of column 0, so TalkBack says "i + 1 of n".
 *
 * @param options one matcher per option, in reading order.
 * @throws AssertionError if any option or the group reports another position or size.
 */
fun ComposeContentTestRule.assertTalkBackReadsList(options: List<SemanticsMatcher>) {
    val nodes = options.map { onNode(it).fetchSemanticsNode() }
    nodes.forEachIndexed { index, node ->
        val item = accessibilityNodeInfo(node).collectionItemInfo
        assertNotNull("option $index: TalkBack gets no list position", item)
        assertEquals("option $index: TalkBack's row", index, item!!.rowIndex)
        assertEquals("option $index: TalkBack's column", 0, item.columnIndex)
    }
    val group = nodes.first().parent
    assertNotNull("the options have no group", group)
    nodes.forEach { assertEquals("every option is in the same group", group!!.id, it.parent?.id) }
    val list = accessibilityNodeInfo(group!!).collectionInfo
    assertNotNull("TalkBack gets no list size for the group", list)
    assertEquals("rows in the list", nodes.size, list!!.rowCount)
    assertEquals("columns in the list", 1, list.columnCount)
}

/**
 * Matches nodes whose TalkBack role is [role].
 *
 * @param role the expected role, such as Role.Switch.
 * @return a matcher for nodes with exactly that role.
 */
fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)
