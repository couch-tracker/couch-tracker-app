package io.github.couchtracker.utils.api

import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.flatMapLoading
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapError
import io.github.couchtracker.utils.withLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlin.coroutines.CoroutineContext

/**
 * A utility class to perform API calls where [T] is the input.
 */
class ApiCallHelper<T>(
    scope: CoroutineScope,
    item: Flow<T>,
) {
    private val retryToken = MutableStateFlow(Any())
    private val item: Flow<T> = item
        .combine(retryToken) { item, _ -> item }
        .shareIn(scope, SharingStarted.Eagerly, 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <O> callApi(f: (T) -> Flow<ApiResult<O>>): Flow<ApiLoadable<O>> {
        return item.flatMapLatest { f(it).withLoading() }
    }

    /**
     * Returns a flow when a _subset_ of the data can be obtained from a cache:
     *  - [Full] is the full data to load via an API
     *  - [Cache] is a subset of [Full], that can be obtained immediately (when available)
     *  Note: [Cache] cannot substitute [Full], so [fullDataFlow] is called even when the cached value is available.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <Cache : Any, Full : Any> callApiWithCache(
        cachedData: (T) -> Cache?,
        fullDataFlow: (T) -> Flow<ApiResult<Pair<Cache, Full>>>,
        context: CoroutineContext = Dispatchers.Default,
    ): Flow<Pair<ApiLoadable<Cache>, ApiLoadable<Full>>> {
        return item
            .transformLatest { item ->
                val cachedData: ApiLoadable<Cache> = cachedData(item)?.let { Loadable.value(it) } ?: Loadable.Loading
                emit(cachedData to Loadable.Loading)
                emitAll(
                    fullDataFlow(item).mapLatest { result ->
                        result
                            .map { (cached, full) ->
                                Loadable.value(cached) to Loadable.value(full)
                            }
                            .mapError { error ->
                                cachedData.flatMapLoading { Loadable.error(error) } to Loadable.error(error)
                            }
                    },
                )
            }
            .flowOn(context)
    }

    /** Retries the download for all [Flow] created by this class */
    suspend fun retryAll() {
        retryToken.emit(Any())
    }
}
