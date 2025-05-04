package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbRatingItem
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

fun TmdbRatingItem.rating(): TmdbRating? {
    val count = voteCount?.takeIf { it > 0 }
    val average = voteAverage
    return if (count != null && average != null) {
        TmdbRating(average, count)
    } else {
        null
    }
}
