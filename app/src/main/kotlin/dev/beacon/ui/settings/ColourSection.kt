package dev.beacon.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.beacon.R
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.ColourMode
import dev.beacon.ui.colourName
import dev.beacon.ui.components.OptionButton
import dev.beacon.ui.components.OutlineButton
import dev.beacon.ui.components.SectionHeading
import dev.beacon.ui.components.SwatchGrid
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.BeaconText
import dev.beacon.ui.theme.BeaconType

/*
 * Why only one app's grid is open at a time: five swatches per app, three
 * apps, would be a very long screen. The open app lives in the shell
 * (editingApp), so opening another app's grid closes the first, and the
 * choice survives rotation. The Change/Done button gets a spoken label that
 * names the app, because several identical "Change" buttons are otherwise
 * indistinguishable to TalkBack.
 */

/** Width per unit of font scale below which a card's button moves under the app name. */
private const val SideBySideMinWidth = 250f

/**
 * The "Screen flash color" section: the two mode options, then either one
 * swatch grid or one card per app.
 *
 * @param settings the current settings.
 * @param editingApp the app whose grid is open in per-app mode, or null.
 * @param actions the changes the screen can ask for.
 */
@Composable
fun ColourSection(settings: BeaconSettings, editingApp: MessageSource?, actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(stringResource(R.string.colour_heading))
        BeaconText(stringResource(R.string.colour_hint), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                ColourMode.SAME_FOR_ALL to R.string.colour_mode_same,
                ColourMode.PER_APP to R.string.colour_mode_per_app,
            ).forEach { (mode, label) ->
                OptionButton(
                    label = stringResource(label),
                    selected = settings.colourMode == mode,
                    onSelect = { actions.setColourMode(mode) },
                    minHeight = 76.dp,
                    modifier = Modifier.fillMaxWidth(),
                    labelStyle = BeaconType.Button,
                )
            }
        }
        if (settings.colourMode == ColourMode.SAME_FOR_ALL) {
            SwatchGrid(settings.globalColour, actions.setGlobalColour, 96.dp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Declaration order is the design's order: SMS, WhatsApp, Telegram.
                MessageSource.entries.forEach { source ->
                    AppColourCard(source, settings, open = editingApp == source, actions)
                }
            }
        }
    }
}

/**
 * One app's card: its name and colour, a Change/Done button and, when open,
 * its swatch grid.
 *
 * @param source the app.
 * @param settings the current settings.
 * @param open true when this app's grid is open.
 * @param actions the changes the screen can ask for.
 */
@Composable
private fun AppColourCard(source: MessageSource, settings: BeaconSettings, open: Boolean, actions: SettingsActions) {
    val shape = RoundedCornerShape(20.dp)
    val colour = settings.colourFor(source)
    val name = stringResource(source.nameRes)
    Column(
        Modifier
            .fillMaxWidth()
            .background(BeaconColors.Card, shape)
            .border(2.dp, BeaconColors.Border, shape)
            // The design's 12 dp padding plus the 2 dp border, which is drawn inside the bounds.
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val spoken = stringResource(if (open) R.string.colour_done_for else R.string.colour_change_for, name)
        val state = stringResource(if (open) R.string.state_expanded else R.string.state_collapsed)
        // The name and its colour line, shared by the side-by-side and the stacked layouts.
        val title: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BeaconText(name, style = BeaconType.RowTitle.copy(color = BeaconColors.Text))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val swatchShape = RoundedCornerShape(6.dp)
                    // Only a picture: the colour's name beside it carries the meaning.
                    Box(
                        Modifier
                            .clearAndSetSemantics {}
                            .size(24.dp)
                            .background(Color(colour.argb), swatchShape)
                            .border(2.dp, BeaconColors.White, swatchShape),
                    )
                    BeaconText(colourName(colour), style = BeaconType.Body.copy(color = BeaconColors.TextSecondary))
                }
            }
        }
        // Same Change/Done button in both layouts; the caller says how wide it is.
        val button: @Composable (Modifier) -> Unit = { modifier ->
            OutlineButton(
                text = stringResource(if (open) R.string.colour_done else R.string.colour_change),
                onClick = { actions.setEditingApp(if (open) null else source) },
                content = BeaconColors.White,
                icon = null,
                minHeight = 64.dp,
                style = BeaconType.SmallButton,
                cornerRadius = 16.dp,
                // Merging makes one node with the spoken label, the state and the button's click.
                modifier = modifier
                    .sizeIn(minWidth = 120.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = spoken
                        stateDescription = state
                    },
            )
        }
        BoxWithConstraints {
            // Beside the name only while a word of it still fits: the room needed per unit of
            // font scale is about 250 dp (the button's 120 dp, the gap, and "WhatsApp" at 24 sp).
            // Otherwise the button goes under the name, full width, instead of breaking words.
            if (maxWidth.value / LocalDensity.current.fontScale >= SideBySideMinWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    title(Modifier.weight(1f))
                    button(Modifier)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    title(Modifier)
                    button(Modifier.fillMaxWidth())
                }
            }
        }
        if (open) SwatchGrid(colour, { actions.setAppColour(source, it) }, 88.dp)
    }
}
