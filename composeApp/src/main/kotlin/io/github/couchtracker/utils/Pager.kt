package io.github.couchtracker.utils

import androidx.paging.PagingData
import androidx.paging.filter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> Flow<PagingData<T>>.removeDuplicates(key: (T) -> Any): Flow<PagingData<T>> {
    return this.mapLatest { pagingData ->
        val items = mutableSetOf<Any>()
        pagingData.filter { item ->
            items.add(key(item))
        }
    }
}
