package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

private typealias MovieDetailsDownloader = BatchDownloadableFlowBuilder<TmdbMovieId, List<AppendResponse>, TmdbMovieDetail>

/**
 * Class that represents a TMDB movie.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbMovie(
    val id: TmdbMovieId,
    val languages: TmdbLanguages,
) {

    private val detailsBatchDownloader = BatchDownloader<List<AppendResponse>, TmdbMovieDetail>(
        initialRequestInput = emptyList(),
        downloader = { appendToResponse ->
            tmdbDownloadResult(logTag = "${id.toExternalId().serialize()}-batched-details") { tmdb ->
                tmdb.movies.getDetails(id.value, languages.apiLanguage.apiParameter, appendToResponse.ifEmpty { null })
            }
        },
    )
    val details = detailsDownloader("details") { it }
        .extractFromResponse { it }
        .localized(
            language = languages.apiLanguage,
            loadFromCacheFn = { cache -> cache.movieDetailsCacheQueries::get },
            putInCacheFn = { cache -> cache.movieDetailsCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val credits = detailsDownloader("credits") { it + AppendResponse.CREDITS }
        .extractNonNullFromResponse(TmdbMovieDetail::credits)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieCreditsCacheQueries::get },
            putInCacheFn = { cache -> cache.movieCreditsCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val images = detailsDownloader("images") { it + AppendResponse.IMAGES }
        .extractNonNullFromResponse(TmdbMovieDetail::images)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieImagesCacheQueries::get },
            putInCacheFn = { cache -> cache.movieImagesCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val videos = detailsDownloader("videos") { it + AppendResponse.VIDEOS }
        .extractNonNullFromResponse("videos") { it.videos?.results }
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieVideosCacheQueries::get },
            putInCacheFn = { cache -> cache.movieVideosCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val releaseDates = detailsDownloader(
        logTag = "release_dates",
    ) { it + AppendResponse.RELEASES_DATES }
        .extractNonNullFromResponse("release_dates") { it.releaseDates?.results }
        .notLocalized(
            loadFromCacheFn = { cache -> cache.movieReleaseDatesCacheQueries::get },
            putInCacheFn = { cache -> cache.movieReleaseDatesCacheQueries::put },
        )
        .flow(detailsBatchDownloader, expiration = TMDB_CACHE_EXPIRATION_FAST)

    private fun detailsDownloader(
        logTag: String,
        prepareRequest: (List<AppendResponse>) -> List<AppendResponse>,
    ): MovieDetailsDownloader {
        return MovieDetailsDownloader(
            id = id,
            logTag = "${id.toExternalId().serialize()}-$logTag",
            prepareRequest = prepareRequest,
        )
    }
}

val TmdbMovieDetail.tmdbMovieId get() = TmdbMovieId(id)
val ApiTmdbMovie.tmdbMovieId get() = TmdbMovieId(id)
