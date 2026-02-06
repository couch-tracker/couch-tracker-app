package io.github.couchtracker.db.profile.season

data class UnknownExternalSeasonId(
    override val provider: String,
    override val value: String,
) : ExternalSeasonId
