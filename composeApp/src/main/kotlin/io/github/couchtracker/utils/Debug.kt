package io.github.couchtracker.utils

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

private const val INJECT_ERRORS = false

private const val MAX_DELAY_MS = 3_000
private const val ERROR_PROBABILITY = 0.25f
private const val CACHE_MISS_CHANCE = 0.25f

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

inline fun <T> logExecutionTime(logTag: String, message: String, f: () -> T): T {
    val (value, took) = measureTimedValue {
        f()
    }
    Log.d(logTag, "$message took $took")
    return value
}
