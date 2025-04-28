package io.github.couchtracker.db.profile.show

data class UnknownExternalShowId(
    override val provider: String,
    override val value: String,
) : ExternalShowId
