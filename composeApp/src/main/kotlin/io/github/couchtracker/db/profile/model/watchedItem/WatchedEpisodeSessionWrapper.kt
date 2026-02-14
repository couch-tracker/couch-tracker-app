package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.WatchedEpisodeSession

data class WatchedEpisodeSessionWrapper(
    private val watchedEpisodeSession: WatchedEpisodeSession,
    val defaultDimensionSelections: WatchedItemDimensionSelectionsWrapper,
) {

    val id get() = watchedEpisodeSession.id
    val showId get() = watchedEpisodeSession.showId
    val name get() = watchedEpisodeSession.name
    val description get() = watchedEpisodeSession.description
    val isActive get() = watchedEpisodeSession.isActive

    init {
        require(defaultDimensionSelections.id == watchedEpisodeSession.defaultDimensionSelections) {
            "WatchedEpisodeSession defaultDimensionSelections and given selections wrapper must match"
        }
    }
}
