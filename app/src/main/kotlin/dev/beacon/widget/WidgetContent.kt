package dev.beacon.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.beacon.R
import dev.beacon.ui.theme.BeaconColors

/*
 * Why one composable that fits itself: the widget has to stay readable at the
 * largest font size, so it builds the lines from strings.xml, asks fitLines
 * which ones fit in the size the launcher gave, and draws only those. The
 * whole widget is one tap target with one spoken label; the icon is
 * decorative. Only two text pairs exist: #0B0B0D on #FFD60A (ON) and
 * #FFFFFF on #0B0B0D (PAUSED), both well above 7:1 (spec sections 10, 11).
 */

/** Gap between lines, in dp (the same 4 dp fitLines allows for). */
private const val LINE_GAP_DP = 4

/** Space between the state icon and its word, in dp (the same 8 dp fitLines allows for). */
private const val ICON_GAP_DP = 8

/**
 * Draws the widget.
 *
 * @param state ON or PAUSED.
 * @param resumeTime the formatted resume time of a timed pause; null otherwise.
 * @param spToDp converts sp to dp at the user's font size, for the fit.
 * @param modifier the modifier for the root; Task 3 passes the tap action here.
 */
@Composable
fun WidgetContent(
    state: WidgetState,
    resumeTime: String?,
    spToDp: (Float) -> Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val on = state is WidgetState.On
    val textColour = if (on) BeaconColors.OnAccent else BeaconColors.Text
    val colour = ColorProvider(textColour)

    val stateWord = context.getString(if (on) R.string.alerts_on else R.string.alerts_paused)
    val stateSp = if (on) 64f else 52f
    // Only a timed pause has a resume line; an open-ended pause has no time to name.
    val resumeText = if (state is WidgetState.Paused && resumeTime != null) {
        context.getString(R.string.widget_back_on_at, resumeTime)
    } else {
        null
    }
    val specs = listOfNotNull(
        LineSpec(WidgetLine.TITLE, context.getString(R.string.alerts_heading), 28f, false),
        LineSpec(WidgetLine.STATE, stateWord, stateSp, true),
        resumeText?.let { LineSpec(WidgetLine.RESUME_TIME, it, 22f, false) },
        LineSpec(
            WidgetLine.TAP_HINT,
            context.getString(if (on) R.string.widget_tap_pause else R.string.widget_tap_resume),
            22f,
            false,
        ),
    )
    val size = LocalSize.current
    val fit = fitLines(specs, size.width.value, size.height.value, spToDp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImageProvider(if (on) R.drawable.widget_on_bg else R.drawable.widget_paused_bg))
            .padding(16.dp)
            .semantics { contentDescription = spokenLabel(context, state, resumeTime) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Draw in display order; the fit only decides which lines are present.
            specs.filter { it.line in fit.lines }.forEachIndexed { index, spec ->
                if (index > 0) Spacer(GlanceModifier.height(LINE_GAP_DP.dp))
                if (spec.line == WidgetLine.STATE) {
                    StateRow(spec, fit.stateIcon, on, colour, spToDp(spec.textSp))
                } else {
                    Text(spec.text, style = lineStyle(spec.textSp, spec.line == WidgetLine.TITLE, colour))
                }
            }
        }
    }
}

/**
 * Draws the state line: the decorative icon and the bold state word.
 *
 * @param spec the state line.
 * @param showIcon true to draw the icon before the word.
 * @param on true for ON (bolt), false for PAUSED (pause bars).
 * @param colour the text colour, which also tints the icon.
 * @param iconDp the icon's width and height in dp: as tall as the text, as fitLines assumes.
 */
@Composable
private fun StateRow(spec: LineSpec, showIcon: Boolean, on: Boolean, colour: ColorProvider, iconDp: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showIcon) {
            // No content description: the word beside it, and the root's label, say it all.
            Image(
                provider = ImageProvider(if (on) R.drawable.widget_bolt else R.drawable.widget_pause),
                contentDescription = null,
                modifier = GlanceModifier.size(iconDp.dp),
                colorFilter = ColorFilter.tint(colour),
            )
            Spacer(GlanceModifier.width(ICON_GAP_DP.dp))
        }
        Text(spec.text, style = lineStyle(spec.textSp, true, colour))
    }
}

/**
 * Builds a text style: system font, centred, in sp.
 *
 * @param sp the text size in sp.
 * @param bold true for bold.
 * @param colour the text colour.
 * @return the style.
 */
private fun lineStyle(sp: Float, bold: Boolean, colour: ColorProvider): TextStyle = TextStyle(
    color = colour,
    fontSize = sp.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    textAlign = TextAlign.Center,
)

/**
 * Gives the one spoken label for the whole widget, which carries every line even when some are not drawn.
 *
 * @param context supplies the strings.
 * @param state ON or PAUSED.
 * @param resumeTime the formatted resume time of a timed pause; null otherwise.
 * @return the label.
 */
private fun spokenLabel(context: Context, state: WidgetState, resumeTime: String?): String = when {
    state is WidgetState.On -> context.getString(R.string.widget_label_on)
    resumeTime != null -> context.getString(R.string.widget_label_paused_until, resumeTime)
    else -> context.getString(R.string.widget_label_paused)
}
