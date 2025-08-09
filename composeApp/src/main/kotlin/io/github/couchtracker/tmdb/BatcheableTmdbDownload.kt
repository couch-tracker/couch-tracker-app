package io.github.couchtracker.tmdb

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.moviebase.tmdb.Tmdb3
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Instant

class BatchableDownloader<T : Any, Req, Res>(
    val logTag: String,
    val loadFromCache: (cache: TmdbCache) -> Query<TmdbTimestampedEntry<T>>,
    val putInCache: (cache: TmdbCache, TmdbTimestampedEntry<T>) -> Unit,
    val prepareRequest: (Req) -> Req,
    val extractFromResponse: (Res) -> T,
    val expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    val prefetch: Duration = expiration * TMDB_CACHE_PREFETCH_THRESHOLD,
)

class BatchableDownloaderFactory<ID, T : Any, Req, Res>(
    val id: ID,
    val logTag: String,
    val prepareRequest: (Req) -> Req,
    val extractFromResponse: (Res) -> T,
    val expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
    val prefetch: Duration = expiration * TMDB_CACHE_PREFETCH_THRESHOLD,
) {

    fun localized(
        language: TmdbLanguage,
        loadFromCacheFn: (TmdbCache) -> (ID, TmdbLanguage, (T, Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>,
        putInCacheFn: (TmdbCache) -> (ID, TmdbLanguage, T, Instant) -> QueryResult<Long>,
    ) = BatchableDownloader(
        logTag = logTag,
        loadFromCache = { cache -> loadFromCacheFn(cache)(id, language, ::TmdbTimestampedEntry) },
        putInCache = { cache, data -> putInCacheFn(cache)(id, language, data.value, data.lastUpdate) },
        prepareRequest = prepareRequest,
        extractFromResponse = extractFromResponse,
        expiration = expiration,
        prefetch = prefetch,
    )

    fun notLocalized(
        loadFromCacheFn: (TmdbCache) -> (ID, (details: T, lastUpdate: Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>,
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

class BatchableRequest<T : Any, Req, Res>(
    val downloader: BatchableDownloader<T, Req, Res>,
    val completable: CompletableDeferred<T> = CompletableDeferred(),
)

private fun <T : Any, Req, Res> BatchableRequest<T, Req, Res>.complete(downloadResult: Res) {
    check(completable.complete(downloader.extractFromResponse(downloadResult)))
}

/**
 * Fulfills the given requests, prioritising data from the cache (see [tmdbGetOrDownload]).
 * Requests that have to be downloaded will be downloaded together using [downloader].
 */
suspend fun <Req, Res> tmdbGetOrDownloadBatched(
    cache: TmdbCache,
    requests: List<BatchableRequest<*, Req, Res>>,
    initialRequestInput: Req,
    downloader: suspend (Tmdb3, Req) -> Res,
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
            toDownload,
            initialRequestInput,
            downloader,
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
        try {
            val result = tmdbGetOrDownload(
                entryTag = downloader.logTag,
                get = { downloader.loadFromCache(cache) },
                put = { downloader.putInCache(cache, it) },
                downloader = {
                    val downloadResult = CompletableDeferred<T>()
                    check(toDownload.complete(BatchableRequest(downloader, downloadResult)))
                    downloadResult.await()
                },
                expiration = downloader.expiration,
                prefetch = downloader.prefetch,
            )
            check(completable.complete(result))
        } catch (e: ApiException) {
            check(completable.completeExceptionally(e))
        } finally {
            toDownload.complete(null)
        }
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
    downloader: suspend (Tmdb3, Req) -> Res,
) {
    require(requests.isNotEmpty())
    val request: Req = requests.fold(initialRequestInput) { acc, request ->
        request.downloader.prepareRequest(acc)
    }
    try {
        val download = tmdbDownload { downloader(it, request) }
        for (request in requests) {
            request.complete(download)
        }
    } catch (e: ApiException) {
        for (request in requests) {
            request.completable.completeExceptionally(e)
        }
    }
}
