package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbAggregateCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.utils.api.ApiResult
import kotlinx.coroutines.flow.Flow
import app.moviebase.tmdb.model.TmdbShow as ApiTmdbShow

fun TmdbShowId.details(language: TmdbLanguage): Flow<ApiResult<TmdbShowDetail>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "details",
        language = language,
        loadFromCacheFn = { showDetailsCacheQueries::get },
        putInCacheFn = { showDetailsCacheQueries::put },
        download = { tmdb ->
            tmdb.show.getDetails(
                showId = value,
                language = language.apiParameter,
            )
        },
    )
}

fun TmdbShowId.images(languages: TmdbLanguagesFilter): Flow<ApiResult<TmdbImages>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "images",
        language = languages,
        loadFromCacheFn = { showImagesCacheQueries::get },
        putInCacheFn = { showImagesCacheQueries::put },
        download = { tmdb ->
            tmdb.show.getImages(
                showId = value,
                includeImageLanguage = languages.apiParameter(),
            )
        },
    )
}

fun TmdbShowId.aggregateCredits(language: TmdbLanguage): Flow<ApiResult<TmdbAggregateCredits>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "aggregate_credits",
        language = language,
        loadFromCacheFn = { showAggregateCreditsCacheQueries::get },
        putInCacheFn = { showAggregateCreditsCacheQueries::put },
        download = { tmdb ->
            tmdb.show.getAggregateCredits(
                showId = value,
                language = language.apiParameter,
            )
        },
    )
}

val ApiTmdbShow.tmdbShowId get() = TmdbShowId(id)
val TmdbShowDetail.tmdbShowId get() = TmdbShowId(id)
