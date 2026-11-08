package dev.beacon.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Why this test exists: at large font sizes two columns would break words,
 * so the grid falls back to one column (spec section 3, row 10). The
 * threshold is plain arithmetic, so it is pinned here without a device.
 */

/** Pins when [gridColumns] gives two columns and when it gives one. */
class AdaptiveGridTest {

    /** Two columns from 300 dp of width per unit of font scale, else one. */
    @Test fun columns() {
        assertEquals(2, gridColumns(358.dp, 1.0f))
        assertEquals(2, gridColumns(300.dp, 1.0f))
        assertEquals(1, gridColumns(299.dp, 1.0f))
        assertEquals(1, gridColumns(358.dp, 1.3f))
    }
}
