package io.github.couchtracker.tmdb

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbImages
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

    suspend fun credits(cache: TmdbCache): TmdbCredits = tmdbGetOrDownload(
        get = { cache.movieCreditsCacheQueries.get(id).awaitAsOneOrNull() },
        put = { cache.movieCreditsCacheQueries.put(tmdbId = id, credits = it) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.CREDITS),
            ).credits ?: error("credits cannot be null")
        },
    )

    suspend fun images(cache: TmdbCache): TmdbImages = tmdbGetOrDownload(
        get = { cache.movieImagesCacheQueries.get(id).awaitAsOneOrNull() },
        put = { cache.movieImagesCacheQueries.put(tmdbId = id, images = it) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.IMAGES),
            ).images ?: error("images cannot be null")
        },
    )

    suspend fun releaseDates(cache: TmdbCache): List<TmdbReleaseDates> = tmdbGetOrDownload(
        get = { cache.movieReleaseDatesCacheQueries.get(id).awaitAsOneOrNull() },
        put = { cache.movieReleaseDatesCacheQueries.put(tmdbId = id, releaseDates = it) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.RELEASES_DATES),
            ).releaseDates?.results.orEmpty()
        },
    )
}
