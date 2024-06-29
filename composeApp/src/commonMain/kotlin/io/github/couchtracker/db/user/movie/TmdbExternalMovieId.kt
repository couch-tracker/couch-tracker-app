package io.github.couchtracker.db.user.movie

import io.github.couchtracker.db.user.ExternalId
import io.github.couchtracker.tmdb.TmdbMovieId
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
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
