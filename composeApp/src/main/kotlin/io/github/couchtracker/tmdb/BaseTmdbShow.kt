package io.github.couchtracker.tmdb

import app.moviebase.tmdb.image.TmdbImage
import app.moviebase.tmdb.model.TmdbRatingItem
import app.moviebase.tmdb.model.TmdbShowDetail
import kotlinx.datetime.LocalDate
import app.moviebase.tmdb.model.TmdbShow as ApiTmdbShow

/** Contains show information that can be obtained from the result of any TMDB API call */
data class BaseTmdbShow(
    val key: TmdbBaseCacheKey<TmdbShowId, TmdbLanguage>,
    val name: String?,
    val overview: String?,
    val poster: TmdbImage?,
    val backdrop: TmdbImage?,
    val firstAirDate: LocalDate?,
    val originalName: String?,
    val originCountry: List<String>,
    val originalLanguage: String?,
    val popularity: Float,
    override val voteCount: Int?,
    override val voteAverage: Float?,
) : TmdbRatingItem

fun ApiTmdbShow.toBaseShow(language: TmdbLanguage): BaseTmdbShow {
    return BaseTmdbShow(
        key = TmdbBaseCacheKey(tmdbShowId, language),
        name = name,
        overview = overview,
        poster = posterImage,
        backdrop = backdropImage,
        firstAirDate = firstAirDate,
        originalName = originalName,
        originCountry = originCountry,
        originalLanguage = originalLanguage,
        popularity = popularity,
        voteCount = voteCount,
        voteAverage = voteAverage,
    )
}

fun TmdbShowDetail.toBaseShow(language: TmdbLanguage): BaseTmdbShow {
    return BaseTmdbShow(
        key = TmdbBaseCacheKey(tmdbShowId, language),
        name = name,
        overview = overview,
        poster = posterImage,
        backdrop = backdropImage,
        firstAirDate = firstAirDate,
        originalName = originalName,
        originCountry = originCountry,
        originalLanguage = originalLanguage,
        popularity = popularity,
        voteCount = voteCount,
        voteAverage = voteAverage,
    )
}
