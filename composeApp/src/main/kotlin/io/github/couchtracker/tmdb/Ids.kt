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
import io.github.couchtracker.tmdb.TmdbEpisodeId

sealed interface TmdbId

sealed interface TmdbIntId : TmdbId {
    val value: Int
}

@JvmInline
value class TmdbMovieId(override val value: Int) : TmdbIntId {
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
value class TmdbShowId(override val value: Int) : TmdbIntId {
    init {
        requireTmdbId(value)
    }

    fun toExternalId(): ExternalShowId = TmdbExternalShowId(this)

    fun season(number: Int) = TmdbSeasonId(showId = this, number = number)

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbShowId, Long> = IntColumnAdapter.map(
            encoder = { it.value },
            decoder = { TmdbShowId(it) },
        )
    }
}

data class TmdbSeasonId(val showId: TmdbShowId, val number: Int) : TmdbId {

    init {
        require(number >= 0) { "Season number cannot be negative, $number given" }
    }

    fun episode(number: Int) = TmdbEpisodeId(seasonId = this, number = number)
}

data class TmdbEpisodeId(val seasonId: TmdbSeasonId, val number: Int) : TmdbId {

    val showId: TmdbShowId get() = seasonId.showId

    init {
        require(number > 0) { "Episode number cannot be negative, $number given" }
    }

    constructor(showId: Int, seasonNumber: Int, episodeNumber: Int) : this(
        seasonId = TmdbSeasonId(showId = TmdbShowId(showId), number = seasonNumber),
        number = episodeNumber,
    )

    fun toExternalId(): ExternalEpisodeId = TmdbExternalEpisodeId(this)
}

@JvmInline
value class TmdbPersonId(override val value: Int) : TmdbIntId {
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
