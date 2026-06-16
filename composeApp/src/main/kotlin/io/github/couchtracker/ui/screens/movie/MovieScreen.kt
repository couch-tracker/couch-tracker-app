@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens.movie

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalMovieId
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.actions.Actions
import io.github.couchtracker.ui.actions.ActionsHorizontalFloatingToolbar
import io.github.couchtracker.ui.actions.MovieActions
import io.github.couchtracker.ui.components.CouchTrackerScreenScaffold
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.ResultScreen
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.viewModelApplication
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform

private const val LOG_TAG = "MovieScreen"

@Serializable
data class MovieScreen(val movieId: String) : Screen() {

    @Composable
    override fun Content() {
        val externalMovieId: ExternalMovieId = ExternalMovieId.parse(movieId)
        when (externalMovieId) {
            is TmdbExternalMovieId -> {
                val viewModel = viewModel {
                    MovieScreenViewModel(
                        application = viewModelApplication(),
                        movieId = externalMovieId.id,
                    )
                }
                val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Movie
                ScreenContainer(colorScheme) {
                    val actions = MovieActions(externalMovieId) {
                        WatchedItemSheetMode.New.Movie(
                            externalMovieId,
                            mediaRuntime = viewModel.fullDetails.resultValueOrNull()?.runtime,
                            mediaLanguages = listOfNotNull(viewModel.fullDetails.resultValueOrNull()?.originalLanguage),
                        )
                    }
                    Content(viewModel, actions)
                }
            }
            is UnknownExternalMovieId -> {
                ScreenContainer(ColorSchemes.Movie) {
                    val actions = MovieActions(externalMovieId) {
                        WatchedItemSheetMode.New.Movie(
                            externalMovieId,
                            mediaRuntime = null,
                            mediaLanguages = emptyList(),
                        )
                    }
                    DefaultErrorScreen(
                        error = UnsupportedItemError(externalMovieId),
                        manageItemActions = actions,
                    )
                }
            }
        }
    }
}

fun NavController.navigateToMovie(id: ExternalMovieId, preloadData: BaseTmdbMovie?) {
    if (preloadData != null) {
        KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().registerItem(preloadData)
    }
    navigate(MovieScreen(ExternalMovieId.serialize(id)))
}

@Composable
private fun Content(viewModel: MovieScreenViewModel, actions: Actions) {
    logCompositions(LOG_TAG, "Recomposing Content")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ResultScreen(
            error = viewModel.baseDetails.resultErrorOrNull(),
            onError = { apiError ->
                DefaultErrorScreen(
                    error = apiError,
                    retry = { viewModel.retryAll() },
                    manageItemActions = actions,
                )
            },
        ) {
            MovieScreenContent(
                viewModel = viewModel,
                totalHeight = constraints.maxHeight,
                reloadMovie = { viewModel.retryAll() },
                actions = actions,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MovieScreenContent(
    viewModel: MovieScreenViewModel,
    totalHeight: Int,
    reloadMovie: () -> Unit,
    actions: Actions,
) {
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        errors = { viewModel.allErrors },
        onRetry = reloadMovie,
    )
    logCompositions(LOG_TAG, "Recomposing MovieScreenContent")
    CouchTrackerScreenScaffold(
        title = { viewModel.baseDetails.resultValueOrNull()?.title.orEmpty() },
        backdrop = { viewModel.baseDetails.resultValueOrNull()?.backdrop },
        floatingActionButton = {
            ActionsHorizontalFloatingToolbar(actions, expanded = toolbarExpanded)
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

@Composable
private fun OverviewScreenComponents.MoviePage(
    innerPadding: PaddingValues,
    viewModel: MovieScreenViewModel,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    logCompositions(LOG_TAG, "Recomposing MoviePage")
    ContentList(innerPadding, modifier) {
        section(title = { textBlock("directors", viewModel.credits.mapResult { it.directorsString }, placeholderLines = 2) }) {
            tags(
                tags = viewModel.fullDetails.mapResult { details ->
                    listOfNotNull(
                        details.baseDetails.year?.toString(),
                        details.runtimeString,
                        details.rating?.formatted,
                    ) + details.genres.map { it.name }
                },
            )
        }
        section(title = { tagline(viewModel.fullDetails.mapResult { it.tagline }) }) {
            overview(viewModel.baseDetails.mapResult { it.overview })
        }
        imagesSection(viewModel.images, totalHeight = totalHeight)
        castSection(viewModel.credits.mapResult { it.cast }, totalHeight = totalHeight)
        crewSection(viewModel.credits.mapResult { it.crew }, totalHeight = totalHeight)
        // To avoid overlaps with the FAB; see FabBaselineTokens.ContainerHeight
        item("fab-spacer") { Spacer(Modifier.height(56.dp)) }
    }
}
