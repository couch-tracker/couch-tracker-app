package io.github.couchtracker.db.profile.externalids

data class UnknownExternalEpisodeId(
    override val provider: String,
    override val value: String,
) : ExternalEpisodeId
