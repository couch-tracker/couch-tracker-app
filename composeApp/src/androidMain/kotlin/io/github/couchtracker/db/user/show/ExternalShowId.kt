package io.github.couchtracker.db.user.show

import io.github.couchtracker.db.user.ExternalId

sealed interface ExternalShowId : ExternalId {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalShowId>(
        inheritors = listOf(TmdbExternalShowId),
        unknownProvider = ::UnknownExternalShowId,
    )
}
