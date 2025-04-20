package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbMovieDetail
import java.text.NumberFormat

data class TmdbRating(
    val average: Float,
    val count: Int,
) {
    init {
        require(count > 0)
    }

    // TODO put in composable function
    fun format() = NumberFormat.getInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(average) + "★"
}

fun TmdbMovieDetail.rating(): TmdbRating? {
    return if (voteCount > 0) {
        TmdbRating(voteAverage, voteCount)
    } else {
        null
    }
}
