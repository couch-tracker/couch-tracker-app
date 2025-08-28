package io.github.couchtracker.tmdb

import android.util.Log
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.moviebase.tmdb.Tmdb3
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.injectApiError
import io.github.couchtracker.utils.injectCacheMiss
import io.github.couchtracker.utils.runApiCatching
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.koin.mp.KoinPlatform
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val LOG_TAG = "TmdbDownload"

val TMDB_CACHE_EXPIRATION_FAST = 30.minutes
val TMDB_CACHE_EXPIRATION_DEFAULT = 12.hours

/**
 * Returns an item from the local cache, or loads it if necessary.
 *
 * Handles caching, stale caching, reloads if cache changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> tmdbGetOrDownload(
    entryTag: String,
    get: () -> Query<TmdbTimestampedEntry<T>>,
    put: (TmdbTimestampedEntry<T>) -> Unit,
    downloader: suspend () -> ApiResult<T>,
    expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): Flow<ApiResult<T>> {
    return get()
        .asFlow()
        .transformLatest {
            val cachedValue = it.executeAsOneOrNull().injectCacheMiss()
            val currentTime = Clock.System.now()
            if (cachedValue != null) {
                val age = currentTime - cachedValue.lastUpdate
                emit(Result.Value(cachedValue.value))
                if (age > expiration) {
                    Log.d(LOG_TAG, "$entryTag: Entry is old (age=$age), starting download")
                    when (val downloaded = downloadAndSave(downloader, put, coroutineContext)) {
                        is Result.Error -> {
                            Log.w(LOG_TAG, "$entryTag: Download failed, using a stale entry", downloaded.error)
                        }
                        is Result.Value -> {
                            emit(downloaded)
                        }
                    }
                }
            } else {
                Log.d(LOG_TAG, "$entryTag: No cached entry, performing download")
                emit(downloadAndSave(downloader, put, coroutineContext))
            }
        }
        .flowOn(coroutineContext)
        .distinctUntilChanged()
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
