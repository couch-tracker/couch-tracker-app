package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Extension of [Result] that allows the value to be [Loading].
 */
sealed interface Loadable<out T, out E> : Actionable<T, E> {

    sealed interface NoError<out T> : Loadable<T, Nothing>

    data object Loading : NoError<Nothing>
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
inline fun <I, O> Loadable.NoError<I>.flatMap(f: (I) -> Loadable.NoError<O>): Loadable.NoError<O> = when (this) {
    is Loadable.Loading -> this
    is Result.Value -> f(value)
}

fun <T> Loadable<T, *>.valueOrNull(): T? {
    return when (this) {
        is Result.Value -> value
        is Result.Error -> null
        Loadable.Loading -> null
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Loadable<T, *>.isLoadingOr(matchValue: (T) -> Boolean): Boolean {
    contract {
        returns(true) implies (this@isLoadingOr is Loadable.NoError)
    }
    return when (this) {
        is Result.Value -> matchValue(value)
        is Result.Error -> false
        Loadable.Loading -> true
    }
}

inline fun <T : R, R, E> Loadable<T, E>.ifError(block: (E) -> R): Loadable.NoError<R> {
    return when (this) {
        Loadable.Loading -> this as Loadable.NoError
        is Result.Value -> this
        is Result.Error -> Result.Value(block(this.error))
    }
}

inline fun <T : R, R> Loadable.NoError<T>.ifLoading(onLoading: () -> R): R {
    return when (this) {
        is Result.Value -> value
        Loadable.Loading -> onLoading()
    }
}

inline fun <I, O> Loadable.NoError<I>.map(f: (I) -> O): Loadable.NoError<O> = flatMap {
    Result.Value(f(it))
}


/**
 * Applies [f] to [Result.Value.value].
 * Otherwise, [Loadable.Loading] and [Result.Error] are returned without executing [f].
 */
inline fun <I, O, E> Loadable<I, E>.map(f: (I) -> O): Loadable<O, E> = flatMap {
    Result.Value(f(it))
}

@Composable
fun <T> Flow<T>.collectAsLoadableWithLifecycle(): State<Loadable.NoError<T>> {
    return remember { map { Result.Value(it) } }.collectAsStateWithLifecycle(Loadable.Loading)
}
