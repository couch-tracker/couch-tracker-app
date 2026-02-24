package io.github.couchtracker.db.profile.externalids

/**
 * Any external ID representing a movie.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
sealed interface ExternalMovieId : ExternalId, WatchableExternalId, BookmarkableExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalMovieId>(
        typeName = "movie",
        inheritors = listOf(TmdbExternalMovieId),
        unknownProvider = ::UnknownExternalMovieId,
    )
}
