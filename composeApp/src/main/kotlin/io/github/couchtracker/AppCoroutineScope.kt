package io.github.couchtracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A coroutine scope with the same lifecycle as the whole app.
 * Obtain it using Koin.
 */
class AppCoroutineScope : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.Default + job
}
