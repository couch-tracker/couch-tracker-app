package io.github.couchtracker.db.user.movie

import io.github.couchtracker.db.user.ExternalId

sealed interface ExternalMovieId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalMovieId>(
        inheritors = listOf(TmdbExternalMovieId),
        unknownProvider = ::UnknownExternalMovieId,
    )
}
