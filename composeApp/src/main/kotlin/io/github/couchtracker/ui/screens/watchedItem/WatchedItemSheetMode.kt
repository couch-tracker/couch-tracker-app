package io.github.couchtracker.ui.screens.watchedItem

import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedEpisodeSession
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import kotlin.time.Duration

/**
 * Represents the mode to open a [WatchedItemSheetScaffold].
 */
sealed interface WatchedItemSheetMode {

    val itemId: ExternalId
    val watchedItemType: WatchedItemType
    val mediaRuntime: Duration?
    val mediaLanguages: List<Bcp47Language>

    /**
     * A sheet opened with blank data, to create a new watched item.
     */
    sealed interface New : WatchedItemSheetMode {

        fun save(db: ProfileData, watchedItem: WatchedItem)

        data class Movie(
            override val itemId: ExternalMovieId,
            override val mediaRuntime: Duration?,
            override val mediaLanguages: List<Bcp47Language>,
        ) : New {
            override val watchedItemType = WatchedItemType.MOVIE

            override fun save(db: ProfileData, watchedItem: WatchedItem) {
                db.watchedMovieQueries.insert(id = watchedItem.id, movieId = itemId)
            }
        }

        data class Episode(
            override val itemId: ExternalEpisodeId,
            val session: WatchedEpisodeSession,
            override val mediaRuntime: Duration?,
            override val mediaLanguages: List<Bcp47Language>,
        ) : New {
            override val watchedItemType = WatchedItemType.EPISODE

            override fun save(db: ProfileData, watchedItem: WatchedItem) {
                db.watchedEpisodeQueries.insert(id = watchedItem.id, episodeId = itemId, session = session.id)
            }
        }
    }

    /**
     * A sheet opened pre-populated with existing data, to edit an existing watched item.
     */
    data class Edit(
        val watchedItem: WatchedItemWrapper,
        override val watchedItemType: WatchedItemType,
        override val mediaRuntime: Duration?,
        override val mediaLanguages: List<Bcp47Language>,
    ) : WatchedItemSheetMode {
        override val itemId: ExternalId get() = watchedItem.itemId
    }
}
