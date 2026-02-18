package io.github.couchtracker.ui.screens.main

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbTimeWindow
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.ui.components.MoviePortrait
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.utils.removeDuplicates
import io.github.couchtracker.utils.settings.get
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class MovieSectionViewModel(application: Application) : AndroidViewModel(application) {
    val exploreState = MovieExploreTabState(application.applicationContext, viewModelScope)
}

@Composable
fun MoviesSection(
    innerPadding: PaddingValues,
    viewModel: MovieSectionViewModel = viewModel(),
) {
    val navController = LocalNavController.current
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        imageModel = R.drawable.aurora_borealis,
        actions = {
            MainSectionDefaults.SearchButton(
                onOpenSearch = {
                    navController.navigate(SearchScreen(filter = SearchableMediaType.MOVIE))
                },
            )
        },
        tabText = { page -> Text(text = MovieTab.entries[page].displayName.str()) },
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (MovieTab.entries[page]) {
                    MovieTab.HISTORY -> WipMessageComposable(
                        gitHubIssueId = 126,
                        description = "All watched movies",
                    )
                    MovieTab.EXPLORE -> MovieListComposable(viewModel.exploreState)
                    MovieTab.WATCHLIST -> WipMessageComposable(
                        gitHubIssueId = 130,
                        description = "All bookmarked movies (watching a movie removes it from the bookmarks)",
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
private fun MovieListComposable(
    tabState: MovieExploreTabState,
) {
    val lazyItems = tabState.movieFlow.collectAsLazyPagingItems()
    val navController = LocalNavController.current
    PaginatedGrid(lazyItems, columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH)) { movie, _ ->
        MoviePortrait(Modifier.fillMaxWidth(), movie?.second) {
            navController.navigateToMovie(it.id, preloadData = movie?.first)
        }
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
                mapper = { movie ->
                    val baseModel = movie.toBaseMovie(tmdbLanguage)
                    baseModel to movie.toMoviePortraitModels(context)
                },
            ).flow
        }
        .removeDuplicates { it.second.id }
        .cachedIn(viewModelScope)
}
