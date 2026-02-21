package io.github.couchtracker.ui.screens.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedEpisodeSession
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
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

        fun save(db: ProfileData, watchedItem: WatchedItem)

        data class Movie(override val itemId: ExternalMovieId) : New {

            override fun save(db: ProfileData, watchedItem: WatchedItem) {
                db.watchedMovieQueries.insert(id = watchedItem.id, movieId = itemId)
            }
        }

        data class Episode(override val itemId: ExternalEpisodeId, val session: WatchedEpisodeSession) : New {

            override fun save(db: ProfileData, watchedItem: WatchedItem) {
                db.watchedEpisodeQueries.insert(id = watchedItem.id, episodeId = itemId, session = session.id)
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
