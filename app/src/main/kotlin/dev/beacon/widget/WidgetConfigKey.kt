package dev.beacon.widget

import android.content.res.Configuration

/*
 * Why a configuration key: the widget's fit depends on the font size, the
 * screen density and the language of its words. After only a font-size
 * change, the launcher draws the widget's old lines at the new size, and a
 * refresh that reaches a Glance session still running recomposes nothing,
 * because the saved pause didn't change (PR 3's D1). So the app holds these
 * three as state that the widget's composition reads (ruling F6). Rotation
 * and the other configuration changes leave the key as it is, so they
 * start no Glance session.
 */

/**
 * The parts of the system configuration the widget's fit depends on.
 *
 * @property fontScale the user's font size.
 * @property densityDpi the screen density, which the display-size setting changes.
 * @property locales the languages, as BCP 47 tags in order; the widget's words come from them.
 */
data class WidgetConfigKey(val fontScale: Float, val densityDpi: Int, val locales: String) {
    companion object {
        /**
         * Reads the key from a configuration.
         *
         * @param config the configuration.
         * @return its font scale, density and languages.
         */
        fun of(config: Configuration): WidgetConfigKey =
            WidgetConfigKey(config.fontScale, config.densityDpi, config.locales.toLanguageTags())
    }
}
