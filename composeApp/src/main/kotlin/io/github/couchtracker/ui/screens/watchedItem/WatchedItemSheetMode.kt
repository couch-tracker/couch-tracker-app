package io.github.couchtracker.ui.screens.watchedItem

import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper

/**
 * Represents the mode to open a [WatchedItemSheetScaffold].
 */
sealed interface WatchedItemSheetMode {

    val itemId: ExternalId

    /**
     * A sheet opened with blank data, to create a new watched item.
     */
    sealed interface New : WatchedItemSheetMode {

        data class Movie(override val itemId: ExternalMovieId) : New

        data class Episode(
            override val itemId: ExternalEpisodeId,
            val showId: ExternalShowId,
            val sessions: List<WatchedEpisodeSessionWrapper>,
        ) : New {
            init {
                require(sessions.all { it.showId == showId })
            }
        }
    }

    /**
     * A sheet opened pre-populated with existing data, to edit an existing watched item.
     */
    data class Edit(val watchedItem: WatchedItemWrapper) : WatchedItemSheetMode {
        override val itemId: ExternalId get() = watchedItem.itemId
    }
}
