package io.github.couchtracker.db.user.episode

data class UnknownExternalEpisodeId(
    override val provider: String,
    override val value: String,
) : ExternalEpisodeId
