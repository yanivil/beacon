package dev.beacon.alert

import dev.beacon.settings.AppSetting
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.FlashColour
import dev.beacon.settings.Pause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Plain JUnit tests for [MessageClassifier] and [HandledRecord]; no Android types. */
class MessageClassifierTest {
    private val SMS_APP = "com.google.android.apps.messaging"

    /**
     * Builds facts for a typical incoming WhatsApp message, overriding only what a test cares about.
     *
     * @return the facts.
     */
    private fun facts(
        pkg: String = "com.whatsapp",
        key: String = "k1",
        ongoing: Boolean = false,
        summary: Boolean = false,
        category: String? = "msg",
        style: Boolean = true,
        newest: Long? = 1_000,
        own: Boolean? = false,
        whenMillis: Long = 1_000,
    ) = NotificationFacts(pkg, key, ongoing, summary, category, style, newest, own, whenMillis)

    /**
     * Runs the classifier with test defaults.
     *
     * @return the decision.
     */
    private fun decide(
        f: NotificationFacts,
        settings: BeaconSettings = BeaconSettings.DEFAULT,
        now: Long = 0,
        seen: HandledRecord = HandledRecord(),
        inUse: Boolean = false,
    ) = MessageClassifier.decide(f, settings, now, SMS_APP, seen) { inUse }

    private fun paused(p: Pause) = BeaconSettings.DEFAULT.copy(pause = p)

