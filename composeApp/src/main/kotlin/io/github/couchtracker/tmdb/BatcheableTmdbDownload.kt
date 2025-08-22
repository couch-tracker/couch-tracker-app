package io.github.couchtracker.tmdb

import android.util.Log
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield
import org.koin.mp.KoinPlatform
import kotlin.reflect.KProperty1
import kotlin.time.Duration
import kotlin.time.Instant

private const val LOG_TAG = "BatchableTmdbDownload"

typealias LocalizedQueryBuilder<ID, T> = (ID, TmdbLanguage, (T, Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>
typealias QueryBuilder<ID, T> = (ID, (details: T, lastUpdate: Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>

class BatchDownloadableFlowBuilder<ID, Req, Res>(
    val id: ID,
    val logTag: String,
    val prepareRequest: (Req) -> Req,
) {

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

    inner class Step2<T : Any>(
        val extractFromResponse: (Res) -> ApiResult<T>,
    ) {
        fun localized(
            language: TmdbLanguage,
            loadFromCacheFn: (TmdbCache) -> LocalizedQueryBuilder<ID, T>,
            putInCacheFn: (TmdbCache) -> (ID, TmdbLanguage, T, Instant) -> QueryResult<Long>,
        ) = Step3(
            logTag = "$language-$logTag",
            loadFromCache = { cache -> loadFromCacheFn(cache)(id, language, ::TmdbTimestampedEntry) },
            putInCache = { cache, data -> putInCacheFn(cache)(id, language, data.value, data.lastUpdate) },
        )

        fun notLocalized(
            loadFromCacheFn: (TmdbCache) -> QueryBuilder<ID, T>,
            putInCacheFn: (TmdbCache) -> (ID, T, Instant) -> QueryResult<Long>,
        ) = Step3(
            logTag = logTag,
            loadFromCache = { cache -> loadFromCacheFn(cache)(id, ::TmdbTimestampedEntry) },
            putInCache = { cache, data -> putInCacheFn(cache)(id, data.value, data.lastUpdate) },
        )

        inner class Step3(
            val logTag: String,
            val loadFromCache: (cache: TmdbCache) -> Query<TmdbTimestampedEntry<T>>,
            val putInCache: (cache: TmdbCache, TmdbTimestampedEntry<T>) -> Unit,
        ) {
            fun flow(
                downloader: BatchDownloader<Req, Res>,
                expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
                cache: TmdbCache = KoinPlatform.getKoin().get(),
            ): Flow<ApiResult<T>> {
                return tmdbGetOrDownload(
                    entryTag = logTag,
                    get = { loadFromCache(cache) },
                    put = { putInCache(cache, it) },
                    downloader = { downloader.download(prepareRequest, extractFromResponse) },
                    expiration = expiration,
                )
            }
        }
    }
}

class BatchDownloader<Req, Res>(
    val initialRequestInput: Req,
    val downloader: suspend (Req) -> ApiResult<Res>,
) {
    private val requestsQueue = Channel<Request<*, Req, Res>>(Int.MAX_VALUE)

    private class Request<T : Any, Req, Res>(
        val prepareRequest: (Req) -> Req,
        val extractFromResponse: (Res) -> ApiResult<T>,
        val completable: CompletableApiResult<T> = CompletableDeferred(),
    )

    /**
     * Collects all requests currently in the queue
     */
    private fun collectQueuedRequests(): List<Request<*, Req, Res>> {
        return buildList {
            while (true) {
                val immediateRequest = requestsQueue.tryReceive()
                if (immediateRequest.isSuccess) {
                    add(immediateRequest.getOrThrow())
                } else {
                    break
                }
            }
        }
    }

    /**
     * Downloads all requests in batch.
     * the input for [downloader] is computed by folding [initialRequestInput] with [BatchableDownloadable.prepareRequest].
     */
    private suspend fun batchDownload(requests: List<Request<*, Req, Res>>) {
        require(requests.isNotEmpty())
        val request: Req = requests.fold(initialRequestInput) { acc, request ->
            request.prepareRequest(acc)
        }
        val download = downloader(request)
        for (request in requests) {
            request.complete(download)
        }
    }

    private fun <T : Any, Req, Res> Request<T, Req, Res>.complete(downloadResult: ApiResult<Res>) {
        val result = downloadResult.flatMap {
            extractFromResponse(it)
        }
        check(completable.complete(result))
    }

    /**
     * Downloads the given request, possibly batching it with other enqueued requests
     */
    suspend fun <T : Any> download(prepareRequest: (Req) -> Req, extractFromResponse: (Res) -> ApiResult<T>): ApiResult<T> {
        val completable = CompletableApiResult<T>()
        requestsQueue.send(Request(prepareRequest, extractFromResponse, completable))
        // Let's give some time for other requests to arrive
        yield()

        val requests = collectQueuedRequests()
        if (requests.isNotEmpty()) {
            Log.d(LOG_TAG, "Processing ${requests.size} requests in batch")
            batchDownload(requests)
        }

        return completable.await()
    }
}
