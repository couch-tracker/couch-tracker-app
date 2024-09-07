package io.github.couchtracker.db.user.episode

import io.github.couchtracker.db.user.ExternalId

/**
 * Any external ID representing an episode.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
sealed interface ExternalEpisodeId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalEpisodeId>(
        inheritors = listOf(TmdbExternalEpisodeId),
        unknownProvider = ::UnknownExternalEpisodeId,
    )
}
