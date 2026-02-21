package io.github.couchtracker.db.profile.externalids

data class UnknownExternalShowId(
    override val provider: String,
    override val value: String,
) : ExternalShowId
