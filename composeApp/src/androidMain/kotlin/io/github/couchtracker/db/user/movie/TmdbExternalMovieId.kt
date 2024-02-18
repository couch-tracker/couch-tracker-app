package io.github.couchtracker.db.user.movie

import io.github.couchtracker.db.user.ExternalId
import io.github.couchtracker.db.user.requireTmdbId

@JvmInline
value class TmdbExternalMovieId(val id: Long) : ExternalMovieId {

    init {
        requireTmdbId(id)
    }

    override val type get() = Companion.type
    override val value get() = id.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalMovieId> {
        override val type = "tmdb"

        override fun ofValue(value: String): TmdbExternalMovieId {
            return TmdbExternalMovieId(value.toLongOrNull() ?: throw IllegalArgumentException("Invalid TMDB external ID: $value"))
        }
    }
}
