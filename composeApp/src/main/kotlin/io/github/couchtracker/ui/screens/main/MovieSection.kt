package io.github.couchtracker.ui.screens.main

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbTimeWindow
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.MoviePortrait
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.removeDuplicates
import io.github.couchtracker.utils.settings.get
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Composable
fun MoviesSection(
    innerPadding: PaddingValues,
    viewModel: MovieSectionViewModel = viewModel(),
) {
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        errors = { viewModel.allErrors },
        onRetry = { viewModel.retryAll() },
    )
    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        imageModel = R.drawable.aurora_borealis,
        title = R.string.main_section_movies.str(),
        actions = {
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
                    MovieTab.EXPLORE -> MovieListComposable(viewModel.exploreState)
                    MovieTab.WATCHLIST -> WatchlistedMoviesGrid(
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
private fun WatchlistedMoviesGrid(
    movies: Loadable<List<Pair<ExternalMovieId, CouchTrackerResult<MoviePortraitModel>>>>,
    emptyMessage: String,
    emptyDescription: String,
) {
    LoadableScreen(movies) { movies ->
        if (movies.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.BookmarkBorder,
                message = emptyMessage,
                details = emptyDescription,
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH),
                contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = OverviewScreenComponents.LIST_BOTTOM_SPACE),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(movies) { (movieId, movieModel) ->
                    MoviePortrait(
                        modifier = Modifier.fillMaxWidth(),
                        movieId = movieId,
                        movieResult = movieModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieListComposable(
    tabState: MovieExploreTabState,
) {
    val lazyItems = tabState.movieFlow.collectAsLazyPagingItems()
    PaginatedGrid(lazyItems, columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH)) { movie, _ ->
        MoviePortrait(Modifier.fillMaxWidth(), movie)
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

class MovieExploreTabState(context: Context, viewModelScope: CoroutineScope) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val movieFlow = AppSettings.get { Tmdb.Languages }
        .map { it.current.apiLanguage }
        .distinctUntilChanged()
        .flatMapLatest { tmdbLanguage ->
            tmdbPager(
                downloader = { page ->
                    trending.getTrendingMovies(timeWindow = TmdbTimeWindow.DAY, page = page, language = tmdbLanguage.apiParameter)
                },
                mapper = { movie -> movie.toMoviePortraitModels(context, tmdbLanguage) },
            ).flow
        }
        .removeDuplicates { it.id }
        .cachedIn(viewModelScope)
}
