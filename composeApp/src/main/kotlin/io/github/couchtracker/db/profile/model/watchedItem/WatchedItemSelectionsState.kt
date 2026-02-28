package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode

class WatchedItemSelectionsState(
    val sheetMode: WatchedItemSheetMode,
    val datetime: DateTimeSectionState,
    val dimensionsState: WatchedItemDimensionSelectionsState,
) {

    val dimensions get() = dimensionsState.dimensions

    fun isValid() = dimensionsState.isValid()

    fun update(selection: WatchedItemDimensionSelection<*>) {
        dimensionsState.update(selection)
    }

    fun save(db: ProfileData) {
        db.transaction {
            val dimensionSelections = dimensionsState.save(db)
            val watchAt = datetime.dateTime?.dateTime
            when (sheetMode) {
                is WatchedItemSheetMode.New -> {
                    val watchedItem = db.watchedItemQueries.insert(
                        watchAt = watchAt,
                        dimensionSelections = dimensionSelections.id,
                    ).executeAsOne()
                    sheetMode.save(db, watchedItem)
                }

                is WatchedItemSheetMode.Edit -> {
                    db.watchedItemQueries.updateWatchAt(
                        id = sheetMode.watchedItem.id,
                        watchAt = watchAt,
                    ).executeAsOne()
                }
            }
        }
    }
}

@Composable
fun rememberWatchedItemSelectionsState(watchedItemType: WatchedItemType, mode: WatchedItemSheetMode): WatchedItemSelectionsState {
    val profileData = LocalFullProfileDataContext.current
    val dimensionSelectionsState = rememberWatchedItemDimensionSelectionsState(
        watchedItemType = watchedItemType,
        mode = when (mode) {
            is WatchedItemSheetMode.New -> WatchedItemDimensionSelectionsMode.New
            is WatchedItemSheetMode.Edit -> WatchedItemDimensionSelectionsMode.Edit(mode.watchedItem.selections)
        },
    )

    // TODO make this savable
    return remember(profileData, watchedItemType, mode, dimensionSelectionsState) {
        when (mode) {
            is WatchedItemSheetMode.New -> WatchedItemSelectionsState(
                sheetMode = mode,
                datetime = DateTimeSectionState(),
                dimensionsState = dimensionSelectionsState,
            )

            is WatchedItemSheetMode.Edit -> WatchedItemSelectionsState(
                sheetMode = mode,
                datetime = DateTimeSectionState(mode.watchedItem.watchAt),
                dimensionsState = dimensionSelectionsState,
            )
        }
    }
}
