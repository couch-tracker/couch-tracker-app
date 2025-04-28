package io.github.couchtracker.db.profile.movie

import io.github.couchtracker.db.profile.ExternalId
import io.github.couchtracker.tmdb.TmdbMovieId

@JvmInline
value class TmdbExternalMovieId(val id: TmdbMovieId) : ExternalMovieId {

    override val provider get() = Companion.provider
    override val value get() = id.value.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalMovieId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalMovieId {
            val id = value.toIntOrNull() ?: throw IllegalArgumentException("Invalid TMDB external ID: $value")
            return TmdbExternalMovieId(TmdbMovieId(id))
        }
    }
}
