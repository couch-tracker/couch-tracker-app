package io.github.couchtracker.db.profile.externalids

data class UnknownExternalMovieId(
    override val provider: String,
    override val value: String,
) : ExternalMovieId
