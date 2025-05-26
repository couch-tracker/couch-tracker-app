package io.github.couchtracker.ui.screens.watchedItem

import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper

/**
 * Represents the mode to open a [WatchedItemSheetScaffold].
 */
sealed interface WatchedItemSheetMode {

    val itemId: WatchableExternalId

    /**
     * A sheet opened with blank data, to create a new watched item.
     */
    data class New(override val itemId: WatchableExternalId) : WatchedItemSheetMode

    /**
     * A sheet opened pre-populated with existing data, to edit an existing watched item.
     */
    data class Edit(val watchedItem: WatchedItemWrapper) : WatchedItemSheetMode {
        override val itemId: WatchableExternalId get() = watchedItem.itemId
    }
}
