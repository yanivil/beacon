package dev.beacon.widget

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateFormat
import java.util.TimeZone

/*
 * Why a configuration key: the widget's drawing depends on more than the
 * saved settings. Its fit depends on the font size, the screen density and
 * the language of its words, and its resume time on the 12/24-hour setting
 * and the time zone. A Glance session that is still running recomposes only
 * when state it reads changes. After only a font-size change, the launcher
 * draws the widget's old lines at the new size, and a refresh that reaches
 * a running session recomposes nothing, because the saved pause didn't
 * change (PR 3's D1). The same held for the resume time after a 12/24-hour
 * or time-zone change. So the app holds all five as state that the widget's
 * composition reads (ruling F6, extended by follow-up R1): a change of any
 * of them composes the widget again, and the resume time is formatted again
 * in that same composition. Rotation and the other configuration changes
 * leave the key as it is, so they start no Glance session.
 */

/**
 * Everything the widget's drawing depends on outside the saved settings: the parts of the system
 * configuration its fit depends on, and the clock settings its resume time is formatted with.
 *
 * @property fontScale the user's font size.
 * @property densityDpi the screen density, which the display-size setting changes.
 * @property locales the languages, as BCP 47 tags in order; the widget's words come from them.
 * @property is24Hour true when the phone shows times in the 24-hour format.
 * @property timeZone the ID of the default time zone, such as "Asia/Jerusalem"; the resume time is shown in it.
 */
data class WidgetConfigKey(
    val fontScale: Float,
    val densityDpi: Int,
    val locales: String,
    val is24Hour: Boolean,
    val timeZone: String,
) {
    companion object {
        /**
         * Reads the key from a configuration and the phone's clock settings.
         *
         * @param context any context of this app; it gives the 12/24-hour setting.
         * @param config the configuration.
         * @return its font scale, density and languages, with the 12/24-hour setting and the default time zone.
         */
        fun of(context: Context, config: Configuration): WidgetConfigKey = WidgetConfigKey(
            fontScale = config.fontScale,
            densityDpi = config.densityDpi,
            locales = config.locales.toLanguageTags(),
            // The setting DateFormat.getTimeFormat follows when the widget formats the resume time;
            // with none saved, it follows the language.
            is24Hour = DateFormat.is24HourFormat(context),
            // Read each time: Android resets a running process's default zone when the zone changes,
            // before it delivers TIMEZONE_CHANGED.
            timeZone = TimeZone.getDefault().id,
        )
    }
}
