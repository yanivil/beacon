package dev.beacon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why the column count adapts: the design always uses two columns, but at
 * large font sizes two columns would break words in half. Dividing the width
 * by the font scale measures the room in "text units", so the grid has two
 * columns only while at least 300 dp of width per unit of font scale is
 * available, and one column otherwise (spec section 3, row 10).
 */

/** The gap between rows and between columns, as in the design. */
private val GridGap = 12.dp

/**
 * How many columns a grid gets.
 *
 * @param maxWidth the width available to the grid.
 * @param fontScale the system font scale (1.0 = default size).
 * @return 2 when `maxWidth / fontScale` is at least 300 dp, otherwise 1.
 */
fun gridColumns(maxWidth: Dp, fontScale: Float): Int = if (maxWidth.value / fontScale >= 300f) 2 else 1

/**
 * Lays out [count] items in one or two columns (see [gridColumns]), with
 * 12 dp gaps. Items in the same row are stretched to the tallest one, like
 * the design's CSS grid. In one-column mode every item is full width.
 *
 * Each item is measured with an exact width and height, so items should
 * fill what they are given. Do not place the grid inside a parent that asks
 * for intrinsic sizes: it measures its width with BoxWithConstraints, which
 * cannot answer them.
 *
 * @param count how many items there are.
 * @param modifier the modifier for the grid; any padding in it is taken off
 *   the width before the columns are chosen.
 * @param spansFullRow true for an item that takes a whole row of its own,
 *   such as the White swatch.
 * @param item draws the item at the given index.
 */
@Composable
fun AdaptiveGrid(
    count: Int,
    modifier: Modifier = Modifier,
    spansFullRow: (Int) -> Boolean = { false },
    item: @Composable (Int) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val columns = gridColumns(maxWidth, LocalDensity.current.fontScale)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GridGap)) {
            gridRows(count, columns, spansFullRow).forEach { row ->
                // IntrinsicSize.Min: the row is as tall as its tallest item, and
                // fillMaxHeight stretches the shorter one to match.
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(GridGap),
                ) {
                    row.forEach { index ->
                        Box(Modifier.weight(1f).fillMaxHeight(), propagateMinConstraints = true) { item(index) }
                    }
                    // A lone item in a two-column row keeps half the width, as in a CSS grid.
                    if (row.size < columns && !spansFullRow(row.first())) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Splits the item indexes into rows.
 *
 * A full-row item ends the row before it, even if that row is not full, and
 * takes a row of its own, as CSS grid's auto-placement does.
 *
 * @param count how many items there are.
 * @param columns 1 or 2.
 * @param spansFullRow true for an item that takes a whole row.
 * @return the rows, each a list of item indexes in order.
 */
private fun gridRows(count: Int, columns: Int, spansFullRow: (Int) -> Boolean): List<List<Int>> {
    val rows = mutableListOf<List<Int>>()
    var current = mutableListOf<Int>()
    for (index in 0 until count) {
        if (spansFullRow(index)) {
            if (current.isNotEmpty()) rows += current
            current = mutableListOf()
            rows += listOf(index)
        } else {
            current += index
            if (current.size == columns) {
                rows += current
                current = mutableListOf()
            }
        }
    }
    if (current.isNotEmpty()) rows += current
    return rows
}
