package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbRatingItem
import io.github.couchtracker.utils.deferredConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat

private val RATING_FORMATTER = deferredConstant {
    NumberFormat.getInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
}

data class TmdbRating(
    val average: Float,
    val count: Int,
    val formatted: String,
) {
    init {
        require(count > 0)
    }

    companion object {
        suspend fun of(average: Float, count: Int): TmdbRating {
            return withContext(Dispatchers.Default) {
                TmdbRating(
                    average = average,
                    count = count,
                    formatted = RATING_FORMATTER.await().format(average) + "★",
                )
            }
        }
    }
}

suspend fun TmdbRatingItem.rating(): TmdbRating? {
    val count = voteCount?.takeIf { it > 0 }
    val average = voteAverage
    return if (count != null && average != null) {
        TmdbRating.of(average, count)
    } else {
        null
    }
}
