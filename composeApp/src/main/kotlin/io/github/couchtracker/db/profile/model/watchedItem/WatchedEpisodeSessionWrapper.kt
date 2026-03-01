package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
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

    companion object {
        fun load(
            db: ProfileData,
            selections: List<WatchedItemDimensionSelectionsWrapper>,
        ): List<WatchedEpisodeSessionWrapper> {
            val selectionsById = selections.associateBy { it.id }

            fun findSelections(id: Long) = selectionsById[id] ?: error("Unable to find WatchedItemDimensionSelections with id $id")

            return db.watchedEpisodeSessionQueries.selectAll().executeAsList()
                .map { WatchedEpisodeSessionWrapper(it, findSelections(it.defaultDimensionSelections)) }
        }
    }
}
