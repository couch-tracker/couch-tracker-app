package io.github.couchtracker.db.user.show

import io.github.couchtracker.db.user.ExternalId
import io.github.couchtracker.db.user.requireTmdbId

@JvmInline
value class TmdbExternalShowId(val id: Long) : ExternalShowId {

    init {
        requireTmdbId(id)
    }

    override val provider get() = Companion.provider
    override val value get() = id.toString()

    companion object : ExternalId.InheritorsCompanion<TmdbExternalShowId> {
        override val provider = "tmdb"

        override fun ofValue(value: String): TmdbExternalShowId {
            return TmdbExternalShowId(value.toLongOrNull() ?: throw IllegalArgumentException("Invalid TMDB external ID: $value"))
        }
    }
}
