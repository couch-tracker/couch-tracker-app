package io.github.couchtracker.db.profile

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

private const val LOG_TAG = "FullProfileData"

data class FullProfileData(
    val showCollection: List<ShowInCollection>,
    val watchedItems: List<WatchedItem>,
) {

    companion object {

        suspend fun load(db: ProfileData, coroutineContext: CoroutineContext = Dispatchers.Default): FullProfileData {
            Log.d(LOG_TAG, "Starting loading full profile data")
            val (data, time) = measureTimedValue {
                FullProfileData(
                    showCollection = db.showInCollectionQueries.selectShowCollection().asFlow().mapToList(coroutineContext).first(),
                    watchedItems = db.watchedItemQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                )
            }
            Log.d(LOG_TAG, "Loading full profile data took $time")
            return data
        }
    }
}
