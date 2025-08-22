package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * Extension of [Result] that allows the value to be [Loading].
 */
sealed interface Loadable<out T> : Actionable<T> {

    data object Loading : Loadable<Nothing>
    data class Loaded<T>(val value: T) : Loadable<T>

    companion object {
        fun <E> error(error: E) = Loaded(Result.Error(error))
        fun <T> value(value: T) = Loaded(Result.Value(value))
    }
}

/**
 * Applies [f] to [Loadable.Loaded.value].
 * Otherwise, [Loadable.Loading] is returned without executing [f].
 */
inline fun <I, O> Loadable<I>.flatMap(f: (I) -> Loadable<O>): Loadable<O> = when (this) {
    is Loadable.Loading -> this
    is Loadable.Loaded -> f(value)
}

/**
 * Applies [f] to [Loadable.Loaded.value].
 * Otherwise, [Loadable.Loading] are returned without executing [f].
 */
inline fun <I, O> Loadable<I>.map(f: (I) -> O): Loadable<O> = flatMap {
    Loadable.Loaded(f(it))
}

/**
 * Returns [Loadable.Loaded.value], or `null` in other cases.
 */
fun <T> Loadable<T>.valueOrNull(): T? = when (this) {
    is Loadable.Loaded -> value
    Loadable.Loading -> null
}

/**
 * Runs [f] when it's loaded.
 */
inline fun <T> Loadable<T>.onValue(f: (T) -> Unit): Loadable<T> {
    if (this is Loadable.Loaded<T>) {
        f(this.value)
    }
    return this
}

@Composable
fun <T> Flow<T>.collectAsLoadableWithLifecycle(): State<Loadable<T>> {
    return remember { map { Loadable.Loaded(it) } }.collectAsStateWithLifecycle(Loadable.Loading)
}

fun <T> Flow<Loadable<T>>.filterLoaded(): Flow<T> {
    return filterIsInstance<Loadable.Loaded<T>>().map { it.value }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun <T> Deferred<T>.awaitAsLoadable(): Loadable<T> {
    var ret: Loadable<T> by remember(this) {
        val initialValue = try {
            Loadable.Loaded(this.getCompleted())
        } catch (_: IllegalStateException) {
            Loadable.Loading
        }
        mutableStateOf(initialValue)
    }
    LaunchedEffect(this) {
        ret = Loadable.Loaded(await())
    }
    return ret
}

@Composable
fun <T> rememberComputationResult(key: Any = Unit, compute: suspend () -> T): Loadable<T> {
    val scope = rememberCoroutineScope()
    return remember(key) {
        scope.async {
            compute()
        }
    }.awaitAsLoadable()
}
