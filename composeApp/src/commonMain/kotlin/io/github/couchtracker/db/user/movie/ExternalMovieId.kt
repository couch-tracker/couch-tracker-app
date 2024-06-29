package io.github.couchtracker.db.user.movie

import io.github.couchtracker.db.user.ExternalId
import kotlinx.serialization.Serializable

/**
 * Any external ID representing a movie.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
@Serializable
sealed interface ExternalMovieId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalMovieId>(
        inheritors = listOf(TmdbExternalMovieId),
        unknownProvider = ::UnknownExternalMovieId,
    )
}
