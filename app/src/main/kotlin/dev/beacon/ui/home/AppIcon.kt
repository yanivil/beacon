package dev.beacon.ui.home

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Telephony
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.beacon.alert.MessageSource
import dev.beacon.ui.components.BeaconIcon
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.theme.BeaconColors

/*
 * Why the real launcher icon first: a senior recognises WhatsApp by its own
 * icon, not by a drawn placeholder. The icon is looked up once per app and
 * kept (remember), because decoding it on every recomposition would be slow.
 * When no package of the app is installed (or Android hides it), the design's
 * own glyph is shown instead, so the row never has an empty tile.
 * The icon is decorative: the row's name is its label for TalkBack.
 */

/**
 * The icon for a message app, on Home's 52 dp tile: the app's launcher icon
 * at 40 dp when installed, otherwise the design's glyph at 30 dp in white
 * (test tag `glyph_<SOURCE>`). It has no content description.
 *
 * @param source the message app.
 */
@Composable
fun AppIcon(source: MessageSource) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 40.dp.roundToPx() }
    // Decoded once per app and size, not on every recomposition. Null means: show the glyph.
    val bitmap = remember(source, px) {
        loadLauncherIcon(context, source)?.let { drawable ->
            // A drawable with no intrinsic size (a plain colour, some adaptive icons) is no usable icon.
            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                null
            } else {
                try {
                    drawable.toBitmap(px, px).asImageBitmap()
                } catch (_: IllegalArgumentException) {
                    // An odd drawable must never crash Home: fall back to the glyph.
                    null
                }
            }
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        BeaconIcon(glyphFor(source), BeaconColors.White, 30.dp, Modifier.testTag("glyph_${source.name}"))
    }
}

/**
 * The design's glyph for a message app.
 *
 * @param source the message app.
 * @return its glyph.
 */
private fun glyphFor(source: MessageSource): ImageVector = when (source) {
    MessageSource.SMS -> BeaconIcons.Sms
    MessageSource.WHATSAPP -> BeaconIcons.WhatsApp
    MessageSource.TELEGRAM -> BeaconIcons.Telegram
}

/**
 * Finds the launcher icon of the first installed package of [source].
 *
 * @param context used to reach the package manager.
 * @param source the message app.
 * @return the icon, or null if no package of the app can be resolved.
 */
private fun loadLauncherIcon(context: Context, source: MessageSource): Drawable? {
    // SMS has no fixed package: the default SMS app is whatever the user chose (null if none).
    val packages = if (source == MessageSource.SMS) {
        listOfNotNull(Telephony.Sms.getDefaultSmsPackage(context))
    } else {
        source.fixedPackages
    }
    for (name in packages) {
        try {
            return context.packageManager.getApplicationIcon(name)
        } catch (_: PackageManager.NameNotFoundException) {
            // Not installed (or hidden from Beacon): try the next package, then fall back to the glyph.
        }
    }
    return null
}
