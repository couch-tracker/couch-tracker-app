package io.github.couchtracker.utils

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.remember
import io.github.couchtracker.BuildConfig
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.utils.error.SimulatedException
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

private const val INJECT_ERRORS = false
private const val INJECT_BROKEN_ITEMS = false

private const val MAX_DELAY_MS = 3_000
private const val ERROR_PROBABILITY = 0.25f
private const val CACHE_MISS_CHANCE = 0.25f

/**
 * @throws SimulatedException
 */
suspend fun injectApiError() {
    if (INJECT_ERRORS) {
        delay(Random.nextInt(MAX_DELAY_MS).milliseconds)
        if (Random.nextFloat() <= ERROR_PROBABILITY) {
            throw SimulatedException()
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

@Suppress("MagicNumber")
fun Collection<ExternalShowId>.injectBrokenItems(): Collection<ExternalShowId> {
    return if (INJECT_BROKEN_ITEMS) {
        this + listOf(
            TmdbExternalShowId(TmdbShowId(546_544)),
            UnknownExternalShowId("xyz", "123456"),
        )
    } else {
        this
    }
}

@PublishedApi
internal class RecompositionCounter(var value: Int)

/** Emits a log whenever the calling function is being recomposed (only in debug) */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun logCompositions(tag: String, msg: String) {
    if (BuildConfig.DEBUG) {
        val recompositionCounter = remember { RecompositionCounter(0) }
        Log.d(tag, "$msg ${recompositionCounter.value} $currentRecomposeScope")
        recompositionCounter.value++
    }
}
