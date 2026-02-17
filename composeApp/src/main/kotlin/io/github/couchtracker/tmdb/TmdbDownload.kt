package io.github.couchtracker.tmdb

import android.util.Log
import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.QueryResult
import app.moviebase.tmdb.Tmdb3
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.api.ApiContext
import io.github.couchtracker.utils.api.ApiException
import io.github.couchtracker.utils.api.ApiResult
import io.github.couchtracker.utils.api.FlowRetryToken
import io.github.couchtracker.utils.api.runApiCatching
import io.github.couchtracker.utils.injectApiError
import io.github.couchtracker.utils.injectCacheMiss
import io.github.couchtracker.utils.logExecutionTime
import io.github.couchtracker.utils.settings.get
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
import kotlin.time.Instant

private const val LOG_TAG = "TmdbDownload"

val TMDB_CACHE_EXPIRATION_FAST = 30.minutes
val TMDB_CACHE_EXPIRATION_DEFAULT = 12.hours

typealias TmdbApiContext = ApiContext<TmdbLanguages>
typealias LocalizedQueryBuilder<ID, L, T> = (ID, L, (T, Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>
typealias QueryBuilder<ID, T> = (ID, (details: T, lastUpdate: Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>

fun tmdbApiContext(
    retryToken: FlowRetryToken = FlowRetryToken(),
    languages: Flow<TmdbLanguages> = AppSettings.get { Tmdb.Languages }.map { it.current },
): TmdbApiContext {
    return ApiContext(retryToken, languages)
}

fun <ID, T : Any> tmdbCachedDownload(
    id: ID,
    logTag: String,
    loadFromCacheFn: TmdbCache.() -> QueryBuilder<ID, T>,
    putInCacheFn: TmdbCache.() -> (ID, T, Instant) -> QueryResult<Long>,
    download: suspend (Tmdb3) -> T,
    expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    cache: TmdbCache = KoinPlatform.getKoin().get(),
): Flow<ApiResult<T>> {
    val logTag = "$id-$logTag"
    return tmdbGetOrDownload(
        entryTag = logTag,
        loadFromCache = { loadFromCacheFn(cache)(id, ::TmdbTimestampedEntry) },
        putInCache = { data -> putInCacheFn(cache)(id, data.value, data.lastUpdate) },
        downloader = { tmdbDownloadResult(logTag = logTag, download = download) },
        expiration = expiration,
    )
}

@Suppress("LongParameterList")
fun <ID, L, T : Any> tmdbLocalizedCachedDownload(
    id: ID,
    logTag: String,
    language: L,
    loadFromCacheFn: TmdbCache.() -> LocalizedQueryBuilder<ID, L, T>,
    putInCacheFn: TmdbCache.() -> (ID, L, T, Instant) -> QueryResult<Long>,
    download: suspend (Tmdb3) -> T,
    expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    cache: TmdbCache = KoinPlatform.getKoin().get(),
): Flow<ApiResult<T>> {
    val logTag = "$id-$language-$logTag"
    return tmdbGetOrDownload(
        entryTag = logTag,
        loadFromCache = { loadFromCacheFn(cache)(id, language, ::TmdbTimestampedEntry) },
        putInCache = { data -> putInCacheFn(cache)(id, language, data.value, data.lastUpdate) },
        downloader = { tmdbDownloadResult(logTag = logTag, download = { download(it) }) },
        expiration = expiration,
    )
}

/**
 * Returns an item from the local cache, or loads it if necessary.
 *
 * Handles caching, stale caching, reloads if cache changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T : Any> tmdbGetOrDownload(
    entryTag: String,
    loadFromCache: () -> Query<TmdbTimestampedEntry<T>>,
    putInCache: (TmdbTimestampedEntry<T>) -> Unit,
    downloader: suspend () -> ApiResult<T>,
    expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): Flow<ApiResult<T>> {
    return loadFromCache()
        .asFlow()
        .transformLatest {
            val cachedValue = it.executeAsOneOrNull().injectCacheMiss()
            val currentTime = Clock.System.now()
            if (cachedValue != null) {
                emit(Result.Value(cachedValue.value))
                val age = currentTime - cachedValue.lastUpdate
                if (age > expiration) {
                    Log.d(LOG_TAG, "$entryTag: Entry is old (age=$age), starting download")
                    when (val downloaded = downloadAndSave(downloader, putInCache, coroutineContext)) {
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
                emit(downloadAndSave(downloader, putInCache, coroutineContext))
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
                logExecutionTime(logTag ?: LOG_TAG, "Tmdb download") {
                    download(client)
                }
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
