package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState

class WatchedItemSelections(
    val datetime: DateTimeSectionState,
    dimensions: List<WatchedItemDimensionSelection<*>>,
) {
    private var dimensionsMap by mutableStateOf(dimensions.associateBy { it.dimension })

    val dimensions get() = dimensionsMap.values

    fun update(selection: WatchedItemDimensionSelection<*>) {
        dimensionsMap = dimensionsMap.plus(selection.dimension to selection)
    }
}

@Composable
fun rememberWatchedItemSelections(watchedItemType: WatchedItemType, watchedItem: WatchedItem?): WatchedItemSelections {
    val profileData = LocalFullProfileDataContext.current

    // TODO savable?
    return remember(profileData) {
        if (watchedItem == null) {
            val dimensions = profileData.watchedItemDimensions
                .filter { watchedItemType in it.appliesTo }
                .map { it.emptySelection() }
            WatchedItemSelections(
                datetime = DateTimeSectionState(),
                dimensions = dimensions,
            )
        } else {
            error("not supported yet")
        }
    }
}
