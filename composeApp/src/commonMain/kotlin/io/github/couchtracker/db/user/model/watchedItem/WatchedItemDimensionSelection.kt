package io.github.couchtracker.db.user.model.watchedItem

import io.github.couchtracker.db.user.WatchedItemChoice
import io.github.couchtracker.db.user.WatchedItemFreeText

sealed interface WatchedItemDimensionSelection<out T> {

    val dimension: WatchedItemDimensionWrapper
    val selection: T

    fun isEmpty(): Boolean
    fun isValid(): Boolean

    data class Choice(
        override val dimension: WatchedItemDimensionWrapper.Choice,
        override val selection: List<WatchedItemChoice>,
    ) : WatchedItemDimensionSelection<List<WatchedItemChoice>> {
        override fun isEmpty() = selection.isEmpty()
        override fun isValid() = dimension.type.maxSelections == null || selection.size < dimension.type.maxSelections
    }

    data class FreeText(
        override val dimension: WatchedItemDimensionWrapper.FreeText,
        override val selection: WatchedItemFreeText?,
    ) : WatchedItemDimensionSelection<WatchedItemFreeText?> {
        override fun isEmpty() = selection == null
        override fun isValid() = selection == null || selection.text.isNotBlank()
    }
}
