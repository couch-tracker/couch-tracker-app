package io.github.couchtracker.utils.api

import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.flatMapLoading
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapError
import io.github.couchtracker.utils.withLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlin.coroutines.CoroutineContext

/**
 * Returns a flow when a _subset_ of the data can be obtained from a cache:
 *  - [Full] is the full data to load via an API
 *  - [Cache] is a subset of [Full], that can be obtained immediately (when available)
 *  Note: [Cache] cannot substitute [Full], so [fullDataFlow] is called even when the cached value is available.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <I, Cache : Any, Full : Any> Flow<I>.callApiWithCache(
    retryToken: FlowRetryToken,
    cachedData: (I) -> Cache?,
    fullDataFlow: (I) -> Flow<ApiResult<Pair<Cache, Full>>>,
    context: CoroutineContext = Dispatchers.Default,
): Flow<Pair<ApiLoadable<Cache>, ApiLoadable<Full>>> {
    return makeRetriable(retryToken).transformLatest { item ->
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

@OptIn(ExperimentalCoroutinesApi::class)
fun <I, O> Flow<I>.callApi(retryToken: FlowRetryToken, f: (I) -> Flow<ApiResult<O>>): Flow<ApiLoadable<O>> {
    return makeRetriable(retryToken).flatMapLatest { f(it).withLoading() }
}
