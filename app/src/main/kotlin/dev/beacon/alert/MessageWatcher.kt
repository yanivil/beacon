package dev.beacon.alert

import dev.beacon.settings.BeaconSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
 * Why a watcher between the listener and the controller: the listener is an
 * Android service that is hard to test, while the rules (spec section 6) need
 * the saved settings. This class applies the classifier with those settings and
 * starts the alert. A decision must never use the first-run defaults: the
 * DataStore loads asynchronously, and a saved "paused" would be missed, so a
 * post waits for the first real settings value. Everything runs on the main
 * thread, which is what makes the unlocked HandledRecord safe.
 */

/**
 * Turns notification events into real alerts.
 *
 * @param settings the saved settings; null until the first load finishes.
 * @param alerts the controller that runs alerts.
 * @param scope a main-thread scope (Dispatchers.Main.immediate), so a decision with loaded settings runs at once.
 * @param defaultSmsPackage the phone's default SMS app, read on every call because the user can change it.
 * @param phoneInUse whether the person is using the phone now (see [phoneInUse]); read when a message's decision
 *   reaches that rule, after any wait for the settings.
 * @param nowEpochMillis the clock for the pause check.
 * @param log receives every decision (debug builds only; never anything else about the notification).
 */
class MessageWatcher(
    private val settings: StateFlow<BeaconSettings?>,
    private val alerts: AlertController,
    private val scope: CoroutineScope,
    private val defaultSmsPackage: () -> String?,
    private val phoneInUse: () -> Boolean,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val log: (Decision) -> Unit = {},
) {
    private val seen = HandledRecord()

    /**
     * Records the notifications showing when the listener connects, so they don't flash later.
     *
     * @param showing the facts of every active notification.
     */
    fun onConnected(showing: List<NotificationFacts>) {
        // Keys removed while the listener was disconnected were never forgotten, and priming
        // must not lower a newer record: start clean, then prime from what is showing now.
        seen.clear()
        val sms = defaultSmsPackage()
        showing.forEach { MessageClassifier.prime(it, sms, seen) }
    }

    /**
     * Decides about a posted notification and starts an alert for a new message.
     *
     * @param facts what was read from the notification.
     */
    fun onPosted(facts: NotificationFacts) {
        scope.launch {
            // Waits (in arrival order) for the saved settings; never decides with the defaults.
            val current = settings.value ?: settings.filterNotNull().first()
            val decision = MessageClassifier.decide(facts, current, nowEpochMillis(), defaultSmsPackage(), seen, phoneInUse)
            log(decision)
            if (decision is Decision.Alert) {
                // A false answer (busy) is fine: the message is already recorded, so it never stacks later.
                alerts.start(
                    AlertRequest(
                        AlertKind.REAL, current.colourFor(decision.source), current.speed,
                        current.alertLengthMillis(), decision.source,
                    ),
                )
            }
        }
    }

    /**
     * Forgets a removed notification so its next post counts as new.
     *
     * @param key the notification key.
     */
    fun onRemoved(key: String) = seen.remove(key)
}
