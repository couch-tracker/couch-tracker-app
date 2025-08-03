package io.github.couchtracker.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

/**
 * Awaits for the completion of all given referred, up to [timeout].
 */
suspend fun List<Deferred<*>>.awaitAll(timeout: Duration) {
    try {
        withTimeout(timeout) {
            awaitAll()
        }
    } catch (_: TimeoutCancellationException) {
    }
}
