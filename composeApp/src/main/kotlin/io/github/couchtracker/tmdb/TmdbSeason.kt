package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbSeasonDetail
import io.github.couchtracker.utils.api.BatchDownloader

private typealias SeasonDetailsDownloader =
    BatchDownloadableFlowBuilder<TmdbSeasonId, List<AppendResponse>, TmdbSeasonDetail>

/**
 * Class that represents a TMDB season.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbSeason(
    val id: TmdbSeasonId,
    val languages: TmdbLanguages,
) {

    private val detailsBatchDownloader = BatchDownloader<List<AppendResponse>, TmdbSeasonDetail>(
        initialRequestInput = emptyList(),
        downloader = { appendToResponse ->
            tmdbDownloadResult(logTag = "${id.toExternalId().serialize()}-batched-details") { tmdb ->
                tmdb.showSeasons.getDetails(
                    showId = id.showId.value,
                    seasonNumber = id.number,
                    language = languages.apiLanguage.apiParameter,
                    appendResponses = appendToResponse.ifEmpty { null },
                )
            }
        },
    )
    val details = detailsDownloader("details") { it }
        .extractFromResponse { it }
        .localized(
            language = languages.apiLanguage,
            loadFromCacheFn = { cache -> cache.seasonDetailsCacheQueries::get },
            putInCacheFn = { cache -> cache.seasonDetailsCacheQueries::put },
        )
        .flow(detailsBatchDownloader)

    private fun detailsDownloader(
        logTag: String,
        prepareRequest: (List<AppendResponse>) -> List<AppendResponse>,
    ): SeasonDetailsDownloader {
        return SeasonDetailsDownloader(
            id = id,
            logTag = "${id.toExternalId().serialize()}-$logTag",
            prepareRequest = prepareRequest,
        )
    }
}
