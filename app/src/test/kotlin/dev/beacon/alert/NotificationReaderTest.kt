package dev.beacon.alert

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Robolectric tests for [factsOf]. */
@RunWith(AndroidJUnit4::class)
class NotificationReaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val me = Person.Builder().setName("Me").setKey("me").build()
    private val ann = Person.Builder().setName("Ann").setKey("ann").build()

    /**
     * Builds a messaging notification and reads its facts.
     *
     * @param extra adds more messages to the style after "hello" at 5_000 from Ann.
     * @return the facts.
     */
    private fun read(extra: (NotificationCompat.MessagingStyle) -> Unit = {}): NotificationFacts {
        val style = NotificationCompat.MessagingStyle(me).addMessage("hello", 5_000, ann)
        extra(style)
        val n = NotificationCompat.Builder(context, "c")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(style).setWhen(4_000).setCategory(NotificationCompat.CATEGORY_MESSAGE).build()
        return factsOf("pkg", "key", n)
    }

    /** Pins that the style, newest time, sender, when and category are read. */
    @Test fun readsMessagingStyle() {
        val f = read()
        assertTrue(f.hasMessagingStyle)
        assertEquals(5_000L, f.newestMessageTime)
        assertEquals(false, f.newestFromOwner)
        assertEquals(4_000L, f.whenMillis)
        assertEquals("msg", f.category)
    }

    /** Pins that the newest message decides, and a null sender is the owner. */
    @Test fun newestMessageDecides() {
        val f = read { it.addMessage("ok", 6_000, null as Person?) }
        assertEquals(6_000L, f.newestMessageTime)
        assertEquals(true, f.newestFromOwner)
    }

    /** Pins that a sender whose key equals the user's key is the owner. */
    @Test fun ownerByKey() {
        val f = read { it.addMessage("ok", 6_000, Person.Builder().setName("Other").setKey("me").build()) }
        assertEquals(true, f.newestFromOwner)
    }

    /**
     * Builds a messaging notification the way WhatsApp does: no Person has a key, and the
     * owner's own reply carries the owner's name (seen on the test phone, phone checks P3).
     *
     * @param newestSender the sender of the newest message.
     * @return the facts.
     */
    private fun readKeyless(newestSender: Person): NotificationFacts {
        val user = Person.Builder().setName("You").build()
        val style = NotificationCompat.MessagingStyle(user)
            .addMessage("hello", 5_000, Person.Builder().setName("Ann").build())
            .addMessage("ok", 6_000, newestSender)
        val n = NotificationCompat.Builder(context, "c")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(style).setWhen(6_000).setCategory(NotificationCompat.CATEGORY_MESSAGE).build()
        return factsOf("com.whatsapp", "key", n)
    }

    /** With no keys anywhere, a newest sender named as the owner is the owner: WhatsApp's own reply. */
    @Test fun ownerByNameWhenNoPersonHasAKey() {
        assertEquals(true, readKeyless(Person.Builder().setName("You").build()).newestFromOwner)
    }

    /** With no keys anywhere, a newest sender with another name is not the owner: an incoming message. */
    @Test fun otherNameWithoutKeysIsNotTheOwner() {
        assertEquals(false, readKeyless(Person.Builder().setName("Ann").build()).newestFromOwner)
    }

    /** When the sender has a key, the keys decide, even if the name is the owner's. */
    @Test fun keysDecideWhenTheSenderHasOne() {
        val f = read { it.addMessage("ok", 6_000, Person.Builder().setName("Me").setKey("ann").build()) }
        assertEquals(false, f.newestFromOwner)
    }

    /** When only the owner has a key, a keyless sender with the owner's name is still not the owner. */
    @Test fun nameIsNotComparedWhenTheOwnerHasAKey() {
        val f = read { it.addMessage("ok", 6_000, Person.Builder().setName("Me").build()) }
        assertEquals(false, f.newestFromOwner)
    }

    /** The facts of a keyless reply hold no names either. */
    @Test fun keylessOwnerCheckHoldsNoNames() {
        val text = readKeyless(Person.Builder().setName("You").build()).toString()
        for (secret in listOf("hello", "ok", "Ann", "You")) assertFalse(secret, Regex("\\b$secret\\b").containsMatchIn(text))
    }

    /** Pins the ongoing and group-summary flags, and a plain notification having no style. */
    @Test fun readsFlags() {
        fun build(block: NotificationCompat.Builder.() -> Unit): Notification =
            NotificationCompat.Builder(context, "c").setSmallIcon(android.R.drawable.ic_dialog_info).apply(block).build()
        assertTrue(factsOf("p", "k", build { setOngoing(true) }).ongoing)
        assertTrue(factsOf("p", "k", build { setGroup("g").setGroupSummary(true) }).groupSummary)
        val plain = factsOf("p", "k", build { })
        assertFalse(plain.hasMessagingStyle)
        assertNull(plain.newestMessageTime)
        assertNull(plain.newestFromOwner)
    }

    /**
     * Pins that the facts hold no message text and no names. Whole words only, because the
     * field name `newestMessageTime` itself contains "Me".
     */
    @Test fun holdsNoContent() {
        val text = read { it.addMessage("ok", 6_000, null as Person?) }.toString()
        for (secret in listOf("hello", "ok", "Ann", "Me")) assertFalse(secret, Regex("\\b$secret\\b").containsMatchIn(text))
    }
}
