package io.github.couchtracker.db.user.model.watchedItem

import io.github.couchtracker.db.user.WatchedItem
import io.github.couchtracker.db.user.WatchedItemChoice
import io.github.couchtracker.db.user.WatchedItemFreeText

data class WatchedItemWrapper(
    val entity: WatchedItem,
    val dimensions: List<WatchedItemDimensionSelection<*>>,
) {

    companion object {
        fun wrapAll(
            watchedItems: List<WatchedItem>,
            dimensions: List<WatchedItemDimensionWrapper>,
            choices: List<WatchedItemChoice>,
            freeTexts: List<WatchedItemFreeText>,
        ): List<WatchedItemWrapper> {
            return watchedItems.map { watchedItem ->
                WatchedItemWrapper(
                    entity = watchedItem,
                    dimensions = dimensions.map { dimension ->
                        when (dimension) {
                            is WatchedItemDimensionWrapper.Choice -> dimension.toSelection(choices)
                            is WatchedItemDimensionWrapper.FreeText -> dimension.toSelection(freeTexts)
                        }
                    },
                )
            }
        }
    }
}
