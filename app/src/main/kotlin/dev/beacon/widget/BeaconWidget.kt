package dev.beacon.widget

import android.content.Context
import androidx.annotation.VisibleForTesting
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.beacon.ui.formatResumeTime
import java.io.IOException
import kotlinx.coroutines.flow.first

/*
 * Why these four pieces: Android finds a widget through its receiver, the
 * receiver names the GlanceAppWidget that draws it, a tap runs an action
 * callback, and the coordinator refreshes the widget through one object.
 * The widget only reads the saved settings and draws them with the clock;
 * every change goes through the PauseCoordinator (ruling 5).
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
        val repo = (context.applicationContext as BeaconApp).settingsRepository
        // Read before drawing, so the first frame is the saved state, never the defaults. A read error
        // already falls back to the defaults inside the repository, so this doesn't throw.
        val saved = repo.settings.first()
        provideContent {
            val settings by repo.settings.collectAsState(initial = saved)
            // From the clock, not the stored value alone: a timed pause past its deadline shows ON.
            val state = widgetStateFor(settings.pause, System.currentTimeMillis())
            val resumeTime = (state as? WidgetState.Paused)?.resumeAtMillis?.let { formatResumeTime(context, it) }
            WidgetContent(
                state = state,
                resumeTime = resumeTime,
                spToDp = { sp -> scaledSpToDp(context.resources.displayMetrics, sp) },
                modifier = GlanceModifier.clickable(actionRunCallback<TogglePauseAction>()),
            )
        }
    }
}

/**
 * Converts a text size to dp at the phone's current font size.
 *
 * @param metrics the display metrics, read at each call so a font-size change is seen.
 * @param sp the text size in sp.
 * @return the size in dp. On Android 14 and later, applyDimension includes the non-linear font scaling.
 */
private fun scaledSpToDp(metrics: DisplayMetrics, sp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics) / metrics.density

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

/** Redraws every placed Beacon widget; the coordinator calls it after each pause change. */
object WidgetRefresher {
    /**
     * What a refresh does. Tests that only start an activity swap it for a no-op: Glance keeps
     * process-wide state, and a Robolectric test that ends mid-update can leave a later update waiting forever.
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
