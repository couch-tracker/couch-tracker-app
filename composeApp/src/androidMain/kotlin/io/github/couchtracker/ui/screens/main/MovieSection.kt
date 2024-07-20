@file:OptIn(ExperimentalFoundationApi::class, ExperimentalCoroutinesApi::class)

package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.moviebase.tmdb.Tmdb3
import app.moviebase.tmdb.model.TmdbDiscover
import app.moviebase.tmdb.model.TmdbDiscoverMovieSortBy
import app.moviebase.tmdb.model.TmdbMovie
import app.moviebase.tmdb.model.TmdbMoviePageResult
import app.moviebase.tmdb.model.TmdbTimeWindow
import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.aurora_borealis
import couch_tracker_app.composeapp.generated.resources.tab_movie_calendar
import couch_tracker_app.composeapp.generated.resources.tab_movie_explore
import couch_tracker_app.composeapp.generated.resources.tab_movie_followed
import couch_tracker_app.composeapp.generated.resources.tab_movie_timeline
import couch_tracker_app.composeapp.generated.resources.tab_movie_up_next
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.tmdbDownload
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MovieGrid
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.seconds

// TODO: do better
private val LANGUAGE = TmdbLanguage.ENGLISH

class MovieSectionViewModel : ViewModel() {
    val tabStates: Map<MovieTab, MovieListStateHolder> = MovieTab.entries.associateWith {
        MovieListStateHolder(viewModelScope, it)
    }
}

@Composable
fun MoviesSection(
    innerPadding: PaddingValues,
    viewModel: MovieSectionViewModel = viewModel(),
) {
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(Res.drawable.aurora_borealis),
        tabText = { page -> Text(text = MovieTab.entries[page].displayName.str()) },
        page = { page ->
            val tab = MovieTab.entries[page]
            MovieListComposable(viewModel.tabStates.getValue(tab))
        },
    )
}

@Composable
private fun MovieListComposable(
    tabState: MovieListStateHolder,
) {
    val context = LocalContext.current
    val movieMaxW = with(LocalDensity.current) {
        (MoviePortraitModel.SUGGESTED_WIDTH * 2).roundToPx()
    }
    val coroutineScope = rememberCoroutineScope()
    val moviePortraitModelList: Flow<Loadable<List<MoviePortraitModel>>> = remember(tabState) {
        tabState.moviesFlow.mapLatest { movies ->
            movies.map {
                it.toMoviePortraitModels(context, LANGUAGE, movieMaxW)
            }
        }
    }
    val state by moviePortraitModelList.collectAsStateWithLifecycle(Loadable.Loading)

    LoadableScreen(
        state,
        onError = { message ->
            DefaultErrorScreen(message) {
                coroutineScope.launch { tabState.retryDownload() }
            }
        },
    ) { movies ->
        MovieGrid(movies)
    }
}

enum class MovieTab(
    val displayName: StringResource,
    val movieDownloader: suspend Tmdb3.() -> TmdbMoviePageResult,
) {
    TIMELINE(
        displayName = Res.string.tab_movie_timeline,
        movieDownloader = { movies.popular(page = 1, LANGUAGE.apiParameter) },
    ),
    EXPLORE(
        displayName = Res.string.tab_movie_explore,
        movieDownloader = { trending.getTrendingMovies(TmdbTimeWindow.DAY, page = 1, LANGUAGE.apiParameter) },
    ),
    FOLLOWED(
        displayName = Res.string.tab_movie_followed,
        movieDownloader = {
            discover.discoverMovie(
                page = 1,
                language = LANGUAGE.apiParameter,
                region = null,
                TmdbDiscover.Movie(sortBy = TmdbDiscoverMovieSortBy.POPULARITY),
            )
        },
    ),
    UP_NEXT(
        displayName = Res.string.tab_movie_up_next,
        movieDownloader = {
            discover.discoverMovie(
                page = 1,
                language = LANGUAGE.apiParameter,
                region = null,
                TmdbDiscover.Movie(sortBy = TmdbDiscoverMovieSortBy.VOTE_AVERAGE),
            )
        },
    ),
    CALENDAR(
        displayName = Res.string.tab_movie_calendar,
        movieDownloader = {
            discover.discoverMovie(
                page = 1,
                language = LANGUAGE.apiParameter,
                region = null,
                TmdbDiscover.Movie(sortBy = TmdbDiscoverMovieSortBy.RELEASE_DATE),
            )
        },
    ),
}

class MovieListStateHolder(
    private val cs: CoroutineScope,
    private val page: MovieTab,
) {
    private val tokensFlow = MutableStateFlow(Any())
    val moviesFlow: SharedFlow<Loadable<List<TmdbMovie>>> = tokensFlow.transformLatest {
        emit(Loadable.Loading)
        try {
            delay(1.seconds)
            val moviesPage = tmdbDownload { tmdb3 ->
                page.movieDownloader(tmdb3).results
            }
            emit(Loadable.Loaded(moviesPage))
        } catch (e: TmdbException) {
            // TODO: translate
            emit(Loadable.Error(e.message ?: "Error"))
        }
    }.shareIn(cs, SharingStarted.Lazily, replay = 1)

    suspend fun retryDownload() {
        cs.launch {
            tokensFlow.emit(Any())
        }.join()
    }
}
