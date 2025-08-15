package io.github.couchtracker.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.TimeSource

suspend fun Collection<Deferred<*>>.awaitWithTimeout(startingPoint: TimeSource.Monotonic.ValueTimeMark, timeout: Duration) {
    val elapsed = TimeSource.Monotonic.markNow() - startingPoint
    val remaining = timeout - elapsed
    withTimeoutOrNull(remaining) {
        awaitAll()
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
