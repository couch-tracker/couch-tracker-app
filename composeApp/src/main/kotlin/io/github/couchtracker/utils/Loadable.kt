package io.github.couchtracker.utils

/**
 * Extension of [Result] that allows the value to be [Loading].
 */
sealed interface Loadable<out T, out E> : Actionable<T, E> {

    data object Loading : Loadable<Nothing, Nothing>
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<I, E>.flatMap(f: (I) -> Loadable<O, E>): Loadable<O, E> = when (this) {
    is Loadable.Loading -> this
    is Result.Error -> this
    is Result.Value -> f(value)
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<I, E>.map(f: (I) -> O): Loadable<O, E> = flatMap {
    Result.Value(f(it))
}
