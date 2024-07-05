@file:OptIn(ExperimentalFoundationApi::class)

package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.moviebase.tmdb.Tmdb3
import app.moviebase.tmdb.model.TmdbDiscover
import app.moviebase.tmdb.model.TmdbDiscoverMovieSortBy
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
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource

// TODO: do better
private val LANGUAGE = TmdbLanguage.ENGLISH

@Composable
fun MoviesSection(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = MovieTab.EXPLORE.ordinal) { MovieTab.entries.size }
    val movieMaxW = with(LocalDensity.current) {
        (MoviePortraitModel.SUGGESTED_WIDTH * 2).roundToPx()
    }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(Res.drawable.aurora_borealis),
        tabText = { page -> Text(text = MovieTab.entries[page].displayName.str()) },
        page = { page ->
            MovieListComposable { tmdb3 ->
                val pageResult = when (MovieTab.entries[page]) {
                    MovieTab.TIMELINE ->
                        tmdb3.movies.popular(page = 1, LANGUAGE.apiParameter)

                    MovieTab.EXPLORE ->
                        tmdb3.trending.getTrendingMovies(TmdbTimeWindow.DAY, page = 1, LANGUAGE.apiParameter)

                    MovieTab.FOLLOWED ->
                        tmdb3.discover.discoverMovie(
                            page = 1,
                            language = LANGUAGE.apiParameter,
                            region = null,
                            TmdbDiscover.Movie(
                                sortBy = TmdbDiscoverMovieSortBy.POPULARITY,
                            ),
                        )

                    MovieTab.UP_NEXT ->
                        tmdb3.discover.discoverMovie(
                            page = 1,
                            language = LANGUAGE.apiParameter,
                            region = null,
                            TmdbDiscover.Movie(
                                sortBy = TmdbDiscoverMovieSortBy.VOTE_AVERAGE,
                            ),
                        )

                    MovieTab.CALENDAR ->
                        tmdb3.discover.discoverMovie(
                            page = 1,
                            language = LANGUAGE.apiParameter,
                            region = null,
                            TmdbDiscover.Movie(
                                sortBy = TmdbDiscoverMovieSortBy.RELEASE_DATE,
                            ),
                        )
                }
                pageResult
                    .results
                    .toMoviePortraitModels(context, LANGUAGE, movieMaxW)
            }
        },
    )
}

// TODO: state isn't saved here. Movies are getting downloaded at every page swipe/screen change
@Composable
private fun MovieListComposable(
    downloadFunction: suspend (Tmdb3) -> List<MoviePortraitModel>,
) {
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<Loadable<List<MoviePortraitModel>>>(Loadable.Loading) }

    suspend fun download() {
        state = Loadable.Loading
        try {
            val movies = tmdbDownload { tmdb3 ->
                downloadFunction(tmdb3)
            }
            state = Loadable.Loaded(movies)
        } catch (e: TmdbException) {
            // TODO: translate
            state = Loadable.Error(e.message ?: "Error")
        }
    }
    LaunchedEffect(Unit) {
        download()
    }

    LoadableScreen(
        state,
        onError = { message ->
            DefaultErrorScreen(message) {
                coroutineScope.launch { download() }
            }
        },
    ) { movies ->
        MovieGrid(movies)
    }
}

private enum class MovieTab(
    val displayName: StringResource,
) {
    TIMELINE(Res.string.tab_movie_timeline),
    EXPLORE(Res.string.tab_movie_explore),
    FOLLOWED(Res.string.tab_movie_followed),
    UP_NEXT(Res.string.tab_movie_up_next),
    CALENDAR(Res.string.tab_movie_calendar),
}
