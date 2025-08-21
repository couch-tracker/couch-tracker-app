package io.github.couchtracker.ui.screens.main

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbCollection
import app.moviebase.tmdb.model.TmdbMovie
import app.moviebase.tmdb.model.TmdbPerson
import app.moviebase.tmdb.model.TmdbSearchableListItem
import app.moviebase.tmdb.model.TmdbShow
import coil3.compose.AsyncImage
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.tmdbMovieId
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.tmdb.tmdbShowId
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.PlaceholdersDefaults
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.TagsRow
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.emptyPager
import io.github.couchtracker.utils.heightWithAspectRatio
import io.github.couchtracker.utils.settings.get
import io.github.couchtracker.utils.str
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

typealias SearchMediaFilters = Set<SearchableMediaType>

@Serializable
data class SearchScreen(val filter: SearchableMediaType?) : Screen() {
    @Composable
    override fun content() {
        val navController = LocalNavController.current
        Content(
            onDismissRequest = { navController.navigateUp() },
            viewModel = viewModel {
                SearchViewModel(filter?.let { setOf(it) } ?: SearchableMediaType.entries.toSet())
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    onDismissRequest: () -> Unit,
    viewModel: SearchViewModel,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchParameters = viewModel.searchParameters

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchParameters.query,
                    onQueryChange = {
                        viewModel.search(incomplete = true, query = it)
                    },
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.search(incomplete = false, query = it)
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
                        if (!searchParameters.isEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearResults() },
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
                    val selected = type in searchParameters.filters
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
                                val newFilters = if (selected) {
                                    if (searchParameters.filters.size > 1) {
                                        searchParameters.filters - type
                                    } else {
                                        SearchableMediaType.entries.toSet()
                                    }
                                } else {
                                    searchParameters.filters + type
                                }
                                viewModel.search(incomplete = false, filters = newFilters)
                            },
                        )
                    }
                }
            }

            val currentSearchInstance by viewModel.currentSearchInstance.collectAsStateWithLifecycle(null)
            if (currentSearchInstance?.searchParameters?.isEmpty() == false) {
                SearchResults(results = viewModel.searchResults, lazyGridState = viewModel.lazyGridState)
            }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

private data class SearchParameters(
    val query: String,
    val filters: SearchMediaFilters,
    val incomplete: Boolean,
) {

    init {
        require(filters.isNotEmpty()) { "Media filters must have at least one element" }
    }

    fun isEmpty() = query.isBlank()
}

private data class SearchInstance(
    val searchParameters: SearchParameters,
    val tmdbLanguage: TmdbLanguage,
    val lazyGridState: LazyGridState,
)

private class SearchViewModel(initialMediaFilters: SearchMediaFilters) : ViewModel() {

