package io.github.couchtracker.tmdb

import app.moviebase.tmdb.Tmdb3
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal val tmdb = Tmdb3 {
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
inline fun <T : Any> tmdbGetOrDownload(
    get: () -> T?,
    put: (T) -> Unit,
    downloader: (Tmdb3) -> T,
): T {
    return get() ?: tmdbDownload(downloader).also {
        put(it)
    }
}

inline fun <T> tmdbDownload(download: (Tmdb3) -> T): T {
    try {
        return download(tmdb)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw TmdbException(e)
    }
}
