package io.github.couchtracker.db.user.show

data class UnknownExternalShowId(
    override val provider: String,
    override val value: String,
) : ExternalShowId
