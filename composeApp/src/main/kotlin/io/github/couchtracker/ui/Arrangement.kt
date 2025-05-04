package io.github.couchtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Horizontal
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

// Similar to Arrangement.Start, but places the last item at the end
private val ArrangementStartSpaceLast = object : Horizontal {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        val consumedSize = sizes.sum()
        val remainingSpace = (totalSize - consumedSize).coerceAtLeast(0)
        val reverse = layoutDirection == LayoutDirection.Rtl
        var current = if (reverse) totalSize - consumedSize else 0
        sizes.forEachIndexed(reverse) { index, size ->
            if (index == sizes.lastIndex) {
                outPositions[index] = current + remainingSpace
            } else {
                outPositions[index] = current
            }
            current += size
        }
    }

    override fun toString() = "Arrangement#StartSpaceLast"
}

/**
 * Place children horizontally such that they are as close as possible to the beginning of the
 * horizontal axis (left if the layout direction is LTR, right otherwise).
 *
 * The last element is placed at the very end.
 *
 * @see Arrangement.Start
 */
val Arrangement.StartSpaceLast get() = ArrangementStartSpaceLast

private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (index: Int, size: Int) -> Unit) {
    if (!reversed) {
        forEachIndexed(action)
    } else {
        for (i in (size - 1) downTo 0) {
            action(i, get(i))
        }
    }
}
