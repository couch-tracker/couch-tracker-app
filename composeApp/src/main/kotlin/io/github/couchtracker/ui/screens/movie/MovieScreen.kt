@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens.movie

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.asWatchable
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.db.profile.movie.TmdbExternalMovieId
import io.github.couchtracker.db.profile.movie.UnknownExternalMovieId
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.ResultScreen
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform

private const val LOG_TAG = "MovieScreen"

@Serializable
data class MovieScreen(val movieId: String) : Screen() {
    @Composable
    override fun content() {
        val externalMovieId: ExternalMovieId = ExternalMovieId.parse(movieId)
        val movieId = when (externalMovieId) {
            is TmdbExternalMovieId -> {
                externalMovieId.id
            }
            is UnknownExternalMovieId -> TODO()
        }

        Content(
            viewModel {
                MovieScreenViewModel(
                    application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                    externalMovieId = externalMovieId,
                    movieId = movieId,
                )
            },
        )
    }
}

fun NavController.navigateToMovie(id: TmdbMovieId, preloadData: BaseTmdbMovie?) {
    if (preloadData != null) {
        KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().registerItem(preloadData)
    }
    navigate(MovieScreen(id.toExternalId().serialize()))
}

@Composable
private fun Content(viewModel: MovieScreenViewModel) {
    val coroutineScope = rememberCoroutineScope()
    logCompositions(LOG_TAG, "Recomposing Content")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ResultScreen(
            error = viewModel.baseDetails.resultErrorOrNull(),
            onError = { exception ->
                Surface {
                    DefaultErrorScreen(
                        errorMessage = exception.title.string(),
                        errorDetails = exception.details?.string(),
                        retry = {
                            coroutineScope.launch { viewModel.retryAll() }
                        },
                    )
                }
            },
        ) {
            MovieScreenContent(
                externalMovieId = viewModel.externalMovieId,
                viewModel = viewModel,
                totalHeight = constraints.maxHeight,
                reloadMovie = {
                    coroutineScope.launch { viewModel.retryAll() }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MovieScreenContent(
    externalMovieId: ExternalMovieId,
    viewModel: MovieScreenViewModel,
    totalHeight: Int,
    reloadMovie: () -> Unit,
) {
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(FloatingToolbarExitDirection.Bottom)
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        loadable = { viewModel.allLoadables },
        onRetry = reloadMovie,
    )
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Movie
    val backgroundColor by animateColorAsState(colorScheme.background)
    logCompositions(LOG_TAG, "Recomposing MovieScreenContent")
    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = colorScheme,
        watchedItemType = WatchedItemType.MOVIE,
        mediaRuntime = { viewModel.fullDetails.resultValueOrNull()?.runtime },
        mediaLanguages = { listOfNotNull(viewModel.fullDetails.resultValueOrNull()?.originalLanguage) },
        backgroundColor = { backgroundColor },
        title = viewModel.baseDetails.resultValueOrNull()?.title.orEmpty(),
        backdrop = viewModel.baseDetails.resultValueOrNull()?.backdrop,
        modifier = Modifier.nestedScroll(toolbarScrollBehavior),
        floatingActionButton = {
            MovieToolbar(
                externalMovieId = externalMovieId,
                expanded = toolbarExpanded,
                onMarkAsWatched = {
                    scaffoldState.open(WatchedItemSheetMode.New(externalMovieId.asWatchable()))
                },
            )
        },
        snackbarHostState = snackbarHostState,
        content = { innerPadding ->
            OverviewScreenComponents.MoviePage(
                viewModel = viewModel,
                modifier = Modifier.floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { toolbarExpanded = true },
                    onCollapse = { toolbarExpanded = false },
                ),
                innerPadding = innerPadding,
                totalHeight = totalHeight,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MovieToolbar(
    externalMovieId: ExternalMovieId,
    expanded: Boolean,
    onMarkAsWatched: () -> Unit,
) {
    val navController = LocalNavController.current
    val fullProfileData = LocalFullProfileDataContext.current
    val watchableId = externalMovieId.asWatchable()
    val watchCount = fullProfileData.watchedItems.count { it.itemId == watchableId }
    HorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(onClick = onMarkAsWatched) {
                Icon(Icons.Filled.Check, R.string.mark_movie_as_watched.str())
            }
        },
        content = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.AutoMirrored.Default.List, contentDescription = "TODO") // TODO
            }
            BadgedBox(
                badge = {
                    if (watchCount > 0) {
                        Badge { Text(watchCount.toString()) }
                    }
                },
            ) {
                IconButton(onClick = { navController.navigateToWatchedItems(watchableId) }) {
                    Icon(Icons.Default.Checklist, contentDescription = R.string.viewing_history.str())
                }
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = "TODO") // TODO
            }
        },
    )
}

@Composable
private fun OverviewScreenComponents.MoviePage(
    innerPadding: PaddingValues,
    viewModel: MovieScreenViewModel,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    val baseDetails = viewModel.baseDetails
    val fullDetails = viewModel.fullDetails
    val images = viewModel.images
    val credits = viewModel.credits
    ContentList(innerPadding, modifier) {
        section(title = { textBlock("directors", credits.mapResult { it.directorsString }, placeholderLines = 2) }) {
            tags(
                tags = fullDetails.mapResult { details ->
                    listOfNotNull(
                        details.baseDetails.year?.toString(),
                        details.runtime?.toString(),
                        details.rating?.formatted,
                    ) + details.genres.map { it.name }
                },
            )
        }
        section(title = { tagline(fullDetails.mapResult { it.tagline }) }) {
            overview(baseDetails.mapResult { it.overview })
        }
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.mapResult { it.cast }, totalHeight = totalHeight)
        crewSection(credits.mapResult { it.crew }, totalHeight = totalHeight)
        // To avoid overlaps with the FAB; see FabBaselineTokens.ContainerHeight
        item("fab-spacer") { Spacer(Modifier.height(56.dp)) }
    }
}
