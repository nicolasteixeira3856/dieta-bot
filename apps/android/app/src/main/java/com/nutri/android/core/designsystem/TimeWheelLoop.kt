package com.nutri.android.core.designsystem

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.abs

// Wheel logic of the meal time picker (AeroTimeWheelDialog, o3t).

/** Virtual indices leave years of scrolling on either side without allocating a backing list. */
internal object TimeWheelLoop {
    const val COUNT = 1_000_000
    fun value(index: Int, period: Int): Int = Math.floorMod(index, period)
    fun initialIndex(value: Int, period: Int): Int = COUNT / 2 - (COUNT / 2 % period) + value
    fun nearestIndex(index: Int, value: Int, period: Int): Int {
        val delta = Math.floorMod(value - value(index, period) + period / 2, period) - period / 2
        return (index + delta).coerceIn(2, COUNT - 3)
    }
}

/** Five visible rows; the third row is snapped to the centre, including during confirmation. */
internal fun LazyListState.centerIndex(): Int {
    val layout = layoutInfo
    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
    return layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        ?: (firstVisibleItemIndex + 2)
}
