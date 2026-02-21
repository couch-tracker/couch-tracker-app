package io.github.couchtracker.tmdb

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import io.github.couchtracker.db.common.adapters.map
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalSeasonId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId

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

    override fun toString(): String = "${showId.value}-$number"

    fun episode(number: Int) = TmdbEpisodeId(seasonId = this, number = number)

    fun toExternalId(): ExternalSeasonId = TmdbExternalSeasonId(this)

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbSeasonId, String> = object : ColumnAdapter<TmdbSeasonId, String> {
            override fun decode(databaseValue: String) = ofValue(databaseValue)
            override fun encode(value: TmdbSeasonId) = value.toString()
        }

        fun ofValue(value: String): TmdbSeasonId {
            val (show, seasonNumber) = value.split('-', limit = 2).also {
                require(it.size == 2) { "Invalid serialized TMDB season ID: $value" }
            }
            fun partError(what: String): Nothing {
                throw IllegalArgumentException("Invalid $what in TMDB season ID: $value")
            }

            val showId = TmdbShowId(show.toIntOrNull() ?: partError("show ID"))

            return TmdbSeasonId(
                showId = showId,
                number = seasonNumber.toIntOrNull() ?: partError("season number"),
            )
        }
    }
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

    override fun toString() = "${showId.value}-${seasonId.number}x$number"

    fun toExternalId(): ExternalEpisodeId = TmdbExternalEpisodeId(this)

    companion object {
        val COLUMN_ADAPTER: ColumnAdapter<TmdbEpisodeId, String> = object : ColumnAdapter<TmdbEpisodeId, String> {
            override fun decode(databaseValue: String) = ofValue(databaseValue)
            override fun encode(value: TmdbEpisodeId) = value.toString()
        }

        fun ofValue(value: String): TmdbEpisodeId {
            val (show, rest) = value.split('-', limit = 2).also {
                require(it.size == 2) { "Invalid serialized TMDB episode ID: $value" }
            }
            fun partError(what: String): Nothing {
                throw IllegalArgumentException("Invalid $what in TMDB external episode ID: $value")
            }

            val showId = TmdbShowId(show.toIntOrNull() ?: partError("show ID"))
            val (seasonNumber, episodeNumber) = rest.split('x', limit = 2).also {
                require(it.size == 2) { "Invalid serialized TMDB episode ID: $value" }
            }

            return TmdbEpisodeId(
                seasonId = TmdbSeasonId(
                    showId = showId,
                    number = seasonNumber.toIntOrNull() ?: partError("season number"),
                ),
                number = episodeNumber.toIntOrNull() ?: partError("season number"),
            )
        }
    }
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
