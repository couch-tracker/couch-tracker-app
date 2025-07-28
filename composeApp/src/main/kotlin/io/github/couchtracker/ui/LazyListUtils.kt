package io.github.couchtracker.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

interface SizeAwareLazyListScope : LazyListScope {
    val itemCount: Int
}

/**
 * Adds the given [content] to the LazyList using a [SizeAwareLazyListScope].
 */
fun LazyListScope.countingElements(content: SizeAwareLazyListScope.() -> Unit): Int {
    val original = this
    val wrappedScope = object : SizeAwareLazyListScope {
        override var itemCount: Int = 0
        override fun item(
            key: Any?,
            contentType: Any?,
            content: @Composable (LazyItemScope.() -> Unit),
        ) {
            itemCount += 1
            original.item(key, contentType, content)
        }

        override fun item(
            key: Any?,
            content: @Composable (LazyItemScope.() -> Unit),
        ) {
            itemCount += 1
            original.item(key, content = content)
        }

        override fun items(
            count: Int,
            key: ((Int) -> Any)?,
            contentType: (Int) -> Any?,
            itemContent: @Composable (LazyItemScope.(Int) -> Unit),
        ) {
            itemCount += count
            original.items(count, key, contentType, itemContent)
        }

        override fun items(
            count: Int,
            key: ((Int) -> Any)?,
            itemContent: @Composable (LazyItemScope.(Int) -> Unit),
        ) {
            itemCount += count
            original.items(count, key, itemContent = itemContent)
        }

        override fun stickyHeader(
            key: Any?,
            contentType: Any?,
            content: @Composable (LazyItemScope.(Int) -> Unit),
        ) {
            itemCount += 1
            original.stickyHeader(key, contentType, content)
        }
    }
    wrappedScope.content()
    return wrappedScope.itemCount
}
