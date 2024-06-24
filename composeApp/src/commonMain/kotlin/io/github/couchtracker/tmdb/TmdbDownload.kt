package io.github.couchtracker.tmdb

import app.moviebase.tmdb.Tmdb3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

private val tmdb = Tmdb3 {
    tmdbApiKey = TmdbConfig.API_KEY
    useTimeout = true
    maxRetriesOnException = 3
}

/**
 * Utility function to handle cached downloads towards TMDB.
 *
 * TODO: multiple calls on the same API should be batched, so the download is performed only once
 * TODO: the cache should expire at some point
 */
suspend fun <T : Any> tmdbGetOrDownload(
    get: suspend () -> T?,
    put: suspend (T) -> Unit,
    downloader: suspend (Tmdb3) -> T,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): T {
    return withContext(coroutineContext) {
        get() ?: tmdbDownload(downloader).also {
            put(it)
        }
    }
}

suspend fun <T> tmdbDownload(download: suspend (Tmdb3) -> T): T {
    try {
        return download(tmdb)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw TmdbException(e)
    }
}
