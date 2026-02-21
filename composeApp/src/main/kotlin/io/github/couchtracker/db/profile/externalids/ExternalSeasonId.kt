package io.github.couchtracker.db.profile.externalids

/**
 * Any external ID representing a season.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
sealed interface ExternalSeasonId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalSeasonId>(
        inheritors = listOf(TmdbExternalSeasonId.Companion),
        unknownProvider = ::UnknownExternalSeasonId,
    )
}
