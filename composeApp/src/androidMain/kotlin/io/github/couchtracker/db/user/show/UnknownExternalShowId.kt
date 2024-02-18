package io.github.couchtracker.db.user.show

data class UnknownExternalShowId(
    override val type: String,
    override val value: String,
) : ExternalShowId
