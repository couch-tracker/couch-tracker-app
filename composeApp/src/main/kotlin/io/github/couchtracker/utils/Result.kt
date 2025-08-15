package io.github.couchtracker.utils

/**
 * Represents a value that can be in error.
 */
sealed interface Result<out T, out E> {

    data class Value<T>(val value: T) : Result<T, Nothing>

    data class Error<E>(val error: E) : Result<Nothing, E>
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Result.Error] is returned without executing [f].
 */
inline fun <I, O, E> Result<I, E>.flatMap(f: (I) -> Result<O, E>): Result<O, E> = when (this) {
    is Result.Error -> this
    is Result.Value -> f(value)
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Result.Error] is returned without executing [f].
 */
inline fun <I, O, E> Result<I, E>.map(f: (I) -> O): Result<O, E> = flatMap {
    Result.Value(f(it))
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<Result<I, E>>.mapResult(f: (I) -> O): Loadable<Result<O, E>> = map { it.map(f) }

/**
 * Returns [Result.Value.value], or `null` in other cases.
 */
fun <T> Result<T, *>.valueOrNull(): T? {
    return when (this) {
        is Result.Value -> this.value
        is Result.Error -> null
    }
}

/**
 * Returns [Result.Error.error], or `null` in other cases.
 */
fun <E> Result<*, E>.errorOrNull(): E? {
    return when (this) {
        is Result.Value -> null
        is Result.Error -> this.error
    }
}

/**
 * Returns [Result.Value.value], or `null` in other cases.
 */
fun <T> Loadable<Result<T, *>>.resultValueOrNull(): T? {
    return valueOrNull()?.valueOrNull()
}

/**
 * Returns [Result.Error.error], or `null` in other cases.
 */
fun <E> Loadable<Result<*, E>>.resultErrorOrNull(): E? {
    return valueOrNull()?.errorOrNull()
}

/**
 * Runs [f] when the result is a value.
 */
inline fun <T, E> Result<T, E>.onValue(f: (T) -> Unit): Result<T, E> {
    if (this is Result.Value<T>) {
        f(this.value)
    }
    return this
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

inline fun <T : R, E, R> Result<T, E>.ifError(block: (E) -> R): R {
    return when (this) {
        is Result.Value -> value
        is Result.Error -> block(this.error)
    }
}
