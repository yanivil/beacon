package dev.beacon.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

/*
 * Why these tests: on the test phone, set to Hebrew, every English line was
 * laid out right to left, so "1 minute" read "minute 1" and full stops moved
 * to the start of the line (the phone checks' defect P1). Each text's
 * direction must follow its own words, while it still sits at the layout's
 * start edge, as the mirrored layout expects. The source check keeps every
 * screen on the one text function that does this.
 *
 * Robolectric's text engine works out a paragraph's direction from its words
 * whatever direction it is asked for, so it can't show "minute 1" itself.
 * These tests therefore check the style BeaconText hands to the text, and
 * where the line sits; the reading order was checked on the emulator with
 * right-to-left forced (docs/checks/2026-11-rtl-text-direction.md).
 */

/** Tests for [BeaconText] and the style it applies. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BeaconTextTest {
    @get:Rule val rule = createComposeRule()

    /**
     * Draws one text 300 dp wide in the given layout direction and returns its layout.
     *
     * @param text the words to draw.
     * @param direction the layout direction around it.
     * @param align the alignment set on the style, if any.
     * @return the text's laid-out result, read the way TalkBack and the overflow check read it.
     */
    private fun layoutOf(text: String, direction: LayoutDirection, align: TextAlign = TextAlign.Unspecified): TextLayoutResult {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BeaconTheme {
                    Box(Modifier.width(300.dp)) {
                        BeaconText(text, Modifier.fillMaxWidth(), BeaconType.Body.copy(textAlign = align))
                    }
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    /** The text is laid out with its direction taken from its words, and aligned to the layout's start (right). */
    @Test fun textGetsContentDirectionAndTheLayoutsStartEdgeInRtl() {
        val style = layoutOf("1 minute", LayoutDirection.Rtl).layoutInput.style
        assertEquals(TextDirection.Content, style.textDirection)
        assertEquals(TextAlign.Right, style.textAlign)
    }

    /** In a right-to-left layout an English line still sits at the start (right) edge, as the mirrored rows expect. */
    @Test fun englishSitsAtTheRightEdgeInRtl() {
        val layout = layoutOf("1 minute", LayoutDirection.Rtl)
        assertEquals(layout.size.width.toFloat(), layout.getLineRight(0), 1f)
        assertTrue(layout.getLineLeft(0) > 0f)
    }

    /** In a left-to-right layout an English line sits at the left edge, as before. */
    @Test fun englishSitsAtTheLeftEdgeInLtr() {
        val layout = layoutOf("1 minute", LayoutDirection.Ltr)
        assertEquals(0f, layout.getLineLeft(0), 1f)
    }

    /** A centred text stays centred in a right-to-left layout. */
    @Test fun centredTextStaysCentredInRtl() {
        val layout = layoutOf("1 minute", LayoutDirection.Rtl, TextAlign.Center)
        assertEquals(layout.getLineLeft(0), layout.size.width - layout.getLineRight(0), 1f)
    }

    /** The style sets the direction from the words, and turns start and end into the layout's edges. */
    @Test fun styleMapsAlignmentToTheLayoutsEdges() {
        val rtl = BeaconType.Body.forLayout(LayoutDirection.Rtl)
        assertEquals(TextDirection.Content, rtl.textDirection)
        assertEquals(TextAlign.Right, rtl.textAlign)
        assertEquals(TextAlign.Left, BeaconType.Body.forLayout(LayoutDirection.Ltr).textAlign)
        assertEquals(TextAlign.Right, BeaconType.Body.copy(textAlign = TextAlign.Start).forLayout(LayoutDirection.Rtl).textAlign)
        assertEquals(TextAlign.Left, BeaconType.Body.copy(textAlign = TextAlign.End).forLayout(LayoutDirection.Rtl).textAlign)
        assertEquals(TextAlign.Right, BeaconType.Body.copy(textAlign = TextAlign.End).forLayout(LayoutDirection.Ltr).textAlign)
        assertEquals(TextAlign.Center, BeaconType.Body.copy(textAlign = TextAlign.Center).forLayout(LayoutDirection.Rtl).textAlign)
    }

    /** No screen draws text with BasicText directly, so every text gets the direction fix. */
    @Test fun onlyBeaconTextCallsBasicText() {
        val root = File("src/main/kotlin")
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        // Guards against a vacuous pass: run from another folder, the walk would find nothing.
        assertTrue("source folder not found from ${File("").absolutePath}", root.isDirectory)
        assertTrue("BeaconText.kt not among the scanned files", files.any { it.name == "BeaconText.kt" })
        val offenders = files
            .filter { it.name != "BeaconText.kt" }
            .filter { file ->
                file.readLines().any { line ->
                    val code = line.trimStart()
                    // Comments may name BasicText; only code that calls it counts.
                    "BasicText(" in line && !code.startsWith("*") && !code.startsWith("//") && !code.startsWith("/*")
                }
            }
            .map { it.path }
        assertEquals(emptyList<String>(), offenders)
    }
}
