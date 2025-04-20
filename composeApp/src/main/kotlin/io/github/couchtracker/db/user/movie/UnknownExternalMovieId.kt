package io.github.couchtracker.db.user.movie

data class UnknownExternalMovieId(
    override val provider: String,
    override val value: String,
) : ExternalMovieId
