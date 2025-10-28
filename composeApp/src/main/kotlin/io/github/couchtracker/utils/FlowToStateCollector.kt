package io.github.couchtracker.utils

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A class to convert a series of [Flow] into [State].
 * The class keeps track of all [Flow]s that have been collected.
 */
class FlowToStateCollector<T>(
    val scope: CoroutineScope,
) {
    private val allStates = mutableListOf<State<T>>()
    val currentValues: List<T> get() = allStates.map { it.value }

    fun <I : T> collectFlow(
        flow: Flow<I>,
        defaultValue: I,
        context: CoroutineContext = Dispatchers.Default,
    ): State<I> {
        val state = mutableStateOf(defaultValue)
        scope.launch(Dispatchers.Main) {
            flow.flowOn(context).collectLatest { item -> state.value = item }
        }
        allStates.add(state)
        return state
    }
}

fun <T, L> FlowToStateCollector<Loadable<L>>.collectFlow(
    flow: Flow<Loadable<T>>,
    context: CoroutineContext = Dispatchers.Default,
): State<Loadable<T>> where T : L {
    return this.collectFlow<Loadable<T>>(
        flow = flow,
        defaultValue = Loadable.Loading,
        context = context,
    )
}
