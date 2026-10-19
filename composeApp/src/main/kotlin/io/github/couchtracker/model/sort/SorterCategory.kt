package io.github.couchtracker.model.sort

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.couchtracker.R
import io.github.couchtracker.utils.Icon
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.toIcon

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
    val icon: Icon,
    val currentState: (currentSorter: Sorter) -> SorterCategoryState,
    val createSorter: (currentSorter: Sorter) -> Sorter,
    val supportedItems: Set<SortableItemType>,
) {
    ALPHABETICAL(
        localizedName = Text.Resource(R.string.sorter_by_name),
        icon = Icons.Default.SortByAlpha.toIcon(),
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
        icon = Icons.Default.StarRate.toIcon(),
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
        icon = Icons.Default.Today.toIcon(),
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
    RANDOM(
        localizedName = Text.Resource(R.string.sorter_random),
        icon = Icon.Resource(R.drawable.dice),
        currentState = {
            when (it) {
                is Sorter.Random -> SorterCategoryState.SELECTED
                else -> SorterCategoryState.UNSELECTED
            }
        },
        createSorter = { Sorter.Random() },
        supportedItems = setOf(SortableItemType.SHOW, SortableItemType.MOVIE),
    ),
    BY_RUNTIME(
        localizedName = Text.Resource(R.string.sorter_by_runtime),
        icon = Icons.Default.WatchLater.toIcon(),
        currentState = {
            when (it) {
                is Sorter.ByRuntime if it.asc -> SorterCategoryState.SELECTED_ASC
                is Sorter.ByRuntime -> SorterCategoryState.SELECTED_DESC
                else -> SorterCategoryState.UNSELECTED
            }
        },
        createSorter = {
            if (it is Sorter.ByRuntime) it.copy(asc = !it.asc) else Sorter.ByRuntime()
        },
        supportedItems = setOf(SortableItemType.SHOW, SortableItemType.MOVIE),
    ),
    BY_EPISODE_RUNTIME(
        localizedName = Text.Resource(R.string.sorter_by_episode_runtime),
        icon = Icons.Default.WatchLater.toIcon(),
        currentState = {
            when (it) {
                is Sorter.ByEpisodeRuntime if it.asc -> SorterCategoryState.SELECTED_ASC
                is Sorter.ByEpisodeRuntime -> SorterCategoryState.SELECTED_DESC
                else -> SorterCategoryState.UNSELECTED
            }
        },
        createSorter = {
            if (it is Sorter.ByEpisodeRuntime) it.copy(asc = !it.asc) else Sorter.ByEpisodeRuntime()
        },
        supportedItems = setOf(SortableItemType.SHOW),
    ),
}
