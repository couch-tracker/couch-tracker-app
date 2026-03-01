package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbGenre
import io.github.couchtracker.utils.api.ApiResult
import kotlinx.coroutines.flow.Flow

fun movieGenres(language: TmdbLanguage): Flow<ApiResult<List<TmdbGenre>>> {
    return tmdbLocalizedCachedDownload(
        logTag = "movie-genres",
        language = language,
        loadFromCacheFn = { movieGenresCacheQueries::get },
        putInCacheFn = { movieGenresCacheQueries::put },
        download = { tmdb ->
            tmdb.genres.getMovieList(
                language = language.apiParameter,
            ).genres
        },
    )
}

fun tvGenres(language: TmdbLanguage): Flow<ApiResult<List<TmdbGenre>>> {
    return tmdbLocalizedCachedDownload(
        logTag = "tv-genres",
        language = language,
        loadFromCacheFn = { tvGenresCacheQueries::get },
        putInCacheFn = { tvGenresCacheQueries::put },
        download = { tmdb ->
            tmdb.genres.getTvList(
                language = language.apiParameter,
            ).genres
        },
    )
}
