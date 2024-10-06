package io.github.couchtracker.db.user

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemDimensionWrapper
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTimedValue

private const val LOG_TAG = "FullUserData"

data class FullUserData(
    val showCollection: List<ShowInCollection>,
    val watchedItemDimensions: List<WatchedItemDimensionWrapper>,
    val watchedItems: List<WatchedItemWrapper>,
) {

    companion object {

        suspend fun load(db: UserData, coroutineContext: CoroutineContext = Dispatchers.Default): FullUserData {
            Log.d(LOG_TAG, "Starting loading full user data")
            val (data, time) = measureTimedValue {
                val watchedItemDimensions = WatchedItemDimensionWrapper.wrapAll(
                    dimensions = db.watchedItemDimensionQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                    choices = db.watchedItemDimensionChoiceQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                )
                FullUserData(
                    showCollection = db.showInCollectionQueries.selectShowCollection().asFlow().mapToList(coroutineContext).first(),
                    watchedItemDimensions = watchedItemDimensions,
                    watchedItems = WatchedItemWrapper.wrapAll(
                        watchedItems = db.watchedItemQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                        dimensions = watchedItemDimensions,
                        choices = db.watchedItemChoiceQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                        freeTexts = db.watchedItemFreeTextQueries.selectAll().asFlow().mapToList(coroutineContext).first(),
                    ),
                )
            }
            Log.d(LOG_TAG, "Loading full user data took $time")
            return data
        }
    }
}
