package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice

/**
 * Represents the value of a specific [WatchedItemDimensionWrapper].
 *
 * @property dimension the dimension this selection represents
 * @property value the value of the current selection
 */
sealed interface WatchedItemDimensionSelection<T> {

    val dimension: WatchedItemDimensionWrapper
    val value: T

    /**
     * Returns whether the current selection is valid.
     *
     * TODO: use this in a meaningful way.
     * TODO: understand if we want to handle an invalid selection coming out of the DB, or prevent that from happening
     */
    fun isValid(): Boolean

    /**
     * Saves this selection to the given [db] for the given [watchedItem], which must already exist.
     *
     * Saving unchanged data is permitted, but might have small side effects (e.g. deleting and re-adding the same row)
     */
    fun save(db: ProfileData, watchedItem: WatchedItem)

    data class Choice(
        override val dimension: WatchedItemDimensionWrapper.Choice,
        override val value: Set<WatchedItemDimensionChoice>,
    ) : WatchedItemDimensionSelection<Set<WatchedItemDimensionChoice>> {

        override fun isValid() = dimension.type.maxSelections == null || value.size <= dimension.type.maxSelections

        override fun save(db: ProfileData, watchedItem: WatchedItem) {
            db.watchedItemChoiceQueries.transaction {
                db.watchedItemChoiceQueries.deleteForDimension(
                    watchedItem = watchedItem.id,
                    dimension = dimension.id,
                )

                for (choice in value) {
                    db.watchedItemChoiceQueries.insert(watchedItem = watchedItem.id, choice = choice.id)
                }
            }
        }

        /**
         * Returns a new [WatchedItemDimensionSelection.Choice] where the given [choice] was toggled.
         *
         * Currently, this function does not allow for the number of selections to go above the max configured amount.
         */
        fun toggled(choice: WatchedItemDimensionChoice): Choice {
            return if (choice in value) {
                Choice(dimension = dimension, value = value - choice)
            } else if (dimension.type.maxSelections == 1) {
                Choice(dimension = dimension, value = setOf(choice))
            } else if (dimension.type.maxSelections != null && value.size + 1 > dimension.type.maxSelections) {
                // TODO we might want to remove this as we implement isValid logic
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
            if (value.isBlank()) {
                db.watchedItemFreeTextQueries.delete(
                    watchedItem = watchedItem.id,
                    watchedItemDimension = dimension.id,
                )
            } else {
                db.watchedItemFreeTextQueries.upsert(
                    watchedItem = watchedItem.id,
                    watchedItemDimension = dimension.id,
                    text = value,
                )
            }
        }
    }
}
