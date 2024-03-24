package io.github.couchtracker.tmdb

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbReleaseDates
import io.github.couchtracker.db.tmdbCache.TmdbCache

/**
 * Class that represents a TMDB movie.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbMovie(
    val id: TmdbMovieId,
    val language: TmdbLanguage,
) {
    suspend fun details(cache: TmdbCache): TmdbMovieDetail = tmdbGetOrDownload(
        get = { cache.movieDetailsCacheQueries.get(id, language).awaitAsOneOrNull() },
        put = { cache.movieDetailsCacheQueries.put(tmdbId = id, language = language, details = it) },
        downloader = { it.movies.getDetails(id.value, language.apiParameter) },
    )

    suspend fun releaseDates(cache: TmdbCache): List<TmdbReleaseDates> = tmdbGetOrDownload(
        get = { cache.movieReleaseDatesCacheQueries.get(id, language).awaitAsOneOrNull() },
        put = { cache.movieReleaseDatesCacheQueries.put(tmdbId = id, language = language, releaseDates = it) },
        downloader = {
            it.movies.getDetails(
                id.value,
                language.apiParameter,
                listOf(AppendResponse.RELEASES_DATES),
            ).releaseDates?.results.orEmpty()
        },
    )
}
