package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbReleaseDates
import app.moviebase.tmdb.model.TmdbVideo
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import kotlinx.coroutines.CompletableDeferred
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

private typealias MovieDetailsRequestInput = List<AppendResponse>
private typealias MovieDetailsRequestOutput = TmdbMovieDetail

/**
 * Class that represents a TMDB movie.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbMovie(
    val id: TmdbMovieId,
    val language: TmdbLanguage,
) {
    private val detailsDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-$language-details",
        loadFromCache = { cache ->
            cache.movieDetailsCacheQueries.get(
                tmdbId = id,
                language = language,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.movieDetailsCacheQueries.put(
                tmdbId = id,
                language = language,
                details = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: MovieDetailsRequestInput -> appendResponses },
        extractFromResponse = { movieDetails: MovieDetailsRequestOutput -> movieDetails },
    )
    private val creditsDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-credits",
        loadFromCache = { cache ->
            cache.movieCreditsCacheQueries.get(
                tmdbId = id,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.movieCreditsCacheQueries.put(
                tmdbId = id,
                credits = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: MovieDetailsRequestInput -> appendResponses.plusElement(AppendResponse.CREDITS) },
        extractFromResponse = { movieDetails: MovieDetailsRequestOutput ->
            movieDetails.credits ?: throw ApiException.DeserializationError("credits cannot be null", null)
        },
    )
    private val imagesDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-images",
        loadFromCache = { cache ->
            cache.movieImagesCacheQueries.get(
                tmdbId = id,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.movieImagesCacheQueries.put(
                tmdbId = id,
                images = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: MovieDetailsRequestInput -> appendResponses.plusElement(AppendResponse.IMAGES) },
        extractFromResponse = { movieDetails: MovieDetailsRequestOutput ->
            movieDetails.images ?: throw ApiException.DeserializationError("images cannot be null", null)
        },
    )
    private val videosDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-videos",
        loadFromCache = { cache ->
            cache.movieVideosCacheQueries.get(
                tmdbId = id,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.movieVideosCacheQueries.put(
                tmdbId = id,
                videos = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: MovieDetailsRequestInput -> appendResponses.plusElement(AppendResponse.VIDEOS) },
        extractFromResponse = { movieDetails: MovieDetailsRequestOutput ->
            movieDetails.videos?.results ?: throw ApiException.DeserializationError("videos cannot be null", null)
        },
    )
    private val releaseDatesDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-release_dates",
        loadFromCache = { cache ->
            cache.movieReleaseDatesCacheQueries.get(
                tmdbId = id,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.movieReleaseDatesCacheQueries.put(
                tmdbId = id,
                releaseDates = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: MovieDetailsRequestInput -> appendResponses.plusElement(AppendResponse.RELEASES_DATES) },
        extractFromResponse = { movieDetails: MovieDetailsRequestOutput ->
            movieDetails.releaseDates?.results ?: throw ApiException.DeserializationError("releaseDates cannot be null", null)
        },
        expiration = TMDB_CACHE_EXPIRATION_FAST,
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
