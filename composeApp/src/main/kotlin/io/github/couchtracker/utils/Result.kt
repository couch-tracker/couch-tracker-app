package io.github.couchtracker.utils

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
