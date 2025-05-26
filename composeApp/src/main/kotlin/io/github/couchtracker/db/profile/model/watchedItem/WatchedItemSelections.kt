package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.db.app.ProfileManager
import io.github.couchtracker.db.profile.ProfileDbResult
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode

class WatchedItemSelections(
    val sheetMode: WatchedItemSheetMode,
    val datetime: DateTimeSectionState,
    dimensions: List<WatchedItemDimensionSelection<*>>,
) {
    private var dimensionsMap by mutableStateOf(dimensions.associateBy { it.dimension })

    val dimensions get() = dimensionsMap.values

    fun update(selection: WatchedItemDimensionSelection<*>) {
        dimensionsMap = dimensionsMap.plus(selection.dimension to selection)
    }

    suspend fun save(profile: ProfileManager.ProfileInfo): ProfileDbResult<Unit> {
        return profile.write { db ->
            val watchAt = datetime.dateTime?.dateTime
            val watchedItem = when (sheetMode) {
                is WatchedItemSheetMode.New -> db.watchedItemQueries.insert(
                    itemId = sheetMode.itemId,
                    watchAt = watchAt,
                ).executeAsOne()

                is WatchedItemSheetMode.Edit -> db.watchedItemQueries.update(
                    id = sheetMode.watchedItem.id,
                    watchAt = watchAt,
                ).executeAsOne()
            }

            for (dimension in dimensions) {
                dimension.save(db, watchedItem)
            }
        }
    }
}

@Composable
fun rememberWatchedItemSelections(watchedItemType: WatchedItemType, mode: WatchedItemSheetMode): WatchedItemSelections {
    val profileData = LocalFullProfileDataContext.current

    // TODO make this savable
    return remember(profileData, watchedItemType, mode) {
        when (mode) {
            is WatchedItemSheetMode.New -> {
                val dimensions = profileData.watchedItemDimensions
                    .filter { watchedItemType in it.appliesTo }
                    .map { it.emptySelection() }
                WatchedItemSelections(
                    sheetMode = mode,
                    datetime = DateTimeSectionState(),
                    dimensions = dimensions,
                )
            }

            is WatchedItemSheetMode.Edit -> {
                WatchedItemSelections(
                    sheetMode = mode,
                    datetime = DateTimeSectionState(mode.watchedItem.watchAt),
                    dimensions = mode.watchedItem.dimensions,
                )
            }
        }
    }
}
