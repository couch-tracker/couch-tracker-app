package io.github.couchtracker.tmdb

import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.model.TmdbMovieDetail
import app.moviebase.tmdb.model.TmdbRatingItem
import kotlinx.datetime.LocalDate
import app.moviebase.tmdb.model.TmdbMovie as ApiTmdbMovie

/** Contains movie information that can be obtained from the result of any TMDB API call */
data class BaseTmdbMovie(
    val key: TmdbBaseCacheKey<TmdbMovieId>,
    val title: String,
    val overview: String?,
    val poster: TmdbImage?,
    val backdrop: TmdbImage?,
    val adult: Boolean,
    val releaseDate: LocalDate?,
    val originalTitle: String?,
    val popularity: Float,
    override val voteCount: Int,
    override val voteAverage: Float,
) : TmdbRatingItem

fun ApiTmdbMovie.toBaseMovie(language: TmdbLanguage): BaseTmdbMovie {
    return BaseTmdbMovie(
        key = TmdbBaseCacheKey(tmdbMovieId, language),
        title = title,
        overview = overview,
        poster = posterImage,
        backdrop = backdropImage,
        adult = adult,
        releaseDate = releaseDate,
        originalTitle = originalTitle,
        popularity = popularity,
        voteCount = voteCount,
        voteAverage = voteAverage,
    )
}

fun TmdbMovieDetail.toBaseMovie(language: TmdbLanguage): BaseTmdbMovie {
    return BaseTmdbMovie(
        key = TmdbBaseCacheKey(tmdbMovieId, language),
        title = title,
        overview = overview,
        poster = posterImage,
        backdrop = backdropImage,
        adult = adult,
        releaseDate = releaseDate,
        originalTitle = originalTitle,
        popularity = popularity,
        voteCount = voteCount,
        voteAverage = voteAverage,
    )
}
