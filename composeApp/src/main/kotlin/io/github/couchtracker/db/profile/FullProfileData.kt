package io.github.couchtracker.db.profile

import android.util.Log
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

private const val LOG_TAG = "FullProfileData"

data class FullProfileData(
    val showCollection: List<ShowInCollection>,
    val watchedItems: List<WatchedItem>,
    val watchedItemDimensions: List<WatchedItemDimensionWrapper>,
) {

    companion object {

        suspend fun load(db: ProfileData, coroutineContext: CoroutineContext = Dispatchers.IO): FullProfileData {
            Log.d(LOG_TAG, "Starting loading full profile data")
            val (data, time) = measureTimedValue {
                withContext(coroutineContext) {
                    FullProfileData(
                        showCollection = db.showInCollectionQueries.selectShowCollection().executeAsList(),
                        watchedItems = db.watchedItemQueries.selectAll().executeAsList(),
                        watchedItemDimensions = WatchedItemDimensionWrapper.load(db),
                    )
                }
            }
            Log.d(LOG_TAG, "Loading full profile data took $time")
            return data
        }
    }
}
