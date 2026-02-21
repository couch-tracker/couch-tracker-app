package io.github.couchtracker.db.profile.externalids

data class UnknownExternalSeasonId(
    override val provider: String,
    override val value: String,
) : ExternalSeasonId
