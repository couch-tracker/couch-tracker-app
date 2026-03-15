package io.github.couchtracker.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest

class FlowRetryContext<T>(
    private val retryToken: FlowRetryToken,
    item: Flow<T>,
) {
    private val retryableFlow = retryToken.makeRetriable(item)

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun <O> invoke(downloadFn: (T) -> Flow<O>): Flow<Loadable<O>> {
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
    fun <Cache, Full, E> withCache(
        cacheFn: (T) -> Cache?,
        downloadFn: (T) -> Flow<Result<Pair<Cache, Full>, E>>,
    ): Flow<Pair<Loadable<Result<Cache, E>>, Loadable<Result<Full, E>>>> {
        return retryableFlow.transformLatest { item ->
            val cachedData: Loadable<Result<Cache, E>> = cacheFn(item)?.let { Loadable.value(it) } ?: Loadable.Loading
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
