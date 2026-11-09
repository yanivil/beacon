package dev.beacon.widget

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import dev.beacon.BeaconApp
import dev.beacon.settings.BeaconSettings
import dev.beacon.ui.formatResumeTime
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/*
 * Why these four pieces: Android finds a widget through its receiver, the
 * receiver names the GlanceAppWidget that draws it, a tap runs an action
 * callback, and the coordinator refreshes the widget through one object.
 * The widget only reads the saved settings and draws them with the clock;
 * every change goes through the PauseCoordinator (ruling 5). Its composition
 * also reads the app's configuration key, so a font-size change re-fits a
 * session that is still running (ruling F6), and a 12/24-hour or time-zone
 * change formats its resume time again (follow-up R1).
 */

/** The home-screen widget: shows ON or PAUSED, and one tap pauses for 1 hour or turns alerts ON. */
class BeaconWidget : GlanceAppWidget() {
    /** Exact: the content is drawn for the size the launcher gives, so the fit sees the real space. */
    override val sizeMode: SizeMode = SizeMode.Exact

    /**
     * Draws the widget from the saved settings and follows their changes.
     *
     * @param context the app's context.
     * @param id which placed widget is drawn.
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as BeaconApp
        val repo = app.settingsRepository
        // Read before drawing, so the first frame is the saved state, never the defaults. A read error
        // already falls back to the defaults inside the repository, so this doesn't throw.
        val saved = repo.settings.first()
        provideContent { WidgetBody(context, repo.settings, saved, app.widgetConfig) }
    }
}

/**
 * The widget's composition: follows the saved settings and the configuration key, and draws the widget.
 *
 * @param context the app's context, for the strings, the time format and the display metrics.
 * @param settings the saved settings as they change.
 * @param saved the settings read before the first frame.
 * @param config the font size, density and languages the widget is fitted for, and the 12/24-hour
 *   setting and time zone its resume time is shown in.
 */
@Composable
internal fun WidgetBody(
    context: Context,
    settings: Flow<BeaconSettings>,
    saved: BeaconSettings,
    config: StateFlow<WidgetConfigKey>,
) {
    val current by settings.collectAsState(initial = saved)
    val key by config.collectAsState()
    // From the clock, not the stored value alone: a timed pause past its deadline shows ON.
    val state = widgetStateFor(current.pause, System.currentTimeMillis())
    val resumeTime = (state as? WidgetState.Paused)?.resumeAtMillis?.let { formatResumeTime(context, it) }
    // A new measure for each key (ruling F6): reading the key here makes a running session compose
    // again after a font-size, density or language change, and the new measure makes the fit run again.
    // The display metrics are already updated when the key changes. The same recomposition formats the
    // resume time above again, so a 12/24-hour or time-zone change in the key shows too (follow-up R1).
    val measure = remember(key) { StaticLayoutMeasure(context.resources.displayMetrics) }
    WidgetContent(
        state = state,
        resumeTime = resumeTime,
        measure = measure,
        modifier = GlanceModifier.clickable(actionRunCallback<TogglePauseAction>()),
    )
}

/** Android's entry point for the widget: the launcher's update requests arrive here. */
class BeaconWidgetReceiver : GlanceAppWidgetReceiver() {
    /** The widget this receiver draws. */
    override val glanceAppWidget: GlanceAppWidget = BeaconWidget()
}

/** The widget's one tap: pauses for 1 hour while ON, turns alerts ON while paused. */
class TogglePauseAction : ActionCallback {
    /**
     * Applies the tap through the coordinator, which saves it, sets the alarm and refreshes the widget.
     *
     * @param context the app's context.
     * @param glanceId the widget that was tapped.
     * @param parameters unused; the tap carries nothing.
     */
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            (context.applicationContext as BeaconApp).pauseCoordinator.tapFromWidget()
        } catch (_: IOException) {
            // Ruling 9: the settings can't be read or saved, so the tap does nothing. Nothing was
            // drawn ahead of the save, so the widget still shows what is saved.
        }
    }
}

/**
 * Redraws every placed Beacon widget. The coordinator calls it after each pause change and after a time,
 * time-zone or language change that what the widget shows depends on; BeaconApp after a font-size, density
 * or language change.
 */
object WidgetRefresher {
    /**
     * What a refresh does. Tests that start Beacon's main screen or connect the listener, which both
     * catch the pause up, swap it for a no-op (NoWidgetRefreshRule), and tests of the refresh itself
     * swap it for a counter: Glance keeps process-wide state, and a Robolectric test that ends
     * mid-update can leave a later update waiting forever.
     */
    @VisibleForTesting
    internal var update: suspend (Context) -> Unit = { BeaconWidget().updateAll(it) }

    /**
     * Asks Glance to update every placed Beacon widget.
     *
     * @param context any context of this app.
     */
    suspend fun refresh(context: Context) {
        update(context)
    }
}
