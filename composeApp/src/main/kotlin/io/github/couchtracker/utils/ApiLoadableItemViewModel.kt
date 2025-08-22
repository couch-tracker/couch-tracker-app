package io.github.couchtracker.utils

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import kotlin.coroutines.CoroutineContext

/**
 * A [androidx.lifecycle.ViewModel] specialized for loading data about an item asynchronously.
 */
abstract class ApiLoadableItemViewModel<T>(
    application: Application,
    item: Flow<T>,
) : AndroidViewModel(application), KoinComponent {
    private val retryToken = MutableStateFlow(Any())
    protected val item: Flow<T> = item
        .combine(retryToken) { item, _ -> item }
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    private val allLoadableStates = mutableListOf<State<ApiLoadable<*>>>()
    val allLoadables: List<ApiLoadable<*>> get() = allLoadableStates.map { it.value }

    protected fun <D> loadable(
        flow: Flow<ApiLoadable<D>>,
        context: CoroutineContext = Dispatchers.Default,
    ): State<ApiLoadable<D>> {
        val state = mutableStateOf<ApiLoadable<D>>(Loadable.Loading)
        viewModelScope.launch(Dispatchers.Main) {
            flow.flowOn(context).collect { item -> state.value = item }
        }
        allLoadableStates.add(state)
        return state
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <I, O> Flow<I>.callApi(f: (I) -> Flow<ApiResult<O>>): Flow<ApiLoadable<O>> {
        return transformLatest {
            emit(Loadable.Loading)
            emitAll(f(it).map { Loadable.Loaded(it) })
        }
    }

    /**
     * Returns a flow when a subset of the data can be obtained from a cache.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <Cache : Any, Full : Any> callApiWithCache(
        cachedData: (T) -> Cache?,
        fullDataFlow: (T) -> Flow<ApiResult<Pair<Cache, Full>>>,
        context: CoroutineContext = Dispatchers.Default,
    ): SharedFlow<Pair<ApiLoadable<Cache>, ApiLoadable<Full>>> {
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
            .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    }

    suspend fun reload() {
        retryToken.emit(Any())
    }
}
