package io.github.couchtracker.tmdb

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import io.github.couchtracker.db.common.map
import io.github.couchtracker.db.user.movie.ExternalMovieId
import io.github.couchtracker.db.user.movie.TmdbExternalMovieId
import io.github.couchtracker.db.user.show.ExternalShowId
import io.github.couchtracker.db.user.show.TmdbExternalShowId

@JvmInline
value class TmdbMovieId(val value: Int) {
    init {
        requireTmdbId(value)
    }

    fun toExternalId(): ExternalMovieId = TmdbExternalMovieId(this)

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbMovieId, Long> = IntColumnAdapter.map(
            encoder = { it.value },
            decoder = { TmdbMovieId(it) },
        )
    }
}

@JvmInline
value class TmdbShowId(val value: Int) {
    init {
        requireTmdbId(value)
    }

    fun toExternalId(): ExternalShowId = TmdbExternalShowId(this)

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbShowId, Long> = IntColumnAdapter.map(
            encoder = { it.value },
            decoder = { TmdbShowId(it) },
        )
    }
}

/**
 * Throws [IllegalArgumentException] when the given [id] is not a valid TMDB ID.
 */
private fun requireTmdbId(id: Int) {
    require(id > 0) { "Invalid non-positive TMDB id: $id" }
}
