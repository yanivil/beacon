package dev.beacon.widget

import android.app.Application
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.RemoteViewsRoot
import androidx.glance.text.EmittableText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.settings.BeaconSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * Why this test: a Glance session keeps one composition running, and after
 * only a font-size change nothing it read had changed, so it drew nothing
 * new and the old lines clipped (PR 3's D1). The composition now reads the
 * configuration key; this test changes the key under a running composition
 * and checks that the widget is fitted again.
 *
 * Why its own small session: Glance's unit-test harness copies its node tree
 * only when it sees the recomposer turn idle, and it misses a recomposition
 * that starts and ends within one pass. So the test runs the composition
 * itself, as a Glance session does: Glance's applier over a RemoteViewsRoot,
 * a recomposer, and the snapshot notifications Glance's session monitor
 * sends. Native graphics, so the real measure is used.
 */

/** A frame clock that runs each frame at once, as the test needs no timing. */
private object ImmediateFrameClock : MonotonicFrameClock {
    /**
     * Runs the frame now.
     *
     * @param onFrame the frame's work.
     * @return what the frame returns.
     */
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R = onFrame(System.nanoTime())
}

/**
 * Collects the text of every text node under [node], in order.
 *
 * @param node where to start.
 * @return the texts.
 */
private fun texts(node: Emittable): List<String> = when (node) {
    is EmittableText -> listOf(node.text)
    is EmittableWithChildren -> node.children.flatMap(::texts)
    else -> emptyList()
}

/** Tests for [WidgetBody], the widget's composition. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-420dpi")
class BeaconWidgetTest {
    /** The test application, for the strings and the display metrics. */
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** A font change under a running composition re-fits it: at 4 x 2 and font 2.0, ON drops the tap hint. */
    @Test fun aFontChangeRefitsTheRunningWidget() = runTest {
        val root = RemoteViewsRoot(maxDepth = 10)
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(Applier(root), recomposer)
        val session = launch(ImmediateFrameClock) { recomposer.runRecomposeAndApplyChanges() }
        val config = MutableStateFlow(WidgetConfigKey.of(app.resources.configuration))
        try {
            composition.setContent {
                CompositionLocalProvider(LocalContext provides app, LocalSize provides DpSize(360.dp, 224.dp)) {
                    WidgetBody(app, flowOf(BeaconSettings.DEFAULT), BeaconSettings.DEFAULT, config)
                }
            }
            advanceUntilIdle()
            assertEquals(listOf("Visual alerts", "ON", "Tap to pause for 1 hour"), texts(root))

            RuntimeEnvironment.setFontScale(2.0f)
            // What BeaconApp does in onConfigurationChanged.
            config.value = WidgetConfigKey.of(app.resources.configuration)
            advanceUntilIdle()
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()
            assertEquals(listOf("Visual alerts", "ON"), texts(root))
        } finally {
            RuntimeEnvironment.setFontScale(1.0f)
            composition.dispose()
            recomposer.cancel()
            session.cancel()
        }
    }
}
