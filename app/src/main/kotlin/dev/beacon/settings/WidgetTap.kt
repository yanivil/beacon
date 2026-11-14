package dev.beacon.settings

/*
 * Why the widget's tap is a choice: the widget's one tap first paused alerts
 * for 1 hour, so they came back on by themselves and nobody could miss
 * messages for longer by forgetting. Some testers want the widget to turn
 * alerts OFF until they tap it again, like Home's "Pause all alerts" (the
 * owner's decision, 14 November 2026). The widget keeps its single big
 * button, and Alert settings chooses what it does. The 1-hour pause stays
 * the default, so nothing changes for anyone who doesn't choose.
 */

/** What one tap on the widget does while alerts are ON. A tap while paused always turns alerts back ON. */
enum class WidgetTap {
    /** Pause for 1 hour; alerts come back on by themselves ([Pause.Until]). */
    PAUSE_ONE_HOUR,

    /** Turn alerts OFF until the next tap, or Home or Alert settings, turns them back on ([Pause.Indefinite]). */
    OFF_UNTIL_TAPPED,
}
