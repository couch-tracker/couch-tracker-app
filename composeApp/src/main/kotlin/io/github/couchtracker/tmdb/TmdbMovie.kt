package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbReleaseDates
import app.moviebase.tmdb.model.TmdbVideo
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.utils.ApiException
import kotlinx.coroutines.CompletableDeferred
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

private typealias MovieBatchableDownloader<T> = BatchableDownloaderFactory<TmdbMovieId, T, List<AppendResponse>, TmdbMovieDetail>

/**
 * Class that represents a TMDB movie.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbMovie(
    val id: TmdbMovieId,
    val language: TmdbLanguage,
) {
    private val detailsDownloader = MovieBatchableDownloader(
        id = id,
        logTag = "${id.toExternalId().serialize()}-$language-details",
        prepareRequest = { it },
        extractFromResponse = { it },
    ).localized(
        language = language,
        loadFromCacheFn = { it.movieDetailsCacheQueries::get },
        putInCacheFn = { it.movieDetailsCacheQueries::put },
    )

    private val creditsDownloader = MovieBatchableDownloader(
        id = id,
        logTag = "${id.toExternalId().serialize()}-credits",
        prepareRequest = { it + AppendResponse.CREDITS },
        extractFromResponse = { it.credits ?: throw ApiException.DeserializationError("credits cannot be null", null) },
    ).notLocalized(
        loadFromCacheFn = { cache -> cache.movieCreditsCacheQueries::get },
        putInCacheFn = { cache -> cache.movieCreditsCacheQueries::put },
    )

    private val imagesDownloader = MovieBatchableDownloader(
        id = id,
        logTag = "${id.toExternalId().serialize()}-images",
        prepareRequest = { it + AppendResponse.IMAGES },
        extractFromResponse = { it.images ?: throw ApiException.DeserializationError("images cannot be null", null) },
    ).notLocalized(
        loadFromCacheFn = { it.movieImagesCacheQueries::get },
        putInCacheFn = { it.movieImagesCacheQueries::put },
    )

    private val videosDownloader = MovieBatchableDownloader(
        id = id,
        logTag = "${id.toExternalId().serialize()}-videos",
        prepareRequest = { it + AppendResponse.VIDEOS },
        extractFromResponse = { it.videos?.results ?: throw ApiException.DeserializationError("videos cannot be null", null) },
    ).notLocalized(
        loadFromCacheFn = { it.movieVideosCacheQueries::get },
        putInCacheFn = { it.movieVideosCacheQueries::put },
    )

    private val releaseDatesDownloader = MovieBatchableDownloader(
        id = id,
        logTag = "${id.toExternalId().serialize()}-release_dates",
        prepareRequest = { it + AppendResponse.RELEASES_DATES },
        extractFromResponse = { it.releaseDates?.results ?: throw ApiException.DeserializationError("releaseDates cannot be null", null) },
        expiration = TMDB_CACHE_EXPIRATION_FAST,
    ).notLocalized(
        loadFromCacheFn = { it.movieReleaseDatesCacheQueries::get },
        putInCacheFn = { it.movieReleaseDatesCacheQueries::put },
    )

    suspend fun details(
        cache: TmdbCache,
        details: CompletableDeferred<TmdbMovieDetail>? = null,
        credits: CompletableDeferred<TmdbCredits>? = null,
        images: CompletableDeferred<TmdbImages>? = null,
        videos: CompletableDeferred<List<TmdbVideo>>? = null,
        releaseDates: CompletableDeferred<List<TmdbReleaseDates>>? = null,
    ) {
        tmdbGetOrDownloadBatched(
            cache = cache,
            requests = listOfNotNull(
                details?.let { BatchableRequest(detailsDownloader, details) },
                credits?.let { BatchableRequest(creditsDownloader, credits) },
                images?.let { BatchableRequest(imagesDownloader, images) },
                videos?.let { BatchableRequest(videosDownloader, videos) },
                releaseDates?.let { BatchableRequest(releaseDatesDownloader, releaseDates) },
            ),
            initialRequestInput = emptyList(),
            downloader = { tmdb, appendToResponse ->
                tmdb.movies.getDetails(id.value, language.apiParameter, appendToResponse.ifEmpty { null })
            },
        )
    }

    suspend fun details(cache: TmdbCache): TmdbMovieDetail {
        return CompletableDeferred<TmdbMovieDetail>().also {
            details(cache, details = it)
        }.await()
    }
}

fun ApiTmdbMovie.toInternalTmdbMovie(language: TmdbLanguage) = TmdbMovie(TmdbMovieId(id), language)
