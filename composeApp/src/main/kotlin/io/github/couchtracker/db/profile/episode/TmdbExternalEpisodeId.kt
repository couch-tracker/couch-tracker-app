package io.github.couchtracker.db.profile.episode

import io.github.couchtracker.db.profile.ExternalId
import io.github.couchtracker.tmdb.TmdbEpisodeId

@JvmInline
value class TmdbExternalEpisodeId(val id: TmdbEpisodeId) : ExternalEpisodeId {

    override val provider get() = Companion.provider
    override val value get() = id.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalEpisodeId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalEpisodeId {
            return TmdbExternalEpisodeId(TmdbEpisodeId.ofValue(value))
        }
    }
}
