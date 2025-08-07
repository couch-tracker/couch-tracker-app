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
    val languages: TmdbLanguages,
) {
    suspend fun details(cache: TmdbCache): TmdbMovieDetail = tmdbGetOrDownload(
        languages = languages,
        tryNextLanguage = { it.overview.isBlank() && it.tagline.isBlank() },
        entryTag = { lang -> "${id.toExternalId().serialize()}-$lang-details" },
        get = { lang -> cache.movieDetailsCacheQueries.get(tmdbId = id, language = lang, mapper = ::TmdbTimestampedEntry) },
        put = { cache.movieDetailsCacheQueries.put(tmdbId = id, language = it.language, details = it.value, lastUpdate = it.lastUpdate) },
        downloader = { lang -> movies.getDetails(movieId = id.value, language = lang.apiParameter) },
    )

    suspend fun credits(cache: TmdbCache): TmdbCredits = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-credits",
        get = { cache.movieCreditsCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieCreditsCacheQueries.put(tmdbId = id, credits = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            movies.getDetails(
                movieId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.CREDITS),
            ).credits ?: error("credits cannot be null")
        },
    )

    suspend fun images(cache: TmdbCache): TmdbImages = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-images",
        get = { cache.movieImagesCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieImagesCacheQueries.put(tmdbId = id, images = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            movies.getDetails(
                movieId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.IMAGES),
            ).images ?: error("images cannot be null")
        },
    )

    suspend fun videos(cache: TmdbCache): List<TmdbVideo> = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-videos",
        get = { cache.movieVideosCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieVideosCacheQueries.put(tmdbId = id, videos = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            movies.getDetails(
                movieId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.VIDEOS),
            ).videos?.results ?: error("videos cannot be null")
        },
    )

    suspend fun releaseDates(cache: TmdbCache): List<TmdbReleaseDates> = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-release_dates",
        get = { cache.movieReleaseDatesCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.movieReleaseDatesCacheQueries.put(tmdbId = id, releaseDates = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            movies.getDetails(
                movieId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.RELEASES_DATES),
            ).releaseDates?.results.orEmpty()
        },
        expiration = TMDB_CACHE_EXPIRATION_FAST,
    )
}

val ApiTmdbMovie.tmdbMovieId get() = TmdbMovieId(id)
