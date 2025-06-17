package io.github.couchtracker.db.profile.movie

import android.os.Parcelable
import io.github.couchtracker.db.profile.ExternalId
import kotlinx.parcelize.Parcelize

/**
 * Any external ID representing a movie.
 *
 * TODO: create specification for "official" supported providers names and their value format, and link it here.
 *
 * @see ExternalId
 */
@Parcelize
sealed interface ExternalMovieId : ExternalId, Parcelable {

    companion object : ExternalId.SealedInterfacesCompanion<ExternalMovieId>(
        inheritors = listOf(TmdbExternalMovieId),
        unknownProvider = ::UnknownExternalMovieId,
    )
}
