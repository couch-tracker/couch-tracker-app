package io.github.couchtracker.tmdb

import android.util.Log
import app.cash.sqldelight.Query
import app.moviebase.tmdb.Tmdb3
import io.github.couchtracker.AndroidApplication
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private const val LOG_TAG = "TmdbDownload"

private val tmdb = Tmdb3 {
    tmdbApiKey = TmdbConfig.API_KEY
    useTimeout = true
    maxRetriesOnException = 3
}

val TMDB_CACHE_EXPIRATION_FAST = 1.hours
val TMDB_CACHE_EXPIRATION_DEFAULT = 1.days
const val TMDB_CACHE_PREFETCH_THRESHOLD = 0.5

/**
 * Utility function to handle cached downloads towards TMDB.
 *
 * TODO: multiple calls on the same API should be batched, so the download is performed only once
 */
suspend fun <T : Any> tmdbGetOrDownload(
    entryTag: String,
    get: () -> Query<TmdbTimestampedEntry<T>>,
    put: (TmdbTimestampedEntry<T>) -> Unit,
    downloader: suspend (Tmdb3) -> T,
    expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    prefetch: Duration = expiration * TMDB_CACHE_PREFETCH_THRESHOLD,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): T {
    require(prefetch <= expiration)
    val cachedValue = withContext(coroutineContext) {
        get().executeAsOneOrNull()
    }
    val currentTime = Clock.System.now()
    return if (cachedValue != null) {
        val age = currentTime - cachedValue.lastUpdate
        if (age > expiration) {
            Log.d(LOG_TAG, "$entryTag: Entry is expired (age=$age), performing download")
            try {
                downloadAndSave(downloader, put, coroutineContext)
            } catch (ignored: ApiException) {
                Log.w(LOG_TAG, "$entryTag: Download failed, returning a stale entry", ignored)
                cachedValue.value
            }
        } else if (age > prefetch) {
            Log.d(LOG_TAG, "$entryTag: Entry is about to expire (age=$age), starting background download")
            AndroidApplication.scope.launch {
                try {
                    downloadAndSave(downloader, put, coroutineContext)
                } catch (ignored: ApiException) {
                    Log.w(LOG_TAG, "$entryTag: error while prefetching will be ignored", ignored)
                }
            }
            cachedValue.value
        } else {
            Log.d(LOG_TAG, "$entryTag: Entry is fresh (age=$age)")
            cachedValue.value
        }
    } else {
        Log.d(LOG_TAG, "$entryTag: No cached entry, performing download")
        downloadAndSave(downloader, put, coroutineContext)
    }
}

private suspend fun <T : Any> downloadAndSave(
    download: suspend (Tmdb3) -> T,
    save: (TmdbTimestampedEntry<T>) -> Unit,
    coroutineContext: CoroutineContext,
): T {
    val currentTime = Clock.System.now()
    val element = tmdbDownload(download)
    withContext(coroutineContext) {
        save(TmdbTimestampedEntry(element, currentTime))
    }
    return element
}

/**
 * @throws ApiException
 */
@Suppress("ThrowsCount")
suspend fun <T> tmdbDownload(
    download: suspend (Tmdb3) -> T,
): T {
    try {
        return download(tmdb)
    } catch (e: ClientRequestException) {
        throw ApiException.ClientError(e.message, e)
    } catch (e: ResponseException) {
        throw ApiException.ServerError(e.message, e)
    } catch (e: IOException) {
        throw ApiException.IOError(e.message, e)
    } catch (e: SerializationException) {
        throw ApiException.DeserializationError(e.message, e)
    }
}
