package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbAggregateCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.CompletableApiResult
import kotlin.time.Duration
import app.moviebase.tmdb.model.TmdbShow as ApiTmdbShow

private typealias ShowDetailsDownloader = BatchableDownloaderBuilder<TmdbShowId, List<AppendResponse>, TmdbShowDetail>

/**
 * Class that represents a TMDB show.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbShow(
    val id: TmdbShowId,
    val language: TmdbLanguage,
) {

    private val detailsDownloader = detailsDownloader("details") { it }
        .extractFromResponse { it }
        .localized(
            language = language,
            loadFromCacheFn = { cache -> cache.showDetailsCacheQueries::get },
            putInCacheFn = { cache -> cache.showDetailsCacheQueries::put },
        )
    private val aggregateCreditsDownloader = detailsDownloader("credits") { it + AppendResponse.AGGREGATE_CREDITS }
        .extractNonNullFromResponse(TmdbShowDetail::aggregateCredits)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.showAggregateCreditsCacheQueries::get },
            putInCacheFn = { cache -> cache.showAggregateCreditsCacheQueries::put },
        )
    private val imagesDownloader = detailsDownloader("images") { it + AppendResponse.IMAGES }
        .extractNonNullFromResponse(TmdbShowDetail::images)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.showImagesCacheQueries::get },
            putInCacheFn = { cache -> cache.showImagesCacheQueries::put },
        )

    private fun detailsDownloader(
        logTag: String,
        expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
        prepareRequest: (List<AppendResponse>) -> List<AppendResponse>,
    ): ShowDetailsDownloader {
        return ShowDetailsDownloader(
            id = id,
            logTag = "${id.toExternalId().serialize()}-$logTag",
            prepareRequest = prepareRequest,
            expiration = expiration,
        )
    }

    suspend fun details(
        cache: TmdbCache,
        details: CompletableApiResult<TmdbShowDetail>? = null,
        aggregateCredits: CompletableApiResult<TmdbAggregateCredits>? = null,
        images: CompletableApiResult<TmdbImages>? = null,
    ) {
        tmdbGetOrDownloadBatched(
            cache = cache,
            requests = listOfNotNull(
                details?.let { BatchableRequest(detailsDownloader, details) },
                aggregateCredits?.let { BatchableRequest(aggregateCreditsDownloader, aggregateCredits) },
                images?.let { BatchableRequest(imagesDownloader, images) },
            ),
            initialRequestInput = emptyList(),
            downloader = { appendToResponse ->
                tmdbDownloadResult(logTag = "${id.toExternalId().serialize()}-batched-details") { tmdb ->
                    tmdb.show.getDetails(id.value, language.apiParameter, appendToResponse.ifEmpty { null })
                }
            },
        )
    }

    suspend fun details(cache: TmdbCache): ApiResult<TmdbShowDetail> {
        return CompletableApiResult<TmdbShowDetail>().also {
            details(cache, details = it)
        }.await()
    }
}

fun ApiTmdbShow.toInternalTmdbShow(language: TmdbLanguage) = TmdbShow(TmdbShowId(id), language)
