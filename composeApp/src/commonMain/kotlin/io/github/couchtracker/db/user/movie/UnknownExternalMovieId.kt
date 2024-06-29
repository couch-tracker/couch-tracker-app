package io.github.couchtracker.db.user.movie

import kotlinx.serialization.Serializable

@Serializable
data class UnknownExternalMovieId(
    override val provider: String,
    override val value: String,
) : ExternalMovieId
