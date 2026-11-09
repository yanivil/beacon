package dev.beacon.widget

import android.app.Application
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.MappedNode
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.glance.text.EmittableText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Why Robolectric: the widget reads its words from strings.xml, so the test
 * needs a real context. Glance's unit-test API renders the composable to a
 * node tree without a launcher.
 */

/** Tests for [WidgetContent] at the 4 x 2 default size. */
@RunWith(AndroidJUnit4::class)
class WidgetContentTest {
    /** The test application, for the strings. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Matches any text node that starts with [prefix]. */
    private fun startsWithText(prefix: String) = GlanceNodeMatcher<MappedNode>("text starting with '$prefix'") { node ->
        (node.value.emittable as? EmittableText)?.text?.startsWith(prefix) == true
    }

    /** ON shows the title, the state and the pause hint, and speaks the ON label. */
    @Test fun onShowsItsThreeLines() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.On, null, { it }) }
        onNode(hasText("Visual alerts")).assertExists()
        onNode(hasText("ON")).assertExists()
        onNode(hasText("Tap to pause for 1 hour")).assertExists()
        onNode(hasContentDescription("Visual alerts are ON. Tap to pause for 1 hour.")).assertExists()
    }

    /** A timed pause adds the resume line and names the time in the label. */
    @Test fun timedPauseShowsTheResumeLine() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.Paused(1L), "10:30", { it }) }
        onNode(hasText("PAUSED")).assertExists()
        onNode(hasText("Back ON by itself at 10:30")).assertExists()
        onNode(
            hasContentDescription("Visual alerts are PAUSED until 10:30. Tap to turn them back on now."),
        ).assertExists()
    }

    /** An open-ended pause has no resume line. */
    @Test fun openPauseHasNoResumeLine() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.Paused(null), null, { it }) }
        onNode(hasText("PAUSED")).assertExists()
        onNode(startsWithText("Back ON")).assertDoesNotExist()
        onNode(hasContentDescription("Visual alerts are PAUSED. Tap to turn them back on now.")).assertExists()
    }

    /** At the largest font the state word stays and the optional lines go. */
    @Test fun largestFontKeepsTheStateWord() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.On, null, { it * 2 }) }
        onNode(hasText("ON")).assertExists()
        onNode(hasText("Tap to pause for 1 hour")).assertDoesNotExist()
        onNode(hasText("Visual alerts")).assertDoesNotExist()
    }
}
