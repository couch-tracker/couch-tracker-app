package io.github.couchtracker.tmdb

import android.util.Log
import app.cash.sqldelight.Query
import app.moviebase.tmdb.Tmdb3
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.ifError
import io.github.couchtracker.utils.injectApiError
import io.github.couchtracker.utils.injectCacheMiss
import io.github.couchtracker.utils.runApiCatching
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.koin.mp.KoinPlatform
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private const val LOG_TAG = "TmdbDownload"

val TMDB_CACHE_EXPIRATION_FAST = 1.hours
val TMDB_CACHE_EXPIRATION_DEFAULT = 1.days
const val TMDB_CACHE_PREFETCH_THRESHOLD = 0.5

/**
 * Utility function to handle cached downloads towards TMDB.
 * Handles caching, stale caching and prefetching.
 */
suspend fun <T : Any> tmdbGetOrDownload(
    entryTag: String,
    get: () -> Query<TmdbTimestampedEntry<T>>,
    put: (TmdbTimestampedEntry<T>) -> Unit,
    downloader: suspend () -> ApiResult<T>,
    backgroundDownloader: () -> Unit,
    expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    prefetch: Duration = expiration * TMDB_CACHE_PREFETCH_THRESHOLD,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): ApiResult<T> {
    require(prefetch <= expiration)
    val cachedValue = withContext(coroutineContext) {
        get().executeAsOneOrNull()
    }.injectCacheMiss()
    val currentTime = Clock.System.now()
    return if (cachedValue != null) {
        val age = currentTime - cachedValue.lastUpdate
        val result = if (age > expiration) {
            Log.d(LOG_TAG, "$entryTag: Entry is expired (age=$age), performing download")
            downloadAndSave(downloader, put, coroutineContext).ifError {
                Log.w(LOG_TAG, "$entryTag: Download failed, returning a stale entry", it)
                cachedValue.value
            }
        } else if (age > prefetch) {
            Log.d(LOG_TAG, "$entryTag: Entry is about to expire (age=$age), starting background download")
            backgroundDownloader()
            cachedValue.value
        } else {
            Log.d(LOG_TAG, "$entryTag: Entry is fresh (age=$age)")
            cachedValue.value
        }
        Result.Value(result)
    } else {
        Log.d(LOG_TAG, "$entryTag: No cached entry, performing download")
        downloadAndSave(downloader, put, coroutineContext)
    }
}

private suspend fun <T : Any> downloadAndSave(
    download: suspend () -> ApiResult<T>,
    save: (TmdbTimestampedEntry<T>) -> Unit,
    coroutineContext: CoroutineContext,
): ApiResult<T> {
    val currentTime = Clock.System.now()
    val element = download()
    if (element is Result.Value) {
        withContext(coroutineContext) {
            save(TmdbTimestampedEntry(element.value, currentTime))
        }
    }
    return element
}

@Suppress("ThrowsCount")
suspend fun <T> tmdbDownloadResult(
    logTag: String?,
    download: suspend (Tmdb3) -> T,
): ApiResult<T> {
    return runApiCatching(logTag) {
        try {
            injectApiError()
            val client = KoinPlatform.getKoin().get<Tmdb3>()
            // preparing the API call is often CPU intensive
            withContext(Dispatchers.Default) {
                download(client)
            }
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
}
