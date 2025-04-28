package io.github.couchtracker.db.profile.movie

import io.github.couchtracker.db.profile.ExternalId

/**
 * Any external ID representing a movie.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
sealed interface ExternalMovieId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalMovieId>(
        inheritors = listOf(TmdbExternalMovieId),
        unknownProvider = ::UnknownExternalMovieId,
    )
}
