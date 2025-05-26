package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice

typealias WatchedItemDimensionSelections = List<WatchedItemDimensionSelection<*>>

sealed interface WatchedItemDimensionSelection<T> {

    val dimension: WatchedItemDimensionWrapper
    val value: T

    fun isValid(): Boolean
    fun save(db: ProfileData, watchedItem: WatchedItem)

    data class Choice(
        override val dimension: WatchedItemDimensionWrapper.Choice,
        override val value: Set<WatchedItemDimensionChoice>,
    ) : WatchedItemDimensionSelection<Set<WatchedItemDimensionChoice>> {

        override fun isValid() = dimension.type.maxSelections == null || value.size <= dimension.type.maxSelections

        override fun save(db: ProfileData, watchedItem: WatchedItem) {
            db.watchedItemChoiceQueries.transaction {
                db.watchedItemChoiceQueries.delete(watchedItem = watchedItem.id)

                for (item in value) {
                    db.watchedItemChoiceQueries.insert(watchedItem = watchedItem.id, choice = item.id)
                }
            }
        }

        fun toggled(choice: WatchedItemDimensionChoice): Choice {
            return if (dimension.type.maxSelections == 1) {
                Choice(dimension = dimension, value = setOf(choice))
            } else if (choice in value) {
                Choice(dimension = dimension, value = value - choice)
            } else if (dimension.type.maxSelections != null && value.size + 1 > dimension.type.maxSelections) {
                this
            } else {
                Choice(dimension = dimension, value = value + choice)
            }
        }
    }

    data class FreeText(
        override val dimension: WatchedItemDimensionWrapper.FreeText,
        override val value: String,
    ) : WatchedItemDimensionSelection<String> {

        override fun isValid() = true

        override fun save(db: ProfileData, watchedItem: WatchedItem) {
            db.watchedItemChoiceQueries.transaction {
                db.watchedItemFreeTextQueries.delete(
                    watchedItem = watchedItem.id,
                    watchedItemDimension = dimension.id,
                )
                if (value.isNotBlank()) {
                    db.watchedItemFreeTextQueries.insert(
                        watchedItem = watchedItem.id,
                        watchedItemDimension = dimension.id,
                        text = value,
                    )
                }
            }
        }
    }
}
