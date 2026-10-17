package io.github.couchtracker.tmdb

import androidx.annotation.VisibleForTesting
import app.moviebase.tmdb.model.TmdbRatingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.floor

private const val MAX_TMDB_RATING = 10f

data class TmdbRating @VisibleForTesting constructor(
    val average: Float,
    val count: Int?,
    val formatted: String,
) : Comparable<TmdbRating> {

    init {
        require(count == null || count > 0)
        require(average in 0f..MAX_TMDB_RATING)
    }

    override fun compareTo(other: TmdbRating): Int {
        return if (average != other.average) {
            average.compareTo(other.average)
        } else {
            (count ?: 0).compareTo(other.count ?: 0)
        }
    }

    companion object {
        suspend fun ofOrNull(average: Float?, count: Int?): TmdbRating? {
            return when {
                average == null || !average.isFinite() -> null
                average == 0f && count != null && count > 0 -> of(average, count)
                average <= 0 || average > MAX_TMDB_RATING -> null
                count == null || count <= 0 -> of(average, null)
                else -> of(average, count)
            }
        }

        private suspend fun of(average: Float, count: Int?): TmdbRating {
            require(average.isFinite())
            return withContext(Dispatchers.Default) {
                // e.g. 7.999 becomes 7.9
                @Suppress("MagicNumber")
                val roundedDown = floor(average * 10) / 10
                TmdbRating(
                    average = average.coerceIn(0f, MAX_TMDB_RATING),
                    count = count,
                    formatted = NumberFormat.getInstance().apply {
                        minimumFractionDigits = 1
                        maximumFractionDigits = 1
                    }.format(roundedDown) + "★",
                )
            }
        }
    }
}

suspend fun TmdbRatingItem.rating() = TmdbRating.ofOrNull(voteAverage, voteCount)
