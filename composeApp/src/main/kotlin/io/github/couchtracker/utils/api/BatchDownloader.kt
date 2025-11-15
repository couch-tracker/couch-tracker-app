package io.github.couchtracker.utils.api

import android.util.Log
import io.github.couchtracker.utils.flatMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.yield

private const val LOG_TAG = "BatchDownloader"

/**
 * Allows callers to batch multiple download requests into a single API call.
 */
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
     * The input for [downloader] is computed by folding [initialRequestInput] with [Request.prepareRequest].
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
