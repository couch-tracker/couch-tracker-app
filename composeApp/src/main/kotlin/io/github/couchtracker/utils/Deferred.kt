package io.github.couchtracker.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * Awaits for the completion of all given referred, up to [timeout].
 */
suspend fun List<Deferred<*>>.awaitAll(timeout: Duration) {
    try {
        withTimeout(timeout) {
            awaitAll()
        }
    } catch (_: TimeoutCancellationException) {
    }
}

fun <T> Collection<Deferred<T>>.emitAsFlow(): Flow<T> {
    val deferred = this
    return channelFlow {
        for (value in deferred) {
            launch { send(value.await()) }
        }
    }
}
