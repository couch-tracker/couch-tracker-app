package io.github.couchtracker.model.sort

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.couchtracker.R
import io.github.couchtracker.utils.Text

enum class SortableItemType {
    SHOW,
    MOVIE,
}

enum class SorterCategoryState {
    UNSELECTED,
    SELECTED,
    SELECTED_ASC,
    SELECTED_DESC,
}

enum class SorterCategory(
    val localizedName: Text,
    val icon: ImageVector,
    val currentState: (currentSorter: Sorter) -> SorterCategoryState,
    val createSorter: (currentSorter: Sorter) -> Sorter,
    val supportedItems: Set<SortableItemType>,
) {
    ALPHABETICAL(
        localizedName = Text.Resource(R.string.sorter_by_name),
        icon = Icons.Default.SortByAlpha,
        currentState = {
            when (it) {
                is Sorter.Alphabetical if it.asc -> SorterCategoryState.SELECTED_ASC
                is Sorter.Alphabetical -> SorterCategoryState.SELECTED_DESC
                else -> SorterCategoryState.UNSELECTED
            }
        },
        createSorter = {
            if (it is Sorter.Alphabetical) it.copy(asc = !it.asc) else Sorter.Alphabetical()
        },
        supportedItems = setOf(SortableItemType.SHOW, SortableItemType.MOVIE),
    ),
    BY_TMDB_RATING(
        localizedName = Text.Resource(R.string.sorter_by_rating),
        icon = Icons.Default.StarRate,
        currentState = {
            when (it) {
                is Sorter.ByTmdbRating if it.asc -> SorterCategoryState.SELECTED_ASC
                is Sorter.ByTmdbRating -> SorterCategoryState.SELECTED_DESC
                else -> SorterCategoryState.UNSELECTED
            }
        },
        createSorter = {
            if (it is Sorter.ByTmdbRating) it.copy(asc = !it.asc) else Sorter.ByTmdbRating()
        },
        supportedItems = setOf(SortableItemType.SHOW, SortableItemType.MOVIE),
    ),
    BY_RELEASE_DATE(
        localizedName = Text.Resource(R.string.sorter_by_release_date),
        icon = Icons.Default.Today,
        currentState = {
            when (it) {
                is Sorter.ByReleaseDate if it.asc -> SorterCategoryState.SELECTED_ASC
                is Sorter.ByReleaseDate -> SorterCategoryState.SELECTED_DESC
                else -> SorterCategoryState.UNSELECTED
            }
        },
        createSorter = {
            if (it is Sorter.ByReleaseDate) it.copy(asc = !it.asc) else Sorter.ByReleaseDate()
        },
        supportedItems = setOf(SortableItemType.SHOW, SortableItemType.MOVIE),
    ),
}
