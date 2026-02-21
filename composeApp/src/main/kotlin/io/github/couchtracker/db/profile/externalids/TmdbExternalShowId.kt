package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.tmdb.TmdbShowId

@JvmInline
value class TmdbExternalShowId(val id: TmdbShowId) : ExternalShowId {

    override val provider get() = Companion.provider
    override val value get() = id.value.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalShowId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalShowId {
            val id = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid TMDB external ID: $value")
            return TmdbExternalShowId(TmdbShowId(id))
        }
    }
}
