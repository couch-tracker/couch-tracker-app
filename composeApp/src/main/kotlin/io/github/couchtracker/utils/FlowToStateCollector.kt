package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
    private val childCollectors = mutableStateListOf<FlowToStateCollector<out T>>()
    val currentValues: List<T> get() = allStates.map { it.value } + childCollectors.flatMap { it.currentValues }

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

    @Composable
    fun childCollector(): FlowToStateCollector<T> {
        val scope = rememberCoroutineScope()
        val childCollector = FlowToStateCollector<T>(scope)
        DisposableEffect(scope) {
            childCollectors.add(childCollector)
            onDispose {
                childCollectors.remove(childCollector)
            }
        }
        return childCollector
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
