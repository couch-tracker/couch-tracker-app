package io.github.couchtracker.db.profile.externalids

/**
 * Any external ID representing a show.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
sealed interface ExternalShowId : ExternalId, BookmarkableExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalShowId>(
        typeName = "show",
        inheritors = listOf(TmdbExternalShowId),
        unknownProvider = ::UnknownExternalShowId,
    )
}
