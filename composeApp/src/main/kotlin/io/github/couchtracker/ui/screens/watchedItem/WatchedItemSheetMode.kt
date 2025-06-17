package io.github.couchtracker.ui.screens.watchedItem

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import kotlinx.parcelize.Parcelize

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

    @Parcelize
    sealed interface Savable : Parcelable {
        data class New(val itemId: WatchableExternalId) : Savable
        data class Edit(val profileId: Long, val watchedItemId: Long) : Savable
    }

    companion object {

        fun saver(fullProfileData: FullProfileData): Saver<WatchedItemSheetMode, Savable> {
            return Saver(
                save = {
                    when (it) {
                        is New -> Savable.New(itemId = it.itemId)
                        is Edit -> Savable.Edit(
                            profileId = fullProfileData.profile.id,
                            watchedItemId = it.watchedItem.id,
                        )
                    }
                },
                restore = { savedMode ->
                    when (savedMode) {
                        is Savable.New -> New(itemId = savedMode.itemId)
                        is Savable.Edit -> when (savedMode.profileId == fullProfileData.profile.id) {
                            true -> fullProfileData.watchedItems.find { it.id == savedMode.watchedItemId }?.let { Edit(it) }
                            false -> null
                        }
                    }
                },
            )
        }
    }
}
