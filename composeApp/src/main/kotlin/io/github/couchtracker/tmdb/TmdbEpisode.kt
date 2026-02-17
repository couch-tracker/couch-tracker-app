package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbEpisodeDetail
import app.moviebase.tmdb.model.TmdbImages
import io.github.couchtracker.utils.api.ApiException
import io.github.couchtracker.utils.api.ApiResult
import kotlinx.coroutines.flow.Flow

fun TmdbEpisodeId.details(language: TmdbLanguage): Flow<ApiResult<TmdbEpisodeDetail>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "details",
        language = language,
        loadFromCacheFn = { episodeDetailsCacheQueries::get },
        putInCacheFn = { episodeDetailsCacheQueries::put },
        download = { tmdb ->
            tmdb.showEpisodes.getDetails(
                showId = showId.value,
                seasonNumber = seasonId.number,
                episodeNumber = number,
                language = language.apiParameter,
            )
        },
    )
}

fun TmdbEpisodeId.images(languages: TmdbLanguagesFilter): Flow<ApiResult<TmdbImages>> {
    // TODO: use https://developer.themoviedb.org/reference/tv-episode-images
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "images",
        language = languages,
        loadFromCacheFn = { episodeImagesCacheQueries::get },
        putInCacheFn = { episodeImagesCacheQueries::put },
        download = { tmdb ->
            tmdb.showEpisodes.getDetails(
                showId = showId.value,
                seasonNumber = seasonId.number,
                episodeNumber = number,
                includeImageLanguages = languages.apiParameter(),
                appendResponses = listOf(AppendResponse.IMAGES),
            ).images ?: throw ApiException.DeserializationError("Unexpected null field images", cause = null)
        },
    )
}
