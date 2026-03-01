package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbGenreId
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

/**
 * Returns an icon for the given genre, in the form of an emoji
 */
fun TmdbGenre.getIcon(): String? {
    // Suppressing warnings about duplicate branches, as the movie id and show id are the same
    @Suppress("KotlinConstantConditions")
    return when (this.id) {
        // Shared genres
        TmdbGenreId.Movie.ANIMATION, TmdbGenreId.Show.ANIMATION -> "\uD83C\uDFA8"
        TmdbGenreId.Movie.COMEDY, TmdbGenreId.Show.COMEDY -> "\uD83D\uDE06"
        TmdbGenreId.Movie.CRIME, TmdbGenreId.Show.CRIME -> "\uD83D\uDD75\uFE0F"
        TmdbGenreId.Movie.DOCUMENTARY, TmdbGenreId.Show.DOCUMENTARY -> "\uD83C\uDFA5"
        TmdbGenreId.Movie.DRAMA, TmdbGenreId.Show.DRAMA -> "\uD83C\uDFAD"
        TmdbGenreId.Movie.FAMILY, TmdbGenreId.Show.FAMILY -> "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"
        TmdbGenreId.Movie.MYSTERY, TmdbGenreId.Show.MYSTERY -> "\uD83D\uDD0D"
        TmdbGenreId.Movie.WESTERN, TmdbGenreId.Show.WESTERN -> "\uD83E\uDD20"
        // Movie only
        TmdbGenreId.Movie.ACTION -> "\uD83D\uDCA5"
        TmdbGenreId.Movie.ADVENTURE -> "\uD83D\uDDFA\uFE0F"
        TmdbGenreId.Movie.FANTASY -> "\uD83E\uDD84"
        TmdbGenreId.Movie.HISTORY -> "\uD83D\uDCDC"
        TmdbGenreId.Movie.HORROR -> "\uD83D\uDE31"
        TmdbGenreId.Movie.MUSIC -> "\uD83C\uDFB5"
        TmdbGenreId.Movie.ROMANCE -> "\u2764\uFE0F"
        TmdbGenreId.Movie.SCIENCE_FICTION -> "\uD83D\uDE80"
        TmdbGenreId.Movie.TV_MOVIE -> "\uD83D\uDCFA"
        TmdbGenreId.Movie.THRILLER -> "\uD83D\uDD2A"
        TmdbGenreId.Movie.WAR -> "\u2694\uFE0F"
        // Show only
        TmdbGenreId.Show.ACTION_ADVENTURE -> "\uD83D\uDCA5"
        TmdbGenreId.Show.KIDS -> "\uD83E\uDDF8"
        TmdbGenreId.Show.NEWS -> "\uD83D\uDCF0"
        TmdbGenreId.Show.REALITY -> "\uD83D\uDCF7"
        TmdbGenreId.Show.SCIENCE_FICTION_FANTASY -> "\uD83D\uDE80"
        TmdbGenreId.Show.SOAP -> "\uD83D\uDC94"
        TmdbGenreId.Show.TALK -> "\uD83C\uDFA4"
        TmdbGenreId.Show.WAR_POLITICS -> "\uD83C\uDFDB\uFE0F"
        else -> null
    }
}
