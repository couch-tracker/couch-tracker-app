package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.WatchedItemDimension
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The type of selection that can be made for a [WatchedItemDimension].
 */
@Serializable
sealed interface WatchedItemDimensionType {

    @Serializable
    @SerialName("choice")
    data class Choice(val maxSelections: Int?) : WatchedItemDimensionType {
        init {
            require(maxSelections == null || maxSelections > 0) { "maxSelections cannot be negative" }
        }

        companion object {
            val SINGLE = Choice(maxSelections = 1)
            val UNBOUNDED = Choice(maxSelections = null)
        }
    }

    @Serializable
    @SerialName("freeText")
    data object FreeText : WatchedItemDimensionType
}
