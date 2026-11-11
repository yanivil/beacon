package dev.beacon.alert

import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.isPausedAt

/*
 * The rules that decide whether a notification is a new message worth a
 * flash. Plain Kotlin with no Android types so every rule is unit-tested.
 * The first failing rule is the reason reported. The "already handled"
 * record is updated for every notification that passes the message checks
 * (rules 1 to 4), whatever the final result, so a message that arrived while
 * paused, switched OFF or from the owner never flashes when re-posted.
 */

/** Why a notification did not start an alert, named after the rule that failed. */
enum class IgnoreReason { NOT_A_SOURCE, ONGOING, GROUP_SUMMARY, NOT_A_MESSAGE, ALREADY_HANDLED, OWN_MESSAGE, IN_USE, PAUSED, APP_OFF }

/** The classifier's answer for one notification. */
sealed interface Decision {
    /**
     * Flash for a new message.
     *
     * @property source the message app it came from.
     */
    data class Alert(val source: MessageSource) : Decision

    /**
     * Do not flash.
     *
     * @property reason the first rule that failed.
     */
    data class Ignore(val reason: IgnoreReason) : Decision
}

/**
 * The newest message time handled for each notification key, in memory only.
 * Used from the main thread only, so it needs no locking.
 */
class HandledRecord {
    private val times = HashMap<String, Long>()

    /**
     * Tells whether a message time is newer than the last one handled for a key.
     *
     * @param key the notification key.
     * @param time the newest message time.
     * @return true if the key has no record or [time] is later than the recorded one.
     */
    fun isNew(key: String, time: Long): Boolean = times[key]?.let { time > it } ?: true

    /**
     * Records a handled message time for a key.
     *
     * @param key the notification key.
     * @param time the message time.
     */
    fun record(key: String, time: Long) {
        times[key] = time
    }

    /**
     * Forgets a key, when its notification is removed.
     *
     * @param key the notification key.
     */
    fun remove(key: String) {
        times.remove(key)
    }

    /** Forgets every key, so a fresh listener connection starts from what is showing then. */
    fun clear() {
        times.clear()
    }
}

/** Applies the message-detection rules (spec section 6). */
object MessageClassifier {
    /**
     * Finds which message source a package belongs to.
     *
     * @param packageName the app's package.
     * @param defaultSmsPackage the phone's default SMS app, or null if none.
     * @return the source, or null if the package is not one.
     */
    fun sourceOf(packageName: String, defaultSmsPackage: String?): MessageSource? =
        MessageSource.entries.firstOrNull { packageName in it.fixedPackages }
            ?: if (defaultSmsPackage != null && packageName == defaultSmsPackage) MessageSource.SMS else null

    /**
     * Decides what to do with a notification.
     *
     * @param facts what was read from the notification.
     * @param settings the current settings.
     * @param nowEpochMillis the current time, for the pause check.
     * @param defaultSmsPackage the phone's default SMS app, or null.
     * @param seen the already-handled record; updated here.
     * @param phoneInUse whether the person is using the phone now: its screen is on and unlocked.
     * @return [Decision.Alert] when every rule passes, otherwise [Decision.Ignore] with the first failing rule.
     */
    fun decide(
        facts: NotificationFacts,
        settings: BeaconSettings,
        nowEpochMillis: Long,
        defaultSmsPackage: String?,
        seen: HandledRecord,
        phoneInUse: Boolean,
    ): Decision {
        val source = sourceOf(facts.packageName, defaultSmsPackage) ?: return Decision.Ignore(IgnoreReason.NOT_A_SOURCE)
        notAMessageReason(facts)?.let { return Decision.Ignore(it) }
        val time = timeOf(facts)
        if (!seen.isNew(facts.key, time)) return Decision.Ignore(IgnoreReason.ALREADY_HANDLED)
        // Record before rules 6 to 9 so own, in-use, paused and OFF messages are never alerted on a re-post.
        seen.record(facts.key, time)
        if (facts.hasMessagingStyle && facts.newestFromOwner == true) return Decision.Ignore(IgnoreReason.OWN_MESSAGE)
        // The owner's decision (11 November 2026): no flash while the person is using the phone. The
        // message app's own banner is the cue then, and the message stays handled, so it can't flash
        // later when the screen goes off.
        if (phoneInUse) return Decision.Ignore(IgnoreReason.IN_USE)
        if (settings.pause.isPausedAt(nowEpochMillis)) return Decision.Ignore(IgnoreReason.PAUSED)
        // A missing map entry falls back to the app's default, as in the settings model.
        if (!(settings.apps[source]?.enabled ?: source.defaultEnabled)) return Decision.Ignore(IgnoreReason.APP_OFF)
        return Decision.Alert(source)
    }

    /**
     * Records a notification that is already showing, without deciding anything.
     * Applies rules 1 to 4 only, so a later re-post of it does not flash.
     *
     * @param facts what was read from the notification.
     * @param defaultSmsPackage the phone's default SMS app, or null.
     * @param seen the already-handled record; updated here.
     */
    fun prime(facts: NotificationFacts, defaultSmsPackage: String?, seen: HandledRecord) {
        if (sourceOf(facts.packageName, defaultSmsPackage) == null || notAMessageReason(facts) != null) return
        seen.record(facts.key, timeOf(facts))
    }

    /**
     * Applies rules 2 to 4.
     *
     * @param facts the notification facts.
     * @return the first failing reason, or null if it is a plain message.
     */
    private fun notAMessageReason(facts: NotificationFacts): IgnoreReason? = when {
        facts.ongoing -> IgnoreReason.ONGOING
        facts.groupSummary -> IgnoreReason.GROUP_SUMMARY
        !facts.hasMessagingStyle && facts.category != "msg" -> IgnoreReason.NOT_A_MESSAGE
        else -> null
    }

    /**
     * The time rule 5 compares: the newest message time, else `when`.
     *
     * @param facts the notification facts.
     * @return the time in milliseconds.
     */
    private fun timeOf(facts: NotificationFacts): Long = facts.newestMessageTime ?: facts.whenMillis
}
