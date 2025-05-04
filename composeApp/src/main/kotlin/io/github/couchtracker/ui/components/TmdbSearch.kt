package io.github.couchtracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbCollection
import app.moviebase.tmdb.model.TmdbMovie
import app.moviebase.tmdb.model.TmdbPerson
import app.moviebase.tmdb.model.TmdbSearchableListItem
import app.moviebase.tmdb.model.TmdbShow
import coil3.compose.AsyncImage
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.tmdb.toInternalTmdbMovie
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.screens.main.TMDB_LANGUAGE
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.str
import kotlinx.coroutines.flow.Flow

typealias SearchMediaFilters = Set<SearchableMediaType>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbSearch(
    onDismissRequest: () -> Unit,
    mediaFilters: SearchMediaFilters,
    onMediaFilters: (SearchMediaFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(mediaFilters.isNotEmpty()) { "Media filters must have at least one element" }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<Flow<PagingData<SearchResultModel>>?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        keyboardController?.hide()
                        searchResults = getMediaSearchFlow(searchQuery, mediaFilters)
                    },
                    expanded = true,
                    onExpandedChange = { if (!it) onDismissRequest() },
                    placeholder = { Text(R.string.search_placeholder.str()) },
                    modifier = Modifier.focusRequester(focusRequester),
                    leadingIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = R.string.back_action.str(),
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    searchResults = null
                                },
                                content = { Icon(Icons.Default.Close, contentDescription = null) },
                            )
                        }
                    },
                )
            },
            expanded = true,
            onExpandedChange = { if (!it) onDismissRequest() },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (type in SearchableMediaType.entries) {
                    val selected = type in mediaFilters
                    MaterialTheme(colorScheme = type.colorScheme) {
                        FilterChip(
                            selected = selected,
                            leadingIcon = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            },
                            label = { Text(type.title.str()) },
                            onClick = {
                                if (selected) {
                                    if (mediaFilters.size > 1) {
                                        onMediaFilters(mediaFilters - type)
                                    } else {
                                        onMediaFilters(SearchableMediaType.entries.toSet())
                                    }
                                } else {
                                    onMediaFilters(mediaFilters + type)
                                }
                                searchResults = getMediaSearchFlow(searchQuery, mediaFilters)
                            },
                        )
                    }
                }
            }

            searchResults?.let { SearchResults(it) }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun SearchResults(results: Flow<PagingData<SearchResultModel>>) {
    val navController = LocalNavController.current

    // TODO should we have a PaginatedList instead?
    // TODO we need to show something if there are no search results
    PaginatedGrid(
        paginatedItems = results.collectAsLazyPagingItems(),
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        itemComposable = { item ->
            // TODO custom view for more flexibility?
            ListItem(
                leadingContent = {
                    // TODO placeholder image
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.aspectRatio(PortraitComposableDefaults.POSTER_ASPECT_RATIO),
                    ) {
                        BoxWithConstraints(Modifier.Companion.fillMaxSize()) {
                            AsyncImage(
                                model = item?.posterModel?.getCoilModel(this.constraints.maxWidth, this.constraints.minHeight), // TODO
                                modifier = Modifier.fillMaxSize(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                },
                headlineContent = { Text(item?.title.orEmpty()) },
                supportingContent = { Text(item?.subtitle.orEmpty()) },
                modifier = Modifier.height(96.dp).clickable { item?.navigate(navController) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        },
    )
}

private data class SearchResultModel(
    val posterModel: ImageModel?,
    val title: String,
    val subtitle: String?,
    val navigate: (NavController) -> Unit,
)

enum class SearchableMediaType(
    @StringRes val title: Int,
    val colorScheme: ColorScheme,
) {
    MOVIE(R.string.search_filter_movies, ColorSchemes.Movie),
    SHOW(R.string.search_filter_shows, ColorSchemes.Show),
    PERSON(R.string.search_filter_people, ColorSchemes.Common),
}

private fun getMediaSearchFlow(query: String, filters: SearchMediaFilters): Flow<PagingData<SearchResultModel>> {
    return when (filters.singleOrNull()) {
        SearchableMediaType.MOVIE -> tmdbPager(
            downloader = { page -> search.findMovies(query, page = page) },
            mapper = { it.toModel() },
        ).flow

        SearchableMediaType.SHOW -> tmdbPager(
            downloader = { page -> search.findShows(query, page = page) },
            mapper = { it.toModel() },
        ).flow

        SearchableMediaType.PERSON -> tmdbPager(
            downloader = { page -> search.findPeople(query, page = page) },
            mapper = { it.toModel() },
        ).flow

        null -> tmdbPager(
            downloader = { page -> search.findMulti(query, page = page) },
            mapper = {
                if (it.type !in filters) {
                    null
                } else {
                    it.toModel()
                }
            },
        ).flow
    }
}

private val TmdbSearchableListItem.type
    get() = when (this) {
        is TmdbMovie -> SearchableMediaType.MOVIE
        is TmdbShow -> SearchableMediaType.SHOW
        is TmdbPerson -> SearchableMediaType.PERSON
        is TmdbCollection -> null
    }

private suspend fun TmdbSearchableListItem.toModel(): SearchResultModel? {
    // TODO: preload?
    return when (this) {
        is TmdbCollection -> null

        is TmdbMovie -> SearchResultModel(
            posterModel = posterImage?.toImageModel(ImagePreloadOptions.DoNotPreload),
            title = title,
            subtitle = releaseDate?.year?.toString(),
            navigate = { it.navigateToMovie(toInternalTmdbMovie(TMDB_LANGUAGE)) },
        )

        is TmdbPerson -> SearchResultModel(
            posterModel = profileImage?.toImageModel(ImagePreloadOptions.DoNotPreload),
            title = name,
            subtitle = null,
            navigate = { /* TODO */ },
        )

        is TmdbShow -> SearchResultModel(
            posterModel = posterImage?.toImageModel(ImagePreloadOptions.DoNotPreload),
            title = name,
            subtitle = firstAirDate?.year?.toString(),
            navigate = { /* TODO */ },
        )
    }
}
