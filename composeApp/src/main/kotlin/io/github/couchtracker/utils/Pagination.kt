package io.github.couchtracker.utils

import androidx.paging.CombinedLoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState

val CombinedLoadStates.isLoaded get() = isIdle && !hasError

fun <Key : Any, Value : Any> emptyPager(): Pager<Key, Value> {
    return Pager(
        config = PagingConfig(pageSize = 1),
        initialKey = null,
        pagingSourceFactory = {
            emptyPagingSource()
        },
    )
}

private fun <Key : Any, Value : Any> emptyPagingSource(): PagingSource<Key, Value> {
    return object : PagingSource<Key, Value>() {

        override suspend fun load(params: LoadParams<Key>): LoadResult<Key, Value> {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
                itemsBefore = 0,
                itemsAfter = 0,
            )
        }

        override fun getRefreshKey(state: PagingState<Key, Value>): Key? {
            return null
        }
    }
}
