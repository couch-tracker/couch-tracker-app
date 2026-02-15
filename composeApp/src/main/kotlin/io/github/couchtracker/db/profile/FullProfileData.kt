package io.github.couchtracker.db.profile

import android.util.Log
import io.github.couchtracker.db.profile.externalids.BookmarkableExternalId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelectionsWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

private const val LOG_TAG = "FullProfileData"

data class FullProfileData(
    val bookmarkedItems: Map<BookmarkableExternalId, BookmarkedItem>,
    val watchedItems: List<WatchedItemWrapper>,
    val watchedEpisodeSessions: Map<ExternalShowId, List<WatchedEpisodeSessionWrapper>>,
    val watchedItemDimensions: List<WatchedItemDimensionWrapper>,
) {

    val watchedEpisodesBySession: Map<WatchedEpisodeSessionWrapper, List<WatchedItemWrapper.Episode>> = watchedItems
        .filterIsInstance<WatchedItemWrapper.Episode>()
        .groupBy { it.session }

    companion object {

        suspend fun load(db: ProfileData, coroutineContext: CoroutineContext = Dispatchers.IO): FullProfileData {
            Log.d(LOG_TAG, "Starting loading full profile data")
            val (data, time) = measureTimedValue {
                withContext(coroutineContext) {
                    val watchedItemDimensions = WatchedItemDimensionWrapper.load(db)
                    val watchedItemDimensionSelections = WatchedItemDimensionSelectionsWrapper.load(db, watchedItemDimensions)
                    val watchedEpisodeSessions = WatchedEpisodeSessionWrapper.load(db, watchedItemDimensionSelections)
                    FullProfileData(
                        bookmarkedItems = db.bookmarkedItemQueries.selectAll().executeAsList().associateBy { it.itemId },
                        watchedItems = WatchedItemWrapper.load(db, watchedItemDimensionSelections, watchedEpisodeSessions),
                        watchedEpisodeSessions = watchedEpisodeSessions.groupBy { it.showId },
                        watchedItemDimensions = watchedItemDimensions,
                    )
                }
            }
            Log.d(LOG_TAG, "Loading full profile data took $time")
            return data
        }
    }
}
