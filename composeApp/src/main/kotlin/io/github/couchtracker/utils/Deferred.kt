package io.github.couchtracker.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

fun <T> Collection<Deferred<T>>.emitAsFlow(): Flow<T> {
    val deferred = this
    return channelFlow {
        for (value in deferred) {
            launch { send(value.await()) }
        }
    }
}