    var lazyGridState by mutableStateOf(LazyGridState(0, 0))
    var searchParameters by mutableStateOf(
        SearchParameters(
            query = "",
            filters = initialMediaFilters,
            incomplete = true,
        ),
    )
        private set

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val currentSearchInstance = snapshotFlow { searchParameters }
        .debounce { if (it.incomplete) 300.milliseconds else 0.milliseconds }
        .distinctUntilChangedBy { it.query to it.filters }
        .combine(AppSettings.get { Tmdb.Languages }) { searchParameters, tmdbLanguages ->
            SearchInstance(
                searchParameters = searchParameters,
                tmdbLanguage = tmdbLanguages.current.apiLanguage,
                lazyGridState = LazyGridState(0, 0),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<SearchResultItem>> = currentSearchInstance
        .flatMapLatest { (searchParameters, tmdbLanguage) ->
            val (query, filters) = searchParameters

            val pager = if (searchParameters.isEmpty()) {
                emptyPager()
            } else {
                tmdbPager(
                    downloader = { page ->
                        when (filters.singleOrNull()) {
                            SearchableMediaType.MOVIE -> search.findMovies(query, page = page, language = tmdbLanguage.apiParameter)
                            SearchableMediaType.SHOW -> search.findShows(query, page = page, language = tmdbLanguage.apiParameter)
                            SearchableMediaType.PERSON -> search.findPeople(query, page = page, language = tmdbLanguage.apiParameter)
                            null -> search.findMulti(query, page = page)
                        }
                    },
                    mapper = {
                        if (it.type !in filters) {
                            null
                        } else {
                            it.toModel(tmdbLanguage)
                        }
                    },
                )
            }
            pager.flow
        }
        .cachedIn(viewModelScope)

    fun search(
        incomplete: Boolean,
        query: String = searchParameters.query,
        filters: SearchMediaFilters = searchParameters.filters,
    ) {
        searchParameters = SearchParameters(
            query = query,
            filters = filters,
            incomplete = incomplete,
        )
        lazyGridState = LazyGridState(0, 0)
    }

    fun clearResults() {
        search(incomplete = false, query = "")
    }
}

@Composable
private fun SearchResults(results: Flow<PagingData<SearchResultItem>>, lazyGridState: LazyGridState) {
    val inset = WindowInsets.Companion.systemBars
    val contentPadding =
        inset.only(WindowInsetsSides.Companion.Horizontal).add(inset.only(WindowInsetsSides.Companion.Bottom))
            .asPaddingValues()

    PaginatedGrid(
        paginatedItems = results.collectAsLazyPagingItems(),
        columns = GridCells.Fixed(1),
        contentPadding = contentPadding,
        lazyGridState = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        itemComposable = { SearchResult(it) },
        emptyComposable = {
            MessageComposable(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                icon = Icons.Default.SearchOff,
                message = R.string.search_no_results.str(),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchResult(item: SearchResultItem?) {
    val navController = LocalNavController.current

    ListItem(
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                shadowElevation = 8.dp,
                tonalElevation = 8.dp,
                modifier = Modifier.heightWithAspectRatio(
                    height = 80.dp,
                    aspectRatio = PortraitComposableDefaults.POSTER_ASPECT_RATIO,
                ),
            ) {
                BoxWithConstraints(contentAlignment = Alignment.BottomStart) {
                    AsyncImage(
                        model = item?.posterModel?.getCoilModel(
                            this.constraints.maxWidth,
                            this.constraints.maxHeight,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                        contentScale = ContentScale.Companion.Crop,
                        fallback = item?.type?.icon?.let { rememberPlaceholderPainter(it, isError = false) },
                        error = item?.type?.icon?.let { rememberPlaceholderPainter(it, isError = true) },
                    )
                }
            }
        },
        headlineContent = { Text(item?.title.orEmpty()) },
        supportingContent = {
            Column {
                val scheme = item?.type?.colorScheme ?: MaterialTheme.colorScheme
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                ) {
                    Text(
                        text = item?.type?.title?.str().orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp),
                    )
                }
                TagsRow(item?.tags.orEmpty())
            }
        },
        modifier = Modifier.clickable { item?.navigate(navController) },
        colors = ListItemDefaults.colors(containerColor = Color.Companion.Transparent),
    )
}

private data class SearchResultItem(
    val posterModel: ImageModel?,
    val title: String,
    val type: SearchableMediaType,
    val tags: List<String>,
    val navigate: (NavController) -> Unit,
)

@Keep
@Serializable
enum class SearchableMediaType(
    @StringRes val title: Int,
    val colorScheme: ColorScheme,
    val icon: ImageVector,
) {
    MOVIE(
        title = R.string.search_filter_movies,
        colorScheme = ColorSchemes.Movie,
        icon = PlaceholdersDefaults.MOVIE.icon,
    ),
    SHOW(
        title = R.string.search_filter_shows,
        colorScheme = ColorSchemes.Show,
        icon = PlaceholdersDefaults.SHOW.icon,
    ),
    PERSON(
        title = R.string.search_filter_people,
        colorScheme = ColorSchemes.Common,
        icon = PlaceholdersDefaults.PERSON.icon,
    ),
}

private val TmdbSearchableListItem.type
    get() = when (this) {
        is TmdbMovie -> SearchableMediaType.MOVIE
        is TmdbShow -> SearchableMediaType.SHOW
        is TmdbPerson -> SearchableMediaType.PERSON
        is TmdbCollection -> null
    }

private suspend fun TmdbSearchableListItem.toModel(language: TmdbLanguage): SearchResultItem? {
    return when (this) {
        is TmdbCollection -> null

        is TmdbMovie -> SearchResultItem(
            posterModel = posterImage?.toImageModel(),
            title = title,
            type = SearchableMediaType.MOVIE,
            tags = listOfNotNull(
                releaseDate?.year?.toString(),
                rating()?.formatted,
            ),
            navigate = { it.navigateToMovie(tmdbMovieId, toBaseMovie(language)) },
        )

        is TmdbPerson -> SearchResultItem(
            posterModel = profileImage?.toImageModel(),
            title = name,
            type = SearchableMediaType.PERSON,
            tags = emptyList(),
            navigate = { /* TODO */ },
        )

        is TmdbShow -> SearchResultItem(
            posterModel = posterImage?.toImageModel(),
            title = name,
            type = SearchableMediaType.SHOW,
            tags = listOfNotNull(firstAirDate?.year?.toString()),
            navigate = { it.navigateToShow(tmdbShowId, toBaseShow(language)) },
        )
    }
}
