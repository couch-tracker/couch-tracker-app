package io.github.couchtracker.db.user.movie

data class UnknownExternalMovieId(
    override val type: String,
    override val value: String,
) : ExternalMovieId
