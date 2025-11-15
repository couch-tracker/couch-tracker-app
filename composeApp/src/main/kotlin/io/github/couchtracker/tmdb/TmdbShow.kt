package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.utils.api.BatchDownloader
import app.moviebase.tmdb.model.TmdbShow as ApiTmdbShow

private typealias ShowDetailsDownloader = BatchDownloadableFlowBuilder<TmdbShowId, List<AppendResponse>, TmdbShowDetail>

/**
 * Class that represents a TMDB show.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbShow(
    val id: TmdbShowId,
    val languages: TmdbLanguages,
) {

    private val detailsBatchDownloader = BatchDownloader<List<AppendResponse>, TmdbShowDetail>(
        initialRequestInput = emptyList(),
        downloader = { appendToResponse ->
            tmdbDownloadResult(logTag = "${id.toExternalId().serialize()}-batched-details") { tmdb ->
                tmdb.show.getDetails(id.value, languages.apiLanguage.apiParameter, appendToResponse.ifEmpty { null })
            }
        },
    )
    val details = detailsDownloader("details") { it }
        .extractFromResponse { it }
        .localized(
            language = languages.apiLanguage,
            loadFromCacheFn = { cache -> cache.showDetailsCacheQueries::get },
            putInCacheFn = { cache -> cache.showDetailsCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val aggregateCredits = detailsDownloader("credits") { it + AppendResponse.AGGREGATE_CREDITS }
        .extractNonNullFromResponse(TmdbShowDetail::aggregateCredits)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.showAggregateCreditsCacheQueries::get },
            putInCacheFn = { cache -> cache.showAggregateCreditsCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val images = detailsDownloader("images") { it + AppendResponse.IMAGES }
        .extractNonNullFromResponse(TmdbShowDetail::images)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.showImagesCacheQueries::get },
            putInCacheFn = { cache -> cache.showImagesCacheQueries::put },
        )
        .flow(detailsBatchDownloader)

    private fun detailsDownloader(
        logTag: String,
        prepareRequest: (List<AppendResponse>) -> List<AppendResponse>,
    ): ShowDetailsDownloader {
        return ShowDetailsDownloader(
            id = id,
            logTag = "${id.toExternalId().serialize()}-$logTag",
            prepareRequest = prepareRequest,
        )
    }
}

val ApiTmdbShow.tmdbShowId get() = TmdbShowId(id)
val TmdbShowDetail.tmdbShowId get() = TmdbShowId(id)
