package io.github.couchtracker.utils

sealed interface Loadable<out T, out E> : Actionable<T, E> {

    /**
     * Specialization of [Loadable] that doesn't allow the [Loading] state.
     * This is equivalent of either [Loaded] or [Error].
     */
    sealed interface Completed<out T, out E> : Loadable<T, E>

    data object Loading : Loadable<Nothing, Nothing>
    data class Error<E>(val error: E) : Completed<Nothing, E>
    data class Loaded<T>(val value: T) : Completed<T, Nothing>
}

/**
 * Applies [f] to [Loadable.Loaded.value].
 * Otherwise, [Loadable.Loading] and [Loadable.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<I, E>.flatMap(f: (I) -> Loadable<O, E>): Loadable<O, E> = when (this) {
    is Loadable.Error -> this
    is Loadable.Loading -> this
    is Loadable.Loaded -> f(value)
}

/**
 * Applies [f] to [Loadable.Loaded.value].
 * Otherwise, [Loadable.Loading] and [Loadable.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<I, E>.map(f: (I) -> O): Loadable<O, E> = flatMap {
    Loadable.Loaded(f(it))
}
