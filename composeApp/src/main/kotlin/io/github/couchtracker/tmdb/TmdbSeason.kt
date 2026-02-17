package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbSeasonDetail
import io.github.couchtracker.utils.api.ApiResult
import kotlinx.coroutines.flow.Flow

fun TmdbSeasonId.details(language: TmdbLanguage): Flow<ApiResult<TmdbSeasonDetail>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "details",
        language = language,
        loadFromCacheFn = { seasonDetailsCacheQueries::get },
        putInCacheFn = { seasonDetailsCacheQueries::put },
        download = { tmdb ->
            tmdb.showSeasons.getDetails(
                showId = showId.value,
                seasonNumber = number,
                language = language.apiParameter,
            )
        },
    )
}
