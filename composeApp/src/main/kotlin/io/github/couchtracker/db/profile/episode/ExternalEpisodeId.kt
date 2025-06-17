package io.github.couchtracker.db.profile.episode

import android.os.Parcelable
import io.github.couchtracker.db.profile.ExternalId
import kotlinx.parcelize.Parcelize

/**
 * Any external ID representing an episode.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
@Parcelize
sealed interface ExternalEpisodeId : ExternalId, Parcelable {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalEpisodeId>(
        inheritors = listOf(TmdbExternalEpisodeId),
        unknownProvider = ::UnknownExternalEpisodeId,
    )
}
