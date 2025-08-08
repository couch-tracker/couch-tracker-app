package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbAggregateCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.ApiException
import kotlinx.coroutines.CompletableDeferred
import app.moviebase.tmdb.model.TmdbShow as ApiTmdbShow

private typealias ShowDetailsRequestInput = List<AppendResponse>
private typealias ShowDetailsRequestOutput = TmdbShowDetail

/**
 * Class that represents a TMDB show.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbShow(
    val id: TmdbShowId,
    val language: TmdbLanguage,
) {
    private val detailsDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-$language-details",
        loadFromCache = { cache ->
            cache.showDetailsCacheQueries.get(
                tmdbId = id,
                language = language,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.showDetailsCacheQueries.put(
                tmdbId = id,
                language = language,
                details = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: ShowDetailsRequestInput -> appendResponses },
        extractFromResponse = { showDetails: ShowDetailsRequestOutput -> showDetails },
    )
    private val aggregateCreditsDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-credits",
        loadFromCache = { cache ->
            cache.showAggregateCreditsCacheQueries.get(
                tmdbId = id,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.showAggregateCreditsCacheQueries.put(
                tmdbId = id,
                credits = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: ShowDetailsRequestInput -> appendResponses.plusElement(AppendResponse.AGGREGATE_CREDITS) },
        extractFromResponse = { showDetails: ShowDetailsRequestOutput ->
            showDetails.aggregateCredits ?: throw ApiException.DeserializationError("aggregateCredits cannot be null", null)
        },
    )
    private val imagesDownloader = BatchableDownloader(
        logTag = "${id.toExternalId().serialize()}-images",
        loadFromCache = { cache ->
            cache.showImagesCacheQueries.get(
                tmdbId = id,
                mapper = ::TmdbTimestampedEntry,
            )
        },
        putInCache = { cache, data ->
            cache.showImagesCacheQueries.put(
                tmdbId = id,
                images = data.value,
                lastUpdate = data.lastUpdate,
            )
        },
        prepareRequest = { appendResponses: ShowDetailsRequestInput -> appendResponses.plusElement(AppendResponse.IMAGES) },
        extractFromResponse = { showDetails: ShowDetailsRequestOutput ->
            showDetails.images ?: throw ApiException.DeserializationError("images cannot be null", null)
        },
    )

    suspend fun details(
        cache: TmdbCache,
        details: CompletableDeferred<TmdbShowDetail>? = null,
        aggregateCredits: CompletableDeferred<TmdbAggregateCredits>? = null,
        images: CompletableDeferred<TmdbImages>? = null,
    ) {
        tmdbGetOrDownloadBatched(
            cache = cache,
            requests = listOfNotNull(
                details?.let { BatchableRequest(detailsDownloader, details) },
                aggregateCredits?.let { BatchableRequest(aggregateCreditsDownloader, aggregateCredits) },
                images?.let { BatchableRequest(imagesDownloader, images) },
            ),
            initialRequestInput = emptyList(),
            downloader = { tmdb, appendToResponse ->
                tmdb.show.getDetails(id.value, language.apiParameter, appendToResponse.ifEmpty { null })
            },
        )
    }

    suspend fun details(cache: TmdbCache): TmdbShowDetail {
        return CompletableDeferred<TmdbShowDetail>().also {
            details(cache, details = it)
        }.await()
    }
}

fun ApiTmdbShow.toInternalTmdbShow(language: TmdbLanguage) = TmdbShow(TmdbShowId(id), language)
