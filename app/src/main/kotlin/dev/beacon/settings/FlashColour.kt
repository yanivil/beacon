package dev.beacon.settings

import androidx.annotation.StringRes
import dev.beacon.R

/*
 * The five flash colours from the design. Declaration order is the order the
 * colour pickers show them in.
 */

/**
 * A colour the screen can flash.
 *
 * @property argb colour as 0xAARRGGBB, held in a Long so the alpha byte fits.
 * @property nameRes string resource for the colour's visible name.
 */
enum class FlashColour(val argb: Long, @StringRes val nameRes: Int) {
    RED(0xFFFF3B30, R.string.colour_red),
    YELLOW(0xFFFFD60A, R.string.colour_yellow),
    BLUE(0xFF0A84FF, R.string.colour_blue),
    GREEN(0xFF30D158, R.string.colour_green),
    WHITE(0xFFFFFFFF, R.string.colour_white),
}
