package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbRatingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat

data class TmdbRating private constructor(
    val average: Float,
    val count: Int?,
    val formatted: String,
) {
    init {
        require(count == null || count > 0)
        require(average > 0)
    }

    companion object {
        suspend fun ofOrNull(average: Float?, count: Int?): TmdbRating? {
            return when {
                average == null || average < 0 -> null
                count == null && average > 0 || count != null && count > 0 -> of(average, count)
                else -> null
            }
        }

        suspend fun of(average: Float, count: Int?): TmdbRating {
            return withContext(Dispatchers.Default) {
                TmdbRating(
                    average = average,
                    count = count,
                    formatted = NumberFormat.getInstance().apply {
                        minimumFractionDigits = 1
                        maximumFractionDigits = 1
                    }.format(average) + "★",
                )
            }
        }
    }
}

suspend fun TmdbRatingItem.rating() = TmdbRating.ofOrNull(voteAverage, voteCount)
