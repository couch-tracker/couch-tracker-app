package io.github.couchtracker.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T, R> Flow<T>.collectWithPrevious(operation: suspend (previous: R?, value: T) -> R): Flow<R> = flow {
    var accumulator: R? = null
    collect { value ->
        accumulator = operation(accumulator, value)
        emit(accumulator)
    }
}
