package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Represents a value that can be in error.
 */
sealed interface Result<out T, out E> : Loadable<T, E> {

    data class Value<T>(val value: T) : Result<T, Nothing>

    data class Error<E>(val error: E) : Result<Nothing, E>
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Result<I, E>.flatMap(f: (I) -> Result<O, E>): Result<O, E> = when (this) {
    is Result.Error -> this
    is Result.Value -> f(value)
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Result<I, E>.map(f: (I) -> O): Result<O, E> = flatMap {
    Result.Value(f(it))
}

/**
 * Calls [block] whenever [this] is not a [Result.Value].
 *
 * Being inline, allows idiomatic code such as:
 * ```kotlin
 * value.onError { return it }
 * ```
 */
inline fun <T, E> Result<T, E>.onError(block: (Result.Error<E>) -> Unit) {
    when (this) {
        is Result.Value -> {}
        is Result.Error -> block(this)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun <T, E> Deferred<Result<T, E>>.awaitAsLoadable(): Loadable<T, E> {
    var ret: Loadable<T, E> by remember {
        val initialValue = try {
            this.getCompleted()
        } catch (_: IllegalStateException) {
            Loadable.Loading
        }
        mutableStateOf(initialValue)
    }
    LaunchedEffect(this) {
        ret = await()
    }
    return ret
}
