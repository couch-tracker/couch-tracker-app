package io.github.couchtracker.db.profile.season

import io.github.couchtracker.db.profile.ExternalId
import io.github.couchtracker.tmdb.TmdbSeasonId

@JvmInline
value class TmdbExternalSeasonId(val id: TmdbSeasonId) : ExternalSeasonId {

    override val provider get() = Companion.provider
    override val value get() = id.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalSeasonId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalSeasonId {
            return TmdbExternalSeasonId(TmdbSeasonId.ofValue(value))
        }
    }
}
