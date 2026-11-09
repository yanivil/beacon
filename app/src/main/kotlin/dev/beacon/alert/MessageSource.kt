package dev.beacon.alert

import androidx.annotation.StringRes
import dev.beacon.R
import dev.beacon.settings.FlashColour

/*
 * The only message apps Beacon flashes for (owner's decision, 8 November
 * 2026). Calls and other app activity are deliberately not listed.
 * Declaration order is the order they are shown in.
 */

/**
 * A kind of incoming message Beacon can alert on.
 *
 * @property nameRes string resource for the app's visible name.
 * @property defaultEnabled whether alerts for it start switched on.
 * @property defaultColour the flash colour it starts with.
 * @property fixedPackages Android package names to match; empty when they are
 *   found at run time.
 */
enum class MessageSource(
    @param:StringRes val nameRes: Int,
    val defaultEnabled: Boolean,
    val defaultColour: FlashColour,
    val fixedPackages: List<String>,
) {
    /** Text messages; the default SMS app is looked up at run time, so no fixed packages. */
    SMS(R.string.app_sms, true, FlashColour.YELLOW, emptyList()),

    /** WhatsApp and WhatsApp Business. */
    WHATSAPP(R.string.app_whatsapp, true, FlashColour.GREEN, listOf("com.whatsapp", "com.whatsapp.w4b")),

    /** Telegram and its web-distributed build. Not tested on a phone in Step 1. */
    TELEGRAM(
        R.string.app_telegram,
        false,
        FlashColour.BLUE,
        listOf("org.telegram.messenger", "org.telegram.messenger.web"),
    ),
}
