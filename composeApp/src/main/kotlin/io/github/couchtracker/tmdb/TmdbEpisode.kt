package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbEpisodeDetail
import io.github.couchtracker.utils.api.BatchDownloader

private typealias EpisodeDetailsDownloader =
    BatchDownloadableFlowBuilder<TmdbEpisodeId, List<AppendResponse>, TmdbEpisodeDetail>

/**
 * Class that represents a TMDB episode.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbEpisode(
    val id: TmdbEpisodeId,
    val languages: TmdbLanguages,
) {

    private val detailsBatchDownloader = BatchDownloader<List<AppendResponse>, TmdbEpisodeDetail>(
        initialRequestInput = emptyList(),
        downloader = { appendToResponse ->
            tmdbDownloadResult(logTag = "${id.toExternalId().serialize()}-batched-details") { tmdb ->
                tmdb.showEpisodes.getDetails(
                    showId = id.showId.value,
                    seasonNumber = id.seasonId.number,
                    episodeNumber = id.number,
                    language = languages.apiLanguage.apiParameter,
                    appendResponses = appendToResponse.ifEmpty { null },
                    includeImageLanguages = "null",
                )
            }
        },
    )
    val details = detailsDownloader("details") { it }
        .extractFromResponse { it }
        .localized(
            language = languages.apiLanguage,
            loadFromCacheFn = { cache -> cache.episodeDetailsCacheQueries::get },
            putInCacheFn = { cache -> cache.episodeDetailsCacheQueries::put },
        )
        .flow(detailsBatchDownloader)
    val images = detailsDownloader("images") { it + AppendResponse.IMAGES }
        .extractNonNullFromResponse(TmdbEpisodeDetail::images)
        .notLocalized(
            loadFromCacheFn = { cache -> cache.episodeImagesCacheQueries::get },
            putInCacheFn = { cache -> cache.episodeImagesCacheQueries::put },
        )
        .flow(detailsBatchDownloader)

    private fun detailsDownloader(
        logTag: String,
        prepareRequest: (List<AppendResponse>) -> List<AppendResponse>,
    ): EpisodeDetailsDownloader {
        return EpisodeDetailsDownloader(
            id = id,
            logTag = "${id.toExternalId().serialize()}-$logTag",
            prepareRequest = prepareRequest,
        )
    }
}
