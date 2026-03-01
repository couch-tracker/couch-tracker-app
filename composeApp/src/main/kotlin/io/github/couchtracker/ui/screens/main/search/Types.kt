package io.github.couchtracker.ui.screens.main.search

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import kotlinx.serialization.Serializable

data class SearchParameters(
    val query: String,
    val filters: SearchMediaFilters,
    val incomplete: Boolean,
) {

    init {
        require(filters.isNotEmpty()) { "Media filters must have at least one element" }
    }

    fun isEmpty() = query.isBlank()
}

data class SearchInstance(
    val searchParameters: SearchParameters,
    val tmdbLanguage: TmdbLanguage,
    val lazyGridState: LazyGridState,
)

data class SearchResultItem(
    val posterModel: ImageModel?,
    val title: String?,
    val type: SearchableMediaType,
    val tags: List<String>,
    val trailingInfo: String?,
    val navigate: (NavController) -> Unit,
)

@Keep
@Serializable
enum class SearchableMediaType(
    @StringRes val title: Int,
    @StringRes val itemType: Int,
    val colorScheme: ColorScheme,
    val icon: ImageVector,
) {
    MOVIE(
        title = R.string.search_filter_movies,
        itemType = R.string.item_type_movie,
        colorScheme = ColorSchemes.Movie,
        icon = PlaceholdersDefaults.MOVIE.icon,
    ),
    SHOW(
        title = R.string.search_filter_shows,
        itemType = R.string.item_type_show,
        colorScheme = ColorSchemes.Show,
        icon = PlaceholdersDefaults.SHOW.icon,
    ),
    PERSON(
        title = R.string.search_filter_people,
        itemType = R.string.item_type_person,
        colorScheme = ColorSchemes.Common,
        icon = PlaceholdersDefaults.PERSON.icon,
    ),
}
