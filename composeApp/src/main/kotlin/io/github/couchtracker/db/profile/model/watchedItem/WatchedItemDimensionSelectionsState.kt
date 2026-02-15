package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItemDimensionSelections

class WatchedItemDimensionSelectionsState(
    val mode: WatchedItemDimensionSelectionsMode,
    dimensions: List<WatchedItemDimensionSelection<*>>,
) {
    private var dimensionsMap by mutableStateOf(dimensions.associateBy { it.dimension })
    val dimensions get() = dimensionsMap.values.toList()

    fun isValid(): Boolean {
        return dimensions.all { it.validity() is WatchedItemDimensionSelectionValidity.Valid }
    }

    fun update(selection: WatchedItemDimensionSelection<*>) {
        val previouslyEnabled = getEnabledDimensions()
        dimensionsMap += selection.dimension to selection
        val currentlyEnabled = getEnabledDimensions()

        // We need to clear the selection of any item that got disabled
        val newlyDisabledDimensions = previouslyEnabled - currentlyEnabled
        for (dimension in newlyDisabledDimensions) {
            dimensionsMap += dimension to dimension.emptySelection()
        }
    }

    private fun getEnabledDimensions(): Set<WatchedItemDimensionWrapper> {
        return dimensions.filter { it.dimension.isEnabled(dimensions) }.map { it.dimension }.toSet()
    }

    fun save(db: ProfileData): WatchedItemDimensionSelections {
        return db.transactionWithResult {
            val dimensionsSelections = when (mode) {
                WatchedItemDimensionSelectionsMode.New -> {
                    val selectionsId = db.watchedItemDimensionSelectionsQueries.insert().executeAsOne()
                    WatchedItemDimensionSelections(selectionsId)
                }
                is WatchedItemDimensionSelectionsMode.Edit -> mode.selections.watchedItemDimensionSelections
            }

            for (dimension in dimensions) {
                dimension.save(db, dimensionsSelections)
            }

            dimensionsSelections
        }
    }
}

sealed interface WatchedItemDimensionSelectionsMode {

    data object New : WatchedItemDimensionSelectionsMode

    data class Edit(val selections: WatchedItemDimensionSelectionsWrapper) : WatchedItemDimensionSelectionsMode
}

@Composable
fun rememberWatchedItemDimensionSelectionsState(
    watchedItemType: WatchedItemType,
    mode: WatchedItemDimensionSelectionsMode,
): WatchedItemDimensionSelectionsState {
    val profileData = LocalFullProfileDataContext.current

    // TODO make this savable
    return remember(profileData, watchedItemType, mode) {
        when (mode) {
            is WatchedItemDimensionSelectionsMode.New -> {
                val dimensions = profileData.watchedItemDimensions
                    .filter { watchedItemType in it.appliesTo }
                    .map { it.emptySelection() }
                WatchedItemDimensionSelectionsState(
                    mode = mode,
                    dimensions = dimensions,
                )
            }

            is WatchedItemDimensionSelectionsMode.Edit -> {
                WatchedItemDimensionSelectionsState(
                    mode = mode,
                    dimensions = mode.selections.dimensions
                        .filter { watchedItemType in it.dimension.appliesTo || !it.isEmpty() },
                )
            }
        }
    }
}
