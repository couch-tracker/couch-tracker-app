package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbReleaseDates
import app.moviebase.tmdb.model.TmdbVideo
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.CompletableApiResult
import kotlin.time.Duration
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

private typealias MovieDetailsDownloader = BatchableDownloaderBuilder<TmdbMovieId, List<AppendResponse>, TmdbMovieDetail>

/**
 * Class that represents a TMDB movie.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbMovie(
    val id: TmdbMovieId,
    val languages: TmdbLanguages,
) {

    private val detailsDownloader = detailsDownloader("details") { it }
        .extractFromResponse { it }
        .localized(
            language = languages.apiLanguage,
            loadFromCacheFn = { cache -> cache.movieDetailsCacheQueries::get },
            putInCacheFn = { cache -> cache.movieDetailsCacheQueries::put },
        )
    private val creditsDownloader = detailsDownloader("credits") { it + AppendResponse.CREDITS }
        .extractNonNullFromResponse(TmdbMovieDetail::credits)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieCreditsCacheQueries::get },
            putInCacheFn = { cache -> cache.movieCreditsCacheQueries::put },
        )
    private val imagesDownloader = detailsDownloader("images") { it + AppendResponse.IMAGES }
        .extractNonNullFromResponse(TmdbMovieDetail::images)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieImagesCacheQueries::get },
            putInCacheFn = { cache -> cache.movieImagesCacheQueries::put },
        )
    private val videosDownloader = detailsDownloader("videos") { it + AppendResponse.VIDEOS }
        .extractNonNullFromResponse("videos") { it.videos?.results }
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieVideosCacheQueries::get },
            putInCacheFn = { cache -> cache.movieVideosCacheQueries::put },
        )
    private val releaseDatesDownloader = detailsDownloader(
        logTag = "release_dates",
        expiration = TMDB_CACHE_EXPIRATION_FAST,
    ) { it + AppendResponse.RELEASES_DATES }
        .extractNonNullFromResponse("release_dates") { it.releaseDates?.results }
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieReleaseDatesCacheQueries::get },
            putInCacheFn = { cache -> cache.movieReleaseDatesCacheQueries::put },
        )

    private fun detailsDownloader(
        logTag: String,
        expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
        prepareRequest: (List<AppendResponse>) -> List<AppendResponse>,
    ): MovieDetailsDownloader {
        return MovieDetailsDownloader(
            id = id,
            logTag = "${id.toExternalId().serialize()}-$logTag",
            prepareRequest = prepareRequest,
            expiration = expiration,
        )
    }

    suspend fun details(
        cache: TmdbCache,
        details: CompletableApiResult<TmdbMovieDetail>? = null,
        credits: CompletableApiResult<TmdbCredits>? = null,
        images: CompletableApiResult<TmdbImages>? = null,
        videos: CompletableApiResult<List<TmdbVideo>>? = null,
        releaseDates: CompletableApiResult<List<TmdbReleaseDates>>? = null,
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
            downloader = { appendToResponse ->
                tmdbDownloadResult(logTag = "${id.toExternalId().serialize()}-batched-details") { tmdb ->
                    tmdb.movies.getDetails(id.value, languages.apiLanguage.apiParameter, appendToResponse.ifEmpty { null })
                }
            },
        )
    }

    suspend fun details(cache: TmdbCache): ApiResult<TmdbMovieDetail> {
        return CompletableApiResult<TmdbMovieDetail>().also {
            details(cache, details = it)
        }.await()
    }
}

val TmdbMovieDetail.tmdbMovieId get() = TmdbMovieId(id)
val ApiTmdbMovie.tmdbMovieId get() = TmdbMovieId(id)
