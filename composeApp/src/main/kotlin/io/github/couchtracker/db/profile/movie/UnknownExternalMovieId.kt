package io.github.couchtracker.db.profile.movie

data class UnknownExternalMovieId(
    override val provider: String,
    override val value: String,
) : ExternalMovieId
