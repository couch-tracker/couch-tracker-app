package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

fun <T> Loadable<T, *>.valueOrNull(): T? {
    return when (this) {
        is Result.Value -> value
        is Result.Error -> null
        Loadable.Loading -> null
    }
}

/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<I, E>.map(f: (I) -> O): Loadable<O, E> = flatMap {
    Result.Value(f(it))
}

@Composable
fun <T> Flow<T>.collectAsLoadableWithLifecycle(): State<Loadable<T, Nothing>> {
    return remember { map { Result.Value(it) } }.collectAsStateWithLifecycle(Loadable.Loading)
}
