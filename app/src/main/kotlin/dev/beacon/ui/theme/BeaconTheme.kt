package dev.beacon.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/*
 * Why locals instead of MaterialTheme: Beacon uses no Material 3. Components
 * draw text with BasicText and read these two locals, so a screen only has to
 * be wrapped in BeaconTheme to get the right default font and colour.
 *
 * Why a custom ripple: material-ripple no longer offers ripple() (from 1.11,
 * and 1.12 has none either: it moved into Material 2/3, which Beacon must not
 * use) and rememberRipple() is a compile error. createRippleModifierNode() is
 * the supported way for a design system to build its own ripple.
 */

/** Default text style for [androidx.compose.foundation.text.BasicText] in Beacon components. */
val LocalBeaconTextStyle = compositionLocalOf<TextStyle> { BeaconType.Body }

/** Default text colour for Beacon components. */
val LocalContentColour = compositionLocalOf<Color> { BeaconColors.Text }

/**
 * Beacon's press feedback: a white, bounded ripple.
 *
 * White shows up on the dark page and on the yellow accent alike.
 */
private object WhiteRipple : IndicationNodeFactory {
    /** Alpha levels for a white ripple; stronger than the default so it is visible on yellow. */
    private val alpha = RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.24f,
        hoveredAlpha = 0.08f,
        pressedAlpha = 0.24f,
    )

    /**
     * Creates the ripple node for one component.
     *
     * @param interactionSource the source whose press events drive the ripple.
     * @return the node that draws the ripple.
     */
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        createRippleModifierNode(
            interactionSource = interactionSource,
            bounded = true,
            radius = Dp.Unspecified,
            color = ColorProducer { Color.White },
            rippleAlpha = { alpha },
        )

    /** Two ripples are interchangeable only if they are this same object. */
    override fun equals(other: Any?): Boolean = other === this

    /** Constant hash, consistent with [equals] for a singleton. */
    override fun hashCode(): Int = -1
}

/**
 * Wraps content with Beacon's press feedback and text defaults.
 *
 * @param content the UI to show.
 */
@Composable
fun BeaconTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIndication provides WhiteRipple,
        LocalBeaconTextStyle provides BeaconType.Body,
        LocalContentColour provides BeaconColors.Text,
        content = content,
    )
}
