package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbReleaseDates
import app.moviebase.tmdb.model.TmdbVideo
import io.github.couchtracker.utils.api.ApiResult
import kotlinx.coroutines.flow.Flow
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

fun TmdbMovieId.details(language: TmdbLanguage): Flow<ApiResult<TmdbMovieDetail>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "details",
        language = language,
        loadFromCacheFn = { movieDetailsCacheQueries::get },
        putInCacheFn = { movieDetailsCacheQueries::put },
        download = { tmdb ->
            tmdb.movies.getDetails(
                movieId = value,
                language = language.apiParameter,
            )
        },
    )
}

fun TmdbMovieId.releaseDates(): Flow<ApiResult<List<TmdbReleaseDates>>> {
    return tmdbCachedDownload(
        id = this,
        logTag = "release_dates",
        loadFromCacheFn = { movieReleaseDatesCacheQueries::get },
        putInCacheFn = { movieReleaseDatesCacheQueries::put },
        download = { tmdb ->
            tmdb.movies.getReleaseDates(value).results
        },
    )
}

fun TmdbMovieId.credits(language: TmdbLanguage): Flow<ApiResult<TmdbCredits>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "credits",
        language = language,
        loadFromCacheFn = { movieCreditsCacheQueries::get },
        putInCacheFn = { movieCreditsCacheQueries::put },
        download = { tmdb ->
            tmdb.movies.getCredits(
                movieId = value,
                language = language.apiParameter,
            )
        },
    )
}

fun TmdbMovieId.images(languages: TmdbLanguagesFilter): Flow<ApiResult<TmdbImages>> {
    return tmdbLocalizedCachedDownload(
        id = this,
        logTag = "images",
        language = languages,
        loadFromCacheFn = { movieImagesCacheQueries::get },
        putInCacheFn = { movieImagesCacheQueries::put },
        download = { tmdb ->
            tmdb.movies.getImages(
                movieId = value,
                includeImageLanguage = languages.apiParameter(),
            )
        },
    )
}

fun TmdbMovieId.videos(): Flow<ApiResult<List<TmdbVideo>>> {
    // TODO: make localized
    return tmdbCachedDownload(
        id = this,
        logTag = "videos",
        loadFromCacheFn = { movieVideosCacheQueries::get },
        putInCacheFn = { movieVideosCacheQueries::put },
        download = { tmdb ->
            tmdb.movies.getVideos(value).results
        },
    )
}

val TmdbMovieDetail.tmdbMovieId get() = TmdbMovieId(id)
val ApiTmdbMovie.tmdbMovieId get() = TmdbMovieId(id)