    /** Pins that a normal WhatsApp message alerts. */
    @Test fun whatsAppMessageAlerts() = assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts()))

    /** Pins that WhatsApp Business counts as WhatsApp. */
    @Test fun whatsAppBusinessAlerts() =
        assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(pkg = "com.whatsapp.w4b")))

    /** Pins that an SMS notification with category msg and no style alerts. */
    @Test fun smsWithCategoryMsgAndNoStyleAlerts() {
        val f = facts(pkg = SMS_APP, style = false, newest = null, own = null, whenMillis = 5_000)
        assertEquals(Decision.Alert(MessageSource.SMS), decide(f))
    }

    /** Pins that Telegram is off by default. */
    @Test fun telegramIsOffByDefault() =
        assertEquals(Decision.Ignore(IgnoreReason.APP_OFF), decide(facts(pkg = "org.telegram.messenger.web")))

    /** Pins that other packages, and the SMS app when none is set, are not sources. */
    @Test fun shellAndUnknownAppsAreNotSources() {
        assertEquals(Decision.Ignore(IgnoreReason.NOT_A_SOURCE), decide(facts(pkg = "com.android.shell")))
        assertNull(MessageClassifier.sourceOf(SMS_APP, defaultSmsPackage = null))
    }

    /** Pins that WhatsApp's backup progress (ongoing, no style, no category) is ignored. */
    @Test fun whatsAppBackupIsOngoing() =
        assertEquals(
            Decision.Ignore(IgnoreReason.ONGOING),
            decide(facts(ongoing = true, style = false, category = null, newest = null, own = null)),
        )

    /** Pins that group summaries are skipped. */
    @Test fun groupSummaryIsSkipped() =
        assertEquals(Decision.Ignore(IgnoreReason.GROUP_SUMMARY), decide(facts(summary = true)))

    /** Pins that the first failing rule is the reason reported. */
    @Test fun firstFailingRuleWins() =
        assertEquals(Decision.Ignore(IgnoreReason.ONGOING), decide(facts(ongoing = true, summary = true)))

    /** Pins that no style and no msg category is not a message. */
    @Test fun noStyleAndNoMsgCategoryIsNotAMessage() =
        assertEquals(
            Decision.Ignore(IgnoreReason.NOT_A_MESSAGE),
            decide(facts(style = false, category = "progress", newest = null, own = null)),
        )

    /** Pins that a MessagingStyle message with no category alerts (WhatsApp's silent channel). */
    @Test fun silentChannelMessageAlerts() =
        assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(category = null)))

    /** Pins that a re-post is handled once and a newer message alerts again. */
    @Test fun repeatedUpdateIsHandledOnce() {
        val seen = HandledRecord()
        assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(), seen = seen))
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
        assertEquals(
            Decision.Alert(MessageSource.WHATSAPP),
            decide(facts(newest = 2_000, whenMillis = 2_000), seen = seen),
        )
    }

    /** Pins that without a style the notification's when is the record's time. */
    @Test fun noStyleUsesWhenForTheRecord() {
        val seen = HandledRecord()
        val f = facts(pkg = SMS_APP, style = false, newest = null, own = null, whenMillis = 7_000)
        decide(f, seen = seen)
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(f, seen = seen))
    }

    /** Pins that the owner's own reply is skipped and still recorded. */
    @Test fun ownReplyIsSkippedAndRecorded() {
        val seen = HandledRecord()
        assertEquals(Decision.Ignore(IgnoreReason.OWN_MESSAGE), decide(facts(own = true), seen = seen))
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(own = false), seen = seen))
    }

    /** Pins the pause edges: indefinite, just before the deadline, exactly at it. */
    @Test fun pauseEdges() {
        assertEquals(Decision.Ignore(IgnoreReason.PAUSED), decide(facts(), settings = paused(Pause.Indefinite)))
        assertEquals(
            Decision.Ignore(IgnoreReason.PAUSED),
            decide(facts(), settings = paused(Pause.Until(10_000)), now = 9_999),
        )
        assertEquals(
            Decision.Alert(MessageSource.WHATSAPP),
            decide(facts(), settings = paused(Pause.Until(10_000)), now = 10_000),
        )
    }

    /** Pins that a message seen while paused never flashes later. */
    @Test fun messageSeenWhilePausedNeverFlashesLater() {
        val seen = HandledRecord()
        decide(facts(), settings = paused(Pause.Indefinite), seen = seen)
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
    }

    /** Pins that a message seen while its app is OFF never flashes after it is switched on. */
    @Test fun messageSeenWhileAppOffNeverFlashesLater() {
        val seen = HandledRecord()
        val off = BeaconSettings.DEFAULT.copy(
            apps = BeaconSettings.DEFAULT.apps.mapValues { (s, a) ->
                if (s == MessageSource.WHATSAPP) a.copy(enabled = false) else a
            },
        )
        assertEquals(Decision.Ignore(IgnoreReason.APP_OFF), decide(facts(), settings = off, seen = seen))
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
    }

    /** Pins that priming records messages only, not ongoing notifications. */
    @Test fun primeRecordsOnlyMessages() {
        val seen = HandledRecord()
        MessageClassifier.prime(facts(), SMS_APP, seen)
        MessageClassifier.prime(facts(key = "backup", ongoing = true), SMS_APP, seen)
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen))
        assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(key = "backup"), seen = seen))
    }

    /** Pins that a removed notification's record is forgotten. */
    @Test fun removedKeyIsForgotten() {
        val seen = HandledRecord()
        decide(facts(), seen = seen)
        seen.remove("k1")
        assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(), seen = seen))
    }

    /** While the person is using the phone, a message doesn't flash: its own banner is the cue (owner, 11 November 2026). */
    @Test fun inUseIgnoresTheMessage() =
        assertEquals(Decision.Ignore(IgnoreReason.IN_USE), decide(facts(), inUse = true))

    /** A message that arrived while the phone was in use is handled: it doesn't flash later, once the screen is off. */
    @Test fun aMessageSeenInUseDoesNotFlashLater() {
        val seen = HandledRecord()
        assertEquals(Decision.Ignore(IgnoreReason.IN_USE), decide(facts(), seen = seen, inUse = true))
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen, inUse = false))
        // A newer message in the same notification flashes, once the phone isn't in use.
        assertEquals(Decision.Alert(MessageSource.WHATSAPP), decide(facts(newest = 2_000), seen = seen, inUse = false))
    }

    /** The owner's own message is named as such even while the phone is in use. */
    @Test fun ownMessageComesBeforeInUse() =
        assertEquals(Decision.Ignore(IgnoreReason.OWN_MESSAGE), decide(facts(own = true), inUse = true))

    /** In use is named before a pause and an app switched OFF. */
    @Test fun inUseComesBeforePausedAndAppOff() {
        assertEquals(Decision.Ignore(IgnoreReason.IN_USE), decide(facts(), paused(Pause.Indefinite), inUse = true))
        val whatsAppOff = BeaconSettings.DEFAULT.copy(
            apps = BeaconSettings.DEFAULT.apps + (MessageSource.WHATSAPP to AppSetting(false, FlashColour.GREEN)),
        )
        assertEquals(Decision.Ignore(IgnoreReason.IN_USE), decide(facts(), whatsAppOff, inUse = true))
    }

    /** A repeat of a message seen while in use is already handled, even while still in use: rule 5 comes first. */
    @Test fun repeatWhileInUseIsAlreadyHandled() {
        val seen = HandledRecord()
        assertEquals(Decision.Ignore(IgnoreReason.IN_USE), decide(facts(), seen = seen, inUse = true))
        assertEquals(Decision.Ignore(IgnoreReason.ALREADY_HANDLED), decide(facts(), seen = seen, inUse = true))
    }

    /** The in-use check is asked only for a message that reaches its rule, not for other apps' notifications. */
    @Test fun inUseIsAskedOnlyAtItsRule() {
        var asked = 0
        MessageClassifier.decide(facts(pkg = "com.example"), BeaconSettings.DEFAULT, 0, SMS_APP, HandledRecord()) { asked++; true }
        assertEquals(0, asked)
        MessageClassifier.decide(facts(), BeaconSettings.DEFAULT, 0, SMS_APP, HandledRecord()) { asked++; true }
        assertEquals(1, asked)
    }
}
