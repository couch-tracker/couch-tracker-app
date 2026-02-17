package io.github.couchtracker.utils.api

import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.flatMapLoading
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest

class ApiContext<T>(
    private val retryToken: FlowRetryToken,
    item: Flow<T>,
) {
    private val retryableFlow = retryToken.makeRetriable(item)

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun <O> invoke(downloadFn: (T) -> Flow<ApiResult<O>>): Flow<ApiLoadable<O>> {
        return retryableFlow.transformLatest { item ->
            emit(Loadable.Loading)
            emitAll(downloadFn(item).map { Loadable.Loaded(it) })
        }
    }

    /**
     * Returns a flow when a _subset_ of the data can be obtained from a cache:
     *  - [Full] is the full data to load via an API
     *  - [Cache] is a subset of [Full], that can be obtained immediately (when available)
     *  Note: [Cache] cannot substitute [Full], so [downloadFn] is called even when the cached value is available.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <Cache, Full> withCache(
        cacheFn: (T) -> Cache?,
        downloadFn: (T) -> Flow<ApiResult<Pair<Cache, Full>>>,
    ): Flow<Pair<ApiLoadable<Cache>, ApiLoadable<Full>>> {
        return retryableFlow.transformLatest { item ->
            val cachedData: ApiLoadable<Cache> = cacheFn(item)?.let { Loadable.value(it) } ?: Loadable.Loading
            emit(cachedData to Loadable.Loading)
            emitAll(
                downloadFn(item).mapLatest { result ->
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
    }

    suspend fun retryAll() {
        retryToken.retryAll()
    }
}
