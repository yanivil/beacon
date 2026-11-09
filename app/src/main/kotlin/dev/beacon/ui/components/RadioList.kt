package dev.beacon.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics

/*
 * Why a grid of options states its own list positions: TalkBack says "2 of 4"
 * from the position each option reports. For a selectableGroup, Compose 1.11
 * works the positions out from the order in which each option is placed in
 * its parent layout. In an AdaptiveGrid every option sits alone in its own
 * grid cell, so every option counted as first, and TalkBack read them all as
 * "1 of 4" (or "1 of 5" for the swatches). Beacon's rule is that every radio
 * group reads as a list, whatever its visual columns, so a grid group says how
 * many rows it has (one column) and each option says which row it is.
 *
 * Why these replace selectableGroup instead of joining it: when the parent is
 * a selectableGroup, Compose writes its own computed position over the one an
 * option states, so the two can't be combined. On Android, selectableGroup
 * does nothing else, so nothing is lost.
 *
 * One-column groups whose options are direct children of a Column (blink
 * speed, colour mode) keep selectableGroup: Compose counts them correctly.
 */

/**
 * Marks this layout as a group of [itemCount] radio options that TalkBack
 * reads as a one-column list ("In list, 4 items"). Use it instead of
 * `selectableGroup()` on a group laid out by [AdaptiveGrid], and give each
 * option [radioListItem].
 *
 * @param itemCount how many options the group holds.
 * @return this modifier with the group's list size.
 */
fun Modifier.radioList(itemCount: Int): Modifier =
    semantics { collectionInfo = CollectionInfo(rowCount = itemCount, columnCount = 1) }

/**
 * Gives one option of a [radioList] its place in the list, so TalkBack reads
 * "index + 1 of count". Put it on the option itself: the node with the
 * RadioButton role.
 *
 * @param index the option's 0-based position in reading order.
 * @return this modifier with the option's list position.
 */
fun Modifier.radioListItem(index: Int): Modifier =
    semantics { collectionItemInfo = CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1) }
