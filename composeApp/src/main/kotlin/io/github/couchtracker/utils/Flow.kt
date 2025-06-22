package io.github.couchtracker.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

fun <T, R> Flow<T>.collectWithPrevious(operation: suspend (previous: R?, value: T) -> R): Flow<R> = flow {
    var accumulator: R? = null
    collect { value ->
        accumulator = operation(accumulator, value)
        emit(accumulator)
    }
}

/**
 * Splits the input flow into two "forked" flow, using [fork1] and [fork2].
 * The two forks are then re-combined using values emitted by [merge].
 *
 * As a design decision, only the latest value of all involved Flows is used.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <I : Any, P1, P2, O> Flow<I>.biFork(
    fork1: (Flow<I>) -> Flow<P1>,
    fork2: (Flow<I>) -> Flow<P2>,
    merge: suspend FlowCollector<O>.(P1, P2) -> Unit,
): Flow<O> = channelFlow {
    val channel1 = Channel<I>(capacity = Channel.CONFLATED)
    val channel2 = Channel<I>(capacity = Channel.CONFLATED)
    val forkedFlow1 = fork1(channel1.consumeAsFlow())
    val forkedFlow2 = fork2(channel2.consumeAsFlow())
    launch {
        collect { value ->
            channel1.send(value)
            channel2.send(value)
        }
        channel1.close()
        channel2.close()
    }
    forkedFlow1
        .combineTransform(forkedFlow2, merge)
        .collectLatest { send(it) }
}
