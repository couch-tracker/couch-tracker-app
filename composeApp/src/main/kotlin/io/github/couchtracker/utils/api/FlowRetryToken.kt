package io.github.couchtracker.utils.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class FlowRetryToken {
    private val retryToken = MutableStateFlow(Any())

    fun <T> makeRetriable(flow: Flow<T>): Flow<T> {
        return flow.combine(retryToken) { item, _ -> item }
    }

    /** Retries the download for all [Flow] where this retry token was used */
    suspend fun retryAll() {
        retryToken.emit(Any())
    }
}

fun <T> Flow<T>.makeRetriable(token: FlowRetryToken): Flow<T> {
    return token.makeRetriable(this)
}
