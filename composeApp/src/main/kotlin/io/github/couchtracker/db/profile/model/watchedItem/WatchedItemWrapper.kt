package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedItemChoice

/**
 * Wrapper around a [WatchedItem]. It's useful to also capture the values for all dimensions associated with it.
 */
data class WatchedItemWrapper(
    val item: WatchedItem,
    val dimensions: List<WatchedItemDimensionSelection<*>>,
) {

    val id get() = item.id
    val addedAt get() = item.addedAt
    val itemId get() = item.itemId
    val watchAt get() = item.watchAt

    fun delete(db: ProfileData) {
        db.watchedItemQueries.delete(item.id)
    }

    companion object {

        /**
         * Loads all the necessary information and wraps each [WatchedItem] in [WatchedItemWrapper].
         *
         * @param dimensions list of all dimensions present in the database, see [WatchedItemDimensionWrapper.load]
         */
        fun load(
            db: ProfileData,
            dimensions: List<WatchedItemDimensionWrapper>,
        ): List<WatchedItemWrapper> {
            val choices = db.watchedItemChoiceQueries.selectAllWithDimensionId().executeAsList()
                .groupBy { it.watchedItem to it.watchedItemDimension }
                .mapValues { (_, list) ->
                    list.map { WatchedItemChoice(watchedItem = it.watchedItem, choice = it.choice) }
                }
            val freeTexts = db.watchedItemFreeTextQueries.selectAll().executeAsList()
                .associateBy { it.watchedItem to it.watchedItemDimension }
            val languages = db.watchedItemLanguageQueries.selectAll().executeAsList()
                .associateBy { it.watchedItem to it.watchedItemDimension }

            return db.watchedItemQueries.selectAll().executeAsList().map { watchedItem ->
                WatchedItemWrapper(
                    item = watchedItem,
                    dimensions = dimensions.map { dimension ->
                        val key = watchedItem.id to dimension.id
                        when (dimension) {
                            is WatchedItemDimensionWrapper.Choice -> dimension.selection(choices[key].orEmpty())
                            is WatchedItemDimensionWrapper.Language -> dimension.selection(languages[key])
                            is WatchedItemDimensionWrapper.FreeText -> dimension.selection(freeTexts[key])
                        }
                    },
                )
            }
        }
    }
}
