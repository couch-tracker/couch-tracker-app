package io.github.couchtracker.utils

import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val INJECT_ERRORS = false

private const val MAX_DELAY_MS = 10_000
private const val ERROR_PROBABILITY = 0.2f
private const val CACHE_MISS_CHANCE = 0.5f

/**
 * @throws ApiException
 */
suspend fun injectApiError() {
    if (INJECT_ERRORS) {
        delay(Random.nextInt(MAX_DELAY_MS).milliseconds)
        if (Random.nextFloat() <= ERROR_PROBABILITY) {
            throw ApiException.SimulatedError()
        }
    }
}

fun <T : Any> T?.injectCacheMiss(): T? {
    if (INJECT_ERRORS) {
        if (Random.nextFloat() <= CACHE_MISS_CHANCE) {
            return null
        }
    }
    return this
}
