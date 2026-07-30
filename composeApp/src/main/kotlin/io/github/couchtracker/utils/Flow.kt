package io.github.couchtracker.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

private sealed interface RememberingCombinedEvent<I : Any, K, C> {
    data class OnNewItem<I : Any, K, C>(val item: I) : RememberingCombinedEvent<I, K, C>
    data class OnNewCachedValue<I : Any, K, C>(val key: K, val cachedValue: C) : RememberingCombinedEvent<I, K, C>
}

/**
 * Transforms each input by combining it with the latest values from a dynamic set of
 * remembered keyed flows.
 *
 * Flows are reused while their key remains present and cancelled when removed.
 * [f] is called once every active flow has emitted at least one value.
 *
 * Note: the map passed to [f] must not be retained.
 */
fun <I : Any, K, C, O> Flow<I>.rememberingCombined(
    keys: (I) -> Set<K>,
    flowToRemember: (K) -> Flow<C>,
    f: (I, Map<K, C>) -> O,
): Flow<O> = channelFlow {
    val jobs = mutableMapOf<K, Job>()
    val events = Channel<RememberingCombinedEvent<I, K, C>>()

    launch {
        collectLatest { item ->
            events.send(RememberingCombinedEvent.OnNewItem(item))
        }
    }

    var latestItem: I? = null
    var keysCount = 0
    val latestValues = mutableMapOf<K, C>()

    fun processEvent(event: RememberingCombinedEvent<I, K, C>) {
        when (event) {
            is RememberingCombinedEvent.OnNewItem -> {
                latestItem = event.item
                val keys = keys(event.item)
                keysCount = keys.size

                // Cancel removed keys
                val keysIter = jobs.iterator()
                while (keysIter.hasNext()) {
                    val (key, job) = keysIter.next()
                    if (key !in keys) {
                        keysIter.remove()
                        job.cancel()
                        latestValues.remove(key)
                    }
                }

                // Start new keys
                keys.forEach { key ->
                    if (key !in jobs) {
                        jobs[key] = launch {
                            flowToRemember(key).collect { value ->
                                events.send(RememberingCombinedEvent.OnNewCachedValue(key, value))
                            }
                        }
                    }
                }

                check(jobs.size == keysCount)
                check(latestValues.size <= keysCount)
            }
            is RememberingCombinedEvent.OnNewCachedValue -> {
                latestValues[event.key] = event.cachedValue
            }
        }
    }

    while (true) {
        processEvent(events.receive())
        do {
            val event = events.tryReceive()
            if (event.isSuccess) {
                processEvent(event.getOrThrow())
            }
        } while (event.isSuccess)

        if (latestItem != null && latestValues.size == keysCount) {
            send(f(latestItem, latestValues))
        }
    }
}
