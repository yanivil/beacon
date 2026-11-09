package dev.beacon.widget

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.EmittableImage
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.MappedNode
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.glance.text.EmittableText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.R
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Why Robolectric: the widget reads its words from strings.xml, so the test
 * needs a real context. Glance's unit-test API renders the composable to a
 * node tree without a launcher. The fit uses the test estimate, so the lines
 * kept here follow WidgetFitTest's numbers.
 */

/** Tests for [WidgetContent], mostly at the 4 x 2 default size. */
@RunWith(AndroidJUnit4::class)
class WidgetContentTest {
    /** The test application, for the strings. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Matches any text node that starts with [prefix]. */
    private fun startsWithText(prefix: String) = GlanceNodeMatcher<MappedNode>("text starting with '$prefix'") { node ->
        (node.value.emittable as? EmittableText)?.text?.startsWith(prefix) == true
    }

    /**
     * Matches an image drawn from the drawable [resId].
     *
     * @param resId the drawable.
     * @return the matcher.
     */
    private fun hasImage(@DrawableRes resId: Int) = GlanceNodeMatcher<MappedNode>("image $resId") { node ->
        ((node.value.emittable as? EmittableImage)?.provider as? AndroidResourceImageProvider)?.resId == resId
    }

    /** ON shows the title, the state and the pause hint, and speaks the ON label. */
    @Test fun onShowsItsThreeLines() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.On, null, EstimateMeasure { it }) }
        onNode(hasText("Visual alerts")).assertExists()
        onNode(hasText("ON")).assertExists()
        onNode(hasText("Tap to pause for 1 hour")).assertExists()
        onNode(hasContentDescription("Visual alerts are ON. Tap to pause for 1 hour.")).assertExists()
        // The bolt goes with ON; the pause bars are PAUSED's.
        onNode(hasImage(R.drawable.widget_bolt)).assertExists()
        onNode(hasImage(R.drawable.widget_pause)).assertDoesNotExist()
    }

    /** A timed pause adds the resume line and names the time in the label. */
    @Test fun timedPauseShowsTheResumeLine() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.Paused(1L), "10:30", EstimateMeasure { it }) }
        onNode(hasText("PAUSED")).assertExists()
        onNode(hasText("Back ON by itself at 10:30")).assertExists()
        onNode(
            hasContentDescription("Visual alerts are PAUSED until 10:30. Tap to turn them back on now."),
        ).assertExists()
        onNode(hasImage(R.drawable.widget_pause)).assertExists()
        onNode(hasImage(R.drawable.widget_bolt)).assertDoesNotExist()
    }

    /** When the fit drops the icon, no icon is drawn, and the word and the resume line stay. */
    @Test fun iconGoesWhenTheFitDropsIt() = runGlanceAppWidgetUnitTest {
        setContext(app)
        // WidgetFitTest's case: at 232 x 160 dp the icon goes before "PAUSED" would wrap.
        setAppWidgetSize(DpSize(232.dp, 160.dp))
        provideComposable { WidgetContent(WidgetState.Paused(1L), "10:30", EstimateMeasure { it }) }
        onNode(hasText("PAUSED")).assertExists()
        onNode(hasText("Back ON by itself at 10:30")).assertExists()
        onNode(hasImage(R.drawable.widget_pause)).assertDoesNotExist()
        onNode(hasImage(R.drawable.widget_bolt)).assertDoesNotExist()
    }

    /** An open-ended pause has no resume line. */
    @Test fun openPauseHasNoResumeLine() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.Paused(null), null, EstimateMeasure { it }) }
        onNode(hasText("PAUSED")).assertExists()
        onNode(startsWithText("Back ON")).assertDoesNotExist()
        onNode(hasContentDescription("Visual alerts are PAUSED. Tap to turn them back on now.")).assertExists()
    }

    /** At the largest font the state word stays and the optional lines go. */
    @Test fun largestFontKeepsTheStateWord() = runGlanceAppWidgetUnitTest {
        setContext(app)
        setAppWidgetSize(DpSize(350.dp, 200.dp))
        provideComposable { WidgetContent(WidgetState.On, null, EstimateMeasure { it * 2 }) }
        onNode(hasText("ON")).assertExists()
        onNode(hasText("Tap to pause for 1 hour")).assertDoesNotExist()
        onNode(hasText("Visual alerts")).assertDoesNotExist()
    }
}
