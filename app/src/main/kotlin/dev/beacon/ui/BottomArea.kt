package dev.beacon.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.beacon.R
import dev.beacon.ui.components.BeaconIcon
import dev.beacon.ui.components.BeaconIcons
import dev.beacon.ui.components.FilledButton
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconType

/*
 * Why the bottom area is its own file: the Test alert button and the tab bar
 * are shared by both tabs, and the shell decides where the button goes. The
 * pieces live here; BeaconAppScreen measures them and places them.
 *
 * Why the Test alert button can leave the fixed area: the design always pins
 * it above the tabs, but at the largest font the button and the tabs together
 * can fill most of a small screen, leaving almost no room for the content. So
 * it stays fixed only while the whole fixed area takes at most a third of the
 * window; otherwise it becomes the last item of the scrolling content, and
 * only the tab bar stays fixed (spec section 3, row 9).
 *
 * Why the tabs are selectable Tabs in a selectable group: TalkBack must read
 * each one as a Tab, say "Selected" for the current one and count "1 of 2",
 * which the design's aria-current buttons don't give. The selected tab is
 * also shown by shape (the top bar and the pill) and a heavier label, never
 * by colour alone.
 */

/** The test tag of the Test alert button. */
internal const val TEST_BUTTON_TAG = "test_button"

/** The width of the line along the top of the button container and the tab bar. */
private val DividerWidth = 2.dp

/** The height of the bar along the top of the selected tab. */
private val TabTopBar = 6.dp

/** Beacon's two screens, in the order of the tab bar. */
enum class Tab {
    /** Home: the alerts card, the apps and the summary. */
    HOME,

    /** Alert settings: duration, speed and colour. */
    SETTINGS,
}

/**
 * Whether the Test alert button stays fixed above the tabs.
 *
 * @param fixedAreaHeightPx the height of the tab bar plus the button's container, in pixels.
 * @param windowHeightPx the window's height, in pixels.
 * @return true while the fixed area takes at most a third of the window.
 */
fun testButtonFixed(fixedAreaHeightPx: Int, windowHeightPx: Int): Boolean = fixedAreaHeightPx * 3 <= windowHeightPx

/**
 * The white "Test alert (3 sec)" button, the full width it is given.
 *
 * @param onClick called when the button is tapped.
 */
@Composable
fun TestAlertButton(onClick: () -> Unit) {
    FilledButton(
        text = stringResource(R.string.test_button),
        onClick = onClick,
        background = BeaconColors.White,
        content = BeaconColors.OnAccent,
        icon = BeaconIcons.Bolt,
        minHeight = 76.dp,
        style = BeaconType.TestButton,
        cornerRadius = 20.dp,
        modifier = Modifier.fillMaxWidth().testTag(TEST_BUTTON_TAG),
        secondaryText = stringResource(R.string.test_button_length),
    )
}

/**
 * The strip that holds the Test alert button: the page colour with a line
 * along its top, fixed above the tabs or at the end of the content.
 *
 * @param onClick called when the button is tapped.
 */
@Composable
fun TestButtonBar(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Page)
            .topBorder(DividerWidth, BeaconColors.Divider)
            // Keeps the button clear of a side navigation bar or cutout (landscape); the tab bar
            // covers the bottom. Inside the content these insets are already consumed, so no double padding.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TestAlertButton(onClick)
    }
}

/**
 * The flat bottom bar with exactly two equal-width tabs, Home and Alert
 * settings. Its colour extends behind the system navigation bar, and the
 * tabs keep clear of side bars and cutouts.
 *
 * @param tab the tab now shown.
 * @param onSelect called with the tab the user tapped.
 */
@Composable
fun TabBar(tab: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.TabBar)
            .topBorder(DividerWidth, BeaconColors.Divider)
            // After the background, so the bar's colour shows behind the navigation bar,
            // while the tabs stay clear of it and of a cutout at the side.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            // As tall as the taller tab, so both tabs match when one label wraps.
            .height(IntrinsicSize.Min)
            .selectableGroup(),
    ) {
        val tabModifier = Modifier.weight(1f).fillMaxHeight()
        TabItem(BeaconIcons.Home, R.string.tab_home, tab == Tab.HOME, { onSelect(Tab.HOME) }, tabModifier)
        TabItem(BeaconIcons.Sliders, R.string.tab_settings, tab == Tab.SETTINGS, { onSelect(Tab.SETTINGS) }, tabModifier)
    }
}

/**
 * One tab: a top bar, an icon on a pill and a label, centred.
 *
 * When selected, the top bar and the pill are yellow, the icon is dark and
 * the label is weight 800. Otherwise there is no bar or pill, the icon is
 * white and the label is weight 700.
 *
 * @param icon the decorative icon; the label is what TalkBack reads.
 * @param label the label's string resource.
 * @param selected true for the tab now shown.
 * @param onClick called when the tab is tapped.
 * @param modifier the modifier that sizes the tab in the bar.
 */
@Composable
private fun TabItem(icon: ImageVector, @StringRes label: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 92.dp)
            // Before the bar's padding, so the whole tab, bar included, takes the tap.
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            // A transparent bar when not selected, like the design's transparent border, keeps both tabs aligned.
            .topBorder(TabTopBar, if (selected) BeaconColors.Accent else Color.Transparent)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 72.dp, height = 40.dp)
                .background(if (selected) BeaconColors.Accent else Color.Transparent, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BeaconIcon(icon, if (selected) BeaconColors.OnAccent else BeaconColors.White, 28.dp)
        }
        // No maxLines and no ellipsis: at large font sizes the label wraps and the bar grows.
        BasicText(
            text = stringResource(label),
            style = BeaconType.Sub.copy(
                color = BeaconColors.White,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                lineHeight = 1.2.em,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * Draws a line of [colour] along the top edge and pads the content below
 * it, like CSS's border-top.
 *
 * @param width the line's thickness.
 * @param colour the line's colour.
 * @return this modifier with the line and the padding added.
 */
private fun Modifier.topBorder(width: Dp, colour: Color): Modifier =
    drawBehind { drawRect(colour, size = Size(size.width, width.toPx())) }.padding(top = width)
