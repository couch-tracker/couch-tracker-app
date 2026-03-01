package io.github.couchtracker.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable

data class ItemPosition(
    val index: Int,
    val size: Int,
    val columns: Int = 1,
) {
    val first get() = index == 0
    val last get() = index == size - 1
    val rowIndex = index / columns
    val columnIndex = index.mod(columns)

    init {
        require(index in 0..<size) { "index must be contained in 0..<size" }
        require(columns > 0)
    }

    fun isInTopStartCorner() = first
    fun isInTopEndCorner() = rowIndex == 0 && (columnIndex == columns - 1 || last)
    fun isInBottomStartCorner(): Boolean {
        val lastRowIndex = (size - 1) / columns
        return rowIndex == lastRowIndex && columnIndex == 0
    }

    fun isInBottomEndCorner(): Boolean {
        // Two cases:
        //  - I'm the last element
        //  - The grid is not a square, and I'm the rightmost element in the second-to-last row
        val lastRowIndex = (size - 1) / columns
        return last ||
            (size.mod(columns) > 0 && rowIndex == lastRowIndex - 1 && columnIndex == columns - 1)
    }
}

inline fun <T> LazyListScope.itemsWithPosition(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(position: ItemPosition, item: T) -> Unit,
) {
    itemsIndexed(items = items, key = key, contentType = contentType) { index, item ->
        val position = ItemPosition(index, items.size)
        itemContent(position, item)
    }
}

inline fun <T> LazyGridScope.itemsWithPosition(
    items: List<T>,
    columns: Int,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(position: ItemPosition, item: T) -> Unit,
) {
    itemsIndexed(items = items, key = key, span = span, contentType = contentType) { index, item ->
        val position = ItemPosition(index, items.size, columns = columns)
        itemContent(position, item)
    }
}
