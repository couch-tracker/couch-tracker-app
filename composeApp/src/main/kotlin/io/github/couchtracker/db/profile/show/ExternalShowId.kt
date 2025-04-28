package io.github.couchtracker.db.profile.show

import io.github.couchtracker.db.profile.ExternalId

/**
 * Any external ID representing a show.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
sealed interface ExternalShowId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalShowId>(
        inheritors = listOf(TmdbExternalShowId),
        unknownProvider = ::UnknownExternalShowId,
    )
}
