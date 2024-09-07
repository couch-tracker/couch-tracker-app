package io.github.couchtracker.db.user

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

private const val LOG_TAG = "FullUserData"

data class FullUserData(
    val showCollection: List<ShowInCollection>,
    val watchedItems: List<WatchedItem>,
) {

    companion object {

        suspend fun load(db: UserData, coroutineContext: CoroutineContext = Dispatchers.Default): FullUserData {
            Log.d(LOG_TAG, "Starting loading full user data")
            val (data, time) = measureTimedValue {
                FullUserData(
                    showCollection = db.showInCollectionQueries.selectShowCollection().asFlow().mapToList(coroutineContext).first(),
                    watchedItems = db.watchedItemQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                )
            }
            Log.d(LOG_TAG, "Loading full user data took $time")
            return data
        }
    }
}
