package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItemChoice
import io.github.couchtracker.db.profile.WatchedItemDimensionSelections
import kotlin.collections.orEmpty

data class WatchedItemDimensionSelectionsWrapper(
    val watchedItemDimensionSelections: WatchedItemDimensionSelections,
    val dimensions: List<WatchedItemDimensionSelection<*>>,
) {

    val id get() = watchedItemDimensionSelections.id

    companion object {

        fun load(
            db: ProfileData,
            dimensions: List<WatchedItemDimensionWrapper>,
        ): List<WatchedItemDimensionSelectionsWrapper> {
            val watchedItemDimensionSelectionsIds = db.watchedItemDimensionSelectionsQueries.selectAll().executeAsList()
            val choices = db.watchedItemChoiceQueries.selectAllWithDimensionId().executeAsList()
                .groupBy { it.selections to it.watchedItemDimension }
                .mapValues { (_, list) ->
                    list.map { WatchedItemChoice(selections = it.selections, choice = it.choice) }
                }
            val freeTexts = db.watchedItemFreeTextQueries.selectAll().executeAsList()
                .associateBy { it.selections to it.dimension }
            val languages = db.watchedItemLanguageQueries.selectAll().executeAsList()
                .associateBy { it.selections to it.dimension }

            return watchedItemDimensionSelectionsIds.map { id ->
                val dimensions = dimensions.map { dimension ->
                    val key = id to dimension.id
                    when (dimension) {
                        is WatchedItemDimensionWrapper.Choice -> dimension.selection(choices[key].orEmpty())
                        is WatchedItemDimensionWrapper.Language -> dimension.selection(languages[key])
                        is WatchedItemDimensionWrapper.FreeText -> dimension.selection(freeTexts[key])
                    }
                }.sortedBy { it.dimension.manualSortIndex }

                WatchedItemDimensionSelectionsWrapper(
                    watchedItemDimensionSelections = WatchedItemDimensionSelections(id),
                    dimensions = dimensions,
                )
            }
        }
    }
}
