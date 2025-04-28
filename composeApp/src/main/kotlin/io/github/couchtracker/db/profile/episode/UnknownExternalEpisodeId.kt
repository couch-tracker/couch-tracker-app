package io.github.couchtracker.db.profile.episode

data class UnknownExternalEpisodeId(
    override val provider: String,
    override val value: String,
) : ExternalEpisodeId
