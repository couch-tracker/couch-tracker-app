package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbTimeWindow
import io.github.couchtracker.R
import io.github.couchtracker.model.sort.SortableItemType
import io.github.couchtracker.model.sort.Sorter
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.settings.appSettings
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.MoviePortrait
import io.github.couchtracker.ui.components.MoviePortraitModel.SecondaryInfo
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.SortActionButton
import io.github.couchtracker.ui.components.SortedGrid
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.removeDuplicates
import io.github.couchtracker.utils.settings.get
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MoviesSection(
    innerPadding: PaddingValues,
    viewModel: MovieSectionViewModel = viewModel(),
) {
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }
    val snackbarHostState = remember { SnackbarHostState() }
    val modelError by viewModel.aggregateError().collectAsStateWithLifecycle(null)
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        error = { modelError },
        onRetry = { viewModel.retryAll() },
    )
    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        imageModel = R.drawable.aurora_borealis,
        title = R.string.main_section_movies.str(),
        actions = {
            when (MovieTab.entries[pagerState.currentPage]) {
                MovieTab.WATCHLIST -> BookmarkedMovieActions()
                else -> {}
            }
            MainSectionDefaults.DefaultAppBarActions()
        },
        tabText = { page -> Text(text = MovieTab.entries[page].displayName.str()) },
        snackbarHostState = snackbarHostState,
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (MovieTab.entries[page]) {
                    MovieTab.HISTORY -> WipMessageComposable(
                        gitHubIssueId = 126,
                        description = "All watched movies",
                    )
                    MovieTab.EXPLORE -> MovieExploreComposable(viewModel.exploreState)
                    MovieTab.WATCHLIST -> BookmarkedMoviesGrid(
                        movies = viewModel.watchlist,
                        emptyMessage = R.string.tab_movies_watchlist_empty.str(),
                        emptyDescription = R.string.tab_movies_watchlist_empty_description.str(),
                    )
                    MovieTab.CALENDAR -> WipMessageComposable(
                        gitHubIssueId = 129,
                        description = "A calendar of bookmarked movies, all 'relevant' releases",
                    )
                }
            }
        },
    )
}

@Composable
private fun BookmarkedMovieActions() {
    val cs = rememberCoroutineScope()
    val settings = appSettings()
    val current = settings.get { StyleAndBehavior.MoviesWatchlistSortOrder }.current
    SortActionButton(
        itemTypes = setOf(SortableItemType.MOVIE),
        currentSorter = current,
        setCurrentSorter = { newSorter ->
            cs.launch {
                settings.getSetting { StyleAndBehavior.MoviesWatchlistSortOrder }.set(newSorter)
            }
        },
    )
}

@Composable
private fun BookmarkedMoviesGrid(
    movies: Loadable<MovieSectionViewModel.BookmarkedMoviesModel>,
    emptyMessage: String,
    emptyDescription: String,
) {
    LoadableScreen(movies) { (sorter, movies) ->
        if (movies.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.BookmarkBorder,
                message = emptyMessage,
                details = emptyDescription,
            )
        } else {
            SortedGrid(
                sorter = sorter,
                sortedItems = movies,
                columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH),
                itemKey = { it.itemKey },
                itemComposable = { movie ->
                    MoviePortrait(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .animateItem(),
                        movieId = movie.movieId,
                        movieResult = movie.data.map { it.portraitModel },
                        secondaryInfo = when (sorter) {
                            is Sorter.ByReleaseDate -> SecondaryInfo.DEFAULT_SECONDARY_INFO + SecondaryInfo.Year
                            is Sorter.ByTmdbRating -> SecondaryInfo.DEFAULT_SECONDARY_INFO + SecondaryInfo.Rating
                            is Sorter.ByRuntime, is Sorter.ByEpisodeRuntime -> SecondaryInfo.DEFAULT_SECONDARY_INFO + SecondaryInfo.Runtime
                            is Sorter.Alphabetical, is Sorter.Random -> SecondaryInfo.DEFAULT_SECONDARY_INFO
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun MovieExploreComposable(
    tabState: MovieExploreTabState,
) {
    val lazyItems = tabState.movieFlow.collectAsLazyPagingItems()
    PaginatedGrid(lazyItems, columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH)) { movie, _ ->
        MoviePortrait(Modifier.fillMaxWidth(), movie, SecondaryInfo.EXPLORE_SECONDARY_INFO)
    }
}

enum class MovieTab(
    @StringRes
    val displayName: Int,
) {
    HISTORY(R.string.tab_movies_history),
    EXPLORE(R.string.tab_movies_explore),
    WATCHLIST(R.string.tab_movies_watchlist),
    CALENDAR(R.string.tab_movies_calendar),
}

class MovieExploreTabState(viewModelScope: CoroutineScope) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val movieFlow = AppSettings.get { Tmdb.Languages }
        .map { it.current.apiLanguage }
        .distinctUntilChanged()
        .flatMapLatest { tmdbLanguage ->
            tmdbPager(
                downloader = { page ->
                    trending.getTrendingMovies(timeWindow = TmdbTimeWindow.DAY, page = page, language = tmdbLanguage.apiParameter)
                },
                mapper = { movie -> movie.toMoviePortraitModels(tmdbLanguage, movie.rating()) },
            ).flow
        }
        .removeDuplicates { it.id }
        .cachedIn(viewModelScope)
}
