package io.github.couchtracker.utils

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Error(val message: String) : Loadable<Nothing>
    data class Loaded<T>(val value: T) : Loadable<T>
}

/**
 * Applies [f] to [Loadable.Loaded.value].
 * Otherwise, [Loadable.Loading] and [Loadable.Error] are returned without executing [f].
 */
inline fun <I, O> Loadable<I>.flatMap(f: (I) -> Loadable<O>): Loadable<O> = when (this) {
    is Loadable.Error -> this
    is Loadable.Loading -> this
    is Loadable.Loaded -> f(value)
}

/**
 * Applies [f] to [Loadable.Loaded.value].
 * Otherwise, [Loadable.Loading] and [Loadable.Error] are returned without executing [f].
 */
inline fun <I, O> Loadable<I>.map(f: (I) -> O): Loadable<O> = flatMap {
    Loadable.Loaded(f(it))
}
