package io.github.couchtracker.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable

data class ListItemPosition(
    val index: Int,
    val size: Int,
) {
    val first get() = index == 0
    val last get() = index == size - 1

    init {
        require(index in 0..<size) { "index must be contained in 0..<size" }
    }
}

inline fun <T> LazyListScope.itemsWithPosition(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyItemScope.(position: ListItemPosition, item: T) -> Unit,
) {
    itemsIndexed(items = items, key = key, contentType = contentType) { index, item ->
        val position = ListItemPosition(index, items.size)
        itemContent(position, item)
    }
}
