package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.Tmdb3
import app.moviebase.tmdb.model.TmdbDiscover
import app.moviebase.tmdb.model.TmdbDiscoverMovieSortBy
import app.moviebase.tmdb.model.TmdbMoviePageResult
import app.moviebase.tmdb.model.TmdbTimeWindow
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.components.MoviePortrait
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.ui.screens.movie.navigateToMovie
import io.github.couchtracker.utils.removeDuplicates
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope

// TODO: do better
val TMDB_LANGUAGE = TmdbLanguage.ENGLISH

class MovieSectionViewModel : ViewModel() {
    val tabStates: Map<MovieTab, MovieTabState> = MovieTab.entries.associateWith {
        MovieTabState(viewModelScope, it)
    }
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
        backgroundImage = painterResource(R.drawable.aurora_borealis),
        actions = {
            MainSectionDefaults.SearchButton(
                onOpenSearch = {
                    navController.navigate(SearchScreen(filter = SearchableMediaType.MOVIE))
                },
            )
        },
        tabText = { page -> Text(text = MovieTab.entries[page].displayName.str()) },
        page = { page ->
            val tab = MovieTab.entries[page]
            MovieListComposable(viewModel.tabStates.getValue(tab))
        },
    )
}

@Composable
private fun MovieListComposable(
    tabState: MovieTabState,
) {
    val lazyItems = tabState.movieFlow.collectAsLazyPagingItems()
    val navController = LocalNavController.current
    PaginatedGrid(lazyItems, columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH)) { movie ->
        MoviePortrait(Modifier.fillMaxWidth(), movie) {
            navController.navigateToMovie(it.movie)
        }
    }
}

enum class MovieTab(
    @StringRes
    val displayName: Int,
    val movieDownloader: suspend Tmdb3.(page: Int) -> TmdbMoviePageResult,
) {
    TIMELINE(
        displayName = R.string.tab_movies_timeline,
        movieDownloader = { page -> movies.popular(page = page, TMDB_LANGUAGE.apiParameter) },
    ),
    EXPLORE(
        displayName = R.string.tab_movies_explore,
        movieDownloader = { page ->
            trending.getTrendingMovies(TmdbTimeWindow.DAY, page = page, TMDB_LANGUAGE.apiParameter)
        },
    ),
    FOLLOWED(
        displayName = R.string.tab_movies_followed,
        movieDownloader = { page ->
            discover.discoverMovie(
                page = page,
                language = TMDB_LANGUAGE.apiParameter,
                region = null,
                TmdbDiscover.Movie(sortBy = TmdbDiscoverMovieSortBy.POPULARITY),
            )
        },
    ),
    UP_NEXT(
        displayName = R.string.tab_movies_up_next,
        movieDownloader = { page ->
            discover.discoverMovie(
                page = page,
                language = TMDB_LANGUAGE.apiParameter,
                region = null,
                TmdbDiscover.Movie(sortBy = TmdbDiscoverMovieSortBy.VOTE_AVERAGE),
            )
        },
    ),
    CALENDAR(
        displayName = R.string.tab_movies_calendar,
        movieDownloader = { page ->
            discover.discoverMovie(
                page = page,
                language = TMDB_LANGUAGE.apiParameter,
                region = null,
                TmdbDiscover.Movie(sortBy = TmdbDiscoverMovieSortBy.RELEASE_DATE),
            )
        },
    ),
}

class MovieTabState(
    viewModelScope: CoroutineScope,
    private val tab: MovieTab,
) {

    private val pager = tmdbPager(
        downloader = { page ->
            tab.movieDownloader(this, page)
        },
        mapper = { movie ->
            movie.toMoviePortraitModels(TMDB_LANGUAGE, ImagePreloadOptions.DoNotPreload)
        },
    )
    val movieFlow = pager.flow.removeDuplicates { it.movie.id.value }.cachedIn(viewModelScope)
}
