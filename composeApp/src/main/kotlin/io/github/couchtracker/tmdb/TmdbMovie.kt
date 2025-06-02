package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbReleaseDates
import app.moviebase.tmdb.model.TmdbVideo
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

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
        entryTag = "${id.toExternalId().serialize()}-$language-details",
        get = { cache.movieDetailsCacheQueries.get(id, language, ::TmdbTimestampedEntry) },
        put = { cache.movieDetailsCacheQueries.put(tmdbId = id, language = language, details = it.value, lastUpdate = it.lastUpdate) },
        downloader = { it.movies.getDetails(id.value, language.apiParameter) },
    )

    suspend fun credits(cache: TmdbCache): TmdbCredits = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-credits",
        get = { cache.movieCreditsCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieCreditsCacheQueries.put(tmdbId = id, credits = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.CREDITS),
            ).credits ?: error("credits cannot be null")
        },
    )

    suspend fun images(cache: TmdbCache): TmdbImages = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-images",
        get = { cache.movieImagesCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieImagesCacheQueries.put(tmdbId = id, images = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.IMAGES),
            ).images ?: error("images cannot be null")
        },
    )

    suspend fun videos(cache: TmdbCache): List<TmdbVideo> = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-videos",
        get = { cache.movieVideosCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieVideosCacheQueries.put(tmdbId = id, videos = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.VIDEOS),
            ).videos?.results ?: error("videos cannot be null")
        },
    )

    suspend fun releaseDates(cache: TmdbCache): List<TmdbReleaseDates> = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-release_dates",
        get = { cache.movieReleaseDatesCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieReleaseDatesCacheQueries.put(tmdbId = id, releaseDates = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.movies.getDetails(
                id.value,
                null,
                listOf(AppendResponse.RELEASES_DATES),
            ).releaseDates?.results.orEmpty()
        },
        expiration = TMDB_CACHE_EXPIRATION_FAST,
    )
}

fun ApiTmdbMovie.toInternalTmdbMovie(language: TmdbLanguage) = TmdbMovie(TmdbMovieId(id), language)
