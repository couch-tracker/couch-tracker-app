package io.github.couchtracker.tmdb

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.CompletableApiResult
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.flatMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1
import kotlin.time.Duration
import kotlin.time.Instant

class BatchableDownloader<T : Any, Req, Res>(
    val logTag: String,
    val loadFromCache: (cache: TmdbCache) -> Query<TmdbTimestampedEntry<T>>,
    val putInCache: (cache: TmdbCache, TmdbTimestampedEntry<T>) -> Unit,
    val prepareRequest: (Req) -> Req,
    val extractFromResponse: (Res) -> ApiResult<T>,
    val expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    val prefetch: Duration = expiration * TMDB_CACHE_PREFETCH_THRESHOLD,
)

typealias LocalizedQueryBuilder<ID, T> = (ID, TmdbLanguage, (T, Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>
typealias QueryBuilder<ID, T> = (ID, (details: T, lastUpdate: Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>

class BatchableDownloaderBuilder<ID, Req, Res>(
    val id: ID,
    val logTag: String,
    val prepareRequest: (Req) -> Req,
    val expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    val prefetch: Duration = expiration * TMDB_CACHE_PREFETCH_THRESHOLD,
) {

    inner class Step2<T : Any>(
        val extractFromResponse: (Res) -> ApiResult<T>,
    ) {
        fun localized(
            language: TmdbLanguage,
            loadFromCacheFn: (TmdbCache) -> LocalizedQueryBuilder<ID, T>,
            putInCacheFn: (TmdbCache) -> (ID, TmdbLanguage, T, Instant) -> QueryResult<Long>,
        ) = BatchableDownloader(
            logTag = "$language-$logTag",
            loadFromCache = { cache -> loadFromCacheFn(cache)(id, language, ::TmdbTimestampedEntry) },
            putInCache = { cache, data -> putInCacheFn(cache)(id, language, data.value, data.lastUpdate) },
            prepareRequest = prepareRequest,
            extractFromResponse = extractFromResponse,
            expiration = expiration,
            prefetch = prefetch,
        )

        fun notLocalized(
            loadFromCacheFn: (TmdbCache) -> QueryBuilder<ID, T>,
            putInCacheFn: (TmdbCache) -> (ID, T, Instant) -> QueryResult<Long>,
        ) = BatchableDownloader(
            logTag = logTag,
            loadFromCache = { cache -> loadFromCacheFn(cache)(id, ::TmdbTimestampedEntry) },
            putInCache = { cache, data -> putInCacheFn(cache)(id, data.value, data.lastUpdate) },
            prepareRequest = prepareRequest,
            extractFromResponse = extractFromResponse,
            expiration = expiration,
            prefetch = prefetch,
        )
    }

    fun <T : Any> extractResultFromResponse(extractFromResponse: (Res) -> ApiResult<T>): Step2<T> {
        return Step2(extractFromResponse)
    }

    fun <T : Any> extractFromResponse(extractFromResponse: (Res) -> T): Step2<T> {
        return extractResultFromResponse { response ->
            Result.Value(extractFromResponse(response))
        }
    }

    fun <T : Any> extractNonNullFromResponse(property: KProperty1<Res, T?>): Step2<T> {
        return extractNonNullFromResponse(fieldName = property.name, extractFromResponse = property::invoke)
    }

    fun <T : Any> extractNonNullFromResponse(fieldName: String, extractFromResponse: (Res) -> T?): Step2<T> {
        return extractResultFromResponse { response ->
            val result = extractFromResponse(response)
            if (result == null) {
                Result.Error(ApiException.DeserializationError("Unexpected null field $fieldName", cause = null))
            } else {
                Result.Value(result)
            }
        }
    }
}

class BatchableRequest<T : Any, Req, Res>(
    val downloader: BatchableDownloader<T, Req, Res>,
    val completable: CompletableApiResult<T> = CompletableDeferred(),
)

private fun <T : Any, Req, Res> BatchableRequest<T, Req, Res>.complete(downloadResult: ApiResult<Res>) {
    val result = downloadResult.flatMap {
        downloader.extractFromResponse(it)
    }
    check(completable.complete(result))
}

/**
 * Fulfills the given requests, prioritising data from the cache (see [tmdbGetOrDownload]).
 * Requests that have to be downloaded will be downloaded together using [downloader].
 */
suspend fun <Req, Res> tmdbGetOrDownloadBatched(
    cache: TmdbCache,
    requests: List<BatchableRequest<*, Req, Res>>,
    initialRequestInput: Req,
    downloader: suspend (Req) -> ApiResult<Res>,
) = coroutineScope {
    val requestsToDownload = requests.map { request ->
        request.getOrRequestDownload(
            scope = this,
            cache = cache,
        )
    }
    val toDownload = requestsToDownload.awaitAll().filterNotNull()
    if (toDownload.isNotEmpty()) {
        batchDownload(
            requests = toDownload,
            initialRequestInput = initialRequestInput,
            downloader = downloader,
        )
    }
}

/**
 * Loads the item using [tmdbGetOrDownload].
 * The caller has to perform the download if the returned value completes with a non-null request.
 */
private fun <T : Any, Req, Res> BatchableRequest<T, Req, Res>.getOrRequestDownload(
    scope: CoroutineScope,
    cache: TmdbCache,
): CompletableDeferred<BatchableRequest<T, Req, Res>?> {
    val toDownload = CompletableDeferred<BatchableRequest<T, Req, Res>?>()
    scope.launch {
        val result = tmdbGetOrDownload(
            entryTag = downloader.logTag,
            get = { downloader.loadFromCache(cache) },
            put = { downloader.putInCache(cache, it) },
            downloader = {
                val downloadResult = CompletableApiResult<T>()
                check(toDownload.complete(BatchableRequest(downloader, downloadResult)))
                downloadResult.await()
            },
            backgroundDownloader = {
                val downloadResult = CompletableApiResult<T>()
                check(toDownload.complete(BatchableRequest(downloader, downloadResult)))
            },
            expiration = downloader.expiration,
            prefetch = downloader.prefetch,
        )
        toDownload.complete(null)
        check(completable.complete(result))
    }
    return toDownload
}

/**
 * Downloads all requests in batch.
 * the input for [downloader] is computed by folding [initialRequestInput] with [BatchableDownloader.prepareRequest].
 */
private suspend fun <Req, Res> batchDownload(
    requests: List<BatchableRequest<*, Req, Res>>,
    initialRequestInput: Req,
    downloader: suspend (Req) -> ApiResult<Res>,
) {
    require(requests.isNotEmpty())
    val request: Req = requests.fold(initialRequestInput) { acc, request ->
        request.downloader.prepareRequest(acc)
    }
    val download = downloader(request)
    for (request in requests) {
        request.complete(download)
    }
}
