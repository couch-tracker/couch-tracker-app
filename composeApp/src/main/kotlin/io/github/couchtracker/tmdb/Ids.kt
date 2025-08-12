package io.github.couchtracker.tmdb

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import io.github.couchtracker.db.common.adapters.map
import io.github.couchtracker.db.profile.episode.ExternalEpisodeId
import io.github.couchtracker.db.profile.episode.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.db.profile.movie.TmdbExternalMovieId
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.db.profile.show.TmdbExternalShowId

sealed interface TmdbId {
    val value: Int
}

@JvmInline
value class TmdbMovieId(override val value: Int) : TmdbId {
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
value class TmdbShowId(override val value: Int) : TmdbId {
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

@JvmInline
value class TmdbEpisodeId(override val value: Int) : TmdbId {
    init {
        requireTmdbId(value)
    }

    fun toExternalId(): ExternalEpisodeId = TmdbExternalEpisodeId(this)

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbEpisodeId, Long> = IntColumnAdapter.map(
            encoder = { it.value },
            decoder = { TmdbEpisodeId(it) },
        )
    }
}

@JvmInline
value class TmdbPersonId(override val value: Int) : TmdbId {
    init {
        requireTmdbId(value)
    }

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbPersonId, Long> = IntColumnAdapter.map(
            encoder = { it.value },
            decoder = { TmdbPersonId(it) },
        )
    }
}

/**
 * Throws [IllegalArgumentException] when the given [id] is not a valid TMDB ID.
 */
private fun requireTmdbId(id: Int) {
    require(id > 0) { "Invalid non-positive TMDB id: $id" }
}
