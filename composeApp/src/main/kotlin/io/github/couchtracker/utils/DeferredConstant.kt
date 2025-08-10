package io.github.couchtracker.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private class DeferredConstant<T>(val initialize: suspend () -> T) {

    val completable: CompletableDeferred<T> = CompletableDeferred()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            completable.complete(initialize())
        }
    }
}

/** Computes a constant on the background thread */
fun <T> deferredConstant(initialize: suspend () -> T): Deferred<T> {
    val initializer = DeferredConstant(initialize)
    return initializer.completable
}
