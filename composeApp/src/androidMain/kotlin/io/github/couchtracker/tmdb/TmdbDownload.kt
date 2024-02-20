package io.github.couchtracker.tmdb

import app.moviebase.tmdb.Tmdb3
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal val tmdb = Tmdb3 {
    tmdbApiKey = TmdbConfig.API_KEY
    useTimeout = true
    maxRetriesOnException = 3
}

inline fun <T> tmdbDownload(f: (Tmdb3) -> T): T {
    try {
        return f(tmdb)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw TmdbException(e)
    }
}
