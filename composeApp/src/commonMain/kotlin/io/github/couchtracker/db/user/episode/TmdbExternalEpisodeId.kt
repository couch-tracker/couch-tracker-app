package io.github.couchtracker.db.user.episode

import io.github.couchtracker.db.user.ExternalId
import io.github.couchtracker.tmdb.TmdbEpisodeId

@JvmInline
value class TmdbExternalEpisodeId(val id: TmdbEpisodeId) : ExternalEpisodeId {

    override val provider get() = Companion.provider
    override val value get() = id.value.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalEpisodeId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalEpisodeId {
            val id = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid TMDB external ID: $value")
            return TmdbExternalEpisodeId(TmdbEpisodeId(id))
        }
    }
}
