package io.github.couchtracker.db.profile.episode

import io.github.couchtracker.db.profile.ExternalId
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId

@JvmInline
value class TmdbExternalEpisodeId(val id: TmdbEpisodeId) : ExternalEpisodeId {

    override val provider get() = Companion.provider
    override val value get() = "${id.showId.value}-${id.seasonId.number}x${id.number}"

    companion object : ExternalId.InheritorsCompanion<TmdbExternalEpisodeId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalEpisodeId {
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

            val episodeIdentifier = TmdbEpisodeId(
                seasonId = TmdbSeasonId(
                    showId = showId,
                    number = seasonNumber.toIntOrNull() ?: partError("season number"),
                ),
                number = episodeNumber.toIntOrNull() ?: partError("season number"),
            )
            return TmdbExternalEpisodeId(episodeIdentifier)
        }
    }
}
