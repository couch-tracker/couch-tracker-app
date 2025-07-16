package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice
import kotlin.collections.minus
import kotlin.collections.plus
import kotlin.text.isBlank

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
     */
    fun validity(): WatchedItemDimensionSelectionValidity

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

        override fun validity(): WatchedItemDimensionSelectionValidity {
            if (dimension.type.maxSelections != null) {
                if (value.size > dimension.type.maxSelections) {
                    return WatchedItemDimensionSelectionValidity.Invalid.TooManyChoicesSelected(dimension.type.maxSelections)
                }
            }
            return WatchedItemDimensionSelectionValidity.Valid
        }

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
         */
        fun toggled(choice: WatchedItemDimensionChoice): Choice {
            return if (choice in value) {
                Choice(dimension = dimension, value = value - choice)
            } else if (dimension.type.maxSelections == 1) {
                Choice(dimension = dimension, value = setOf(choice))
            } else {
                Choice(dimension = dimension, value = value + choice)
            }
        }
    }

    data class Language(
        override val dimension: WatchedItemDimensionWrapper.Language,
        override val value: Bcp47Language?,
    ) : WatchedItemDimensionSelection<Bcp47Language?> {

        override fun validity() = WatchedItemDimensionSelectionValidity.Valid

        override fun save(db: ProfileData, watchedItem: WatchedItem) {
            if (value == null) {
                db.watchedItemLanguageQueries.delete(
                    watchedItem = watchedItem.id,
                    watchedItemDimension = dimension.id,
                )
            } else {
                db.watchedItemLanguageQueries.upsert(
                    watchedItem = watchedItem.id,
                    watchedItemDimension = dimension.id,
                    language = value,
                )
            }
        }

        fun toggled(language: Bcp47Language): Language {
            return if (language == value) {
                Language(dimension = dimension, value = null)
            } else {
                Language(dimension = dimension, value = language)
            }
        }
    }

    data class FreeText(
        override val dimension: WatchedItemDimensionWrapper.FreeText,
        override val value: String,
    ) : WatchedItemDimensionSelection<String> {

        override fun validity() = WatchedItemDimensionSelectionValidity.Valid

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
