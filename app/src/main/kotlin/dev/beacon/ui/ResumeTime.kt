package dev.beacon.ui

import android.content.Context
import java.util.Date

/*
 * Why the platform formatter: the resume time must read the way the phone's
 * own clock does, so a senior who set a 24-hour clock sees "14:30" and one
 * with a 12-hour clock sees "2:30 PM". DateFormat.getTimeFormat follows that
 * setting and the phone's language.
 */

/**
 * Formats the moment a timed pause ends, as a time of day.
 *
 * @param context any context; it supplies the 12- or 24-hour setting and the locale.
 * @param epochMillis the moment to show, in epoch milliseconds.
 * @return the time of day in the phone's own style, unchanged from the platform
 *   (newer ICU may put a narrow no-break space before "PM").
 */
fun formatResumeTime(context: Context, epochMillis: Long): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date(epochMillis))
