package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode

class WatchedItemSelections(
    val sheetMode: WatchedItemSheetMode,
    val datetime: DateTimeSectionState,
    dimensions: List<WatchedItemDimensionSelection<*>>,
) {
    private var dimensionsMap by mutableStateOf(dimensions.associateBy { it.dimension })

    val dimensions get() = dimensionsMap.values

    fun isValid(): Boolean {
        return dimensions.all { it.validity() is WatchedItemDimensionSelectionValidity.Valid }
    }

    fun update(selection: WatchedItemDimensionSelection<*>) {
        dimensionsMap = dimensionsMap.plus(selection.dimension to selection)
    }

    fun save(db: ProfileData) {
        db.transaction {
            val watchAt = datetime.dateTime?.dateTime
            val watchedItem = when (sheetMode) {
                is WatchedItemSheetMode.New -> {
                    val watchedItem = db.watchedItemQueries.insert(watchAt = watchAt).executeAsOne()
                    sheetMode.save(db, watchedItem)
                    watchedItem
                }

                is WatchedItemSheetMode.Edit -> db.watchedItemQueries.updateWatchAt(
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
