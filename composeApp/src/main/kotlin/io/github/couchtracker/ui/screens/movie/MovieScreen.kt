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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.asWatchable
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.db.profile.movie.TmdbExternalMovieId
import io.github.couchtracker.db.profile.movie.UnknownExternalMovieId
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMemoryCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.ErrorScreen
import io.github.couchtracker.ui.components.LoadableContainer
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.isLoadingOr
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.valueOrNull
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private const val LOG_TAG = "MovieScreen"

@Serializable
data class MovieScreen(val movieId: String, val language: String) : Screen() {
    @Composable
    override fun content() {
        val tmdbMovie = when (val movieId = ExternalMovieId.parse(movieId)) {
            is TmdbExternalMovieId -> {
                TmdbMovie(movieId.id, TmdbLanguage.parse(language))
            }

            is UnknownExternalMovieId -> TODO()
        }
        Content(tmdbMovie)
    }
}

fun NavController.navigateToMovie(movie: TmdbMovie, preloadData: BaseTmdbMovie?) {
    if (preloadData != null) {
        TmdbMemoryCache.registerItem(preloadData)
    }
    navigate(MovieScreen(movie.id.toExternalId().serialize(), movie.language.serialize()))
}

@Composable
private fun Content(movie: TmdbMovie) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val tmdbCache = koinInject<TmdbCache>()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        var screenModel by remember {
            mutableStateOf<MovieScreenModelState>(
                MovieScreenModelState(
                    context = ctx,
                    movie = movie,
                    width = this.constraints.maxWidth,
                    height = this.constraints.maxHeight,
                    tmdbCache = tmdbCache,
                    scope = coroutineScope,
                ).also { it.load() },
            )
        }

        ErrorScreen(
            data = screenModel.baseDetails.map { },
            onError = { exception ->
                Surface {
                    DefaultErrorScreen(
                        errorMessage = exception.title.string(),
                        errorDetails = exception.details?.string(),
                        retry = {
                            coroutineScope.launch { screenModel.load() }
                        },
                    )
                }
            },
        ) {
            MovieScreenContent(
                movie = movie,
                model = screenModel,
                totalHeight = constraints.maxHeight,
                reloadMovie = {
                    coroutineScope.launch { screenModel.load() }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MovieScreenContent(
    movie: TmdbMovie,
    model: MovieScreenModelState,
    totalHeight: Int,
    reloadMovie: () -> Unit,
) {
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(FloatingToolbarExitDirection.Bottom)
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    //TODO
//    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
//        snackbarHostState = snackbarHostState,
//        state = model?.allDeferred.orEmpty(),
//        onRetry = { reloadMovie() },
//    )
    val backgroundColor by animateColorAsState(model.colorScheme.background)
    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = model.colorScheme,
        backgroundColor = { backgroundColor },
        watchedItemType = WatchedItemType.MOVIE,
        mediaRuntime = { model.fullDetails.valueOrNull()?.runtime },
        mediaLanguages = { listOfNotNull(model.fullDetails.valueOrNull()?.originalLanguage) },
        title = model.baseDetails.valueOrNull()?.title.orEmpty(),
        backdrop = model.baseDetails.valueOrNull()?.backdrop,
        modifier = Modifier.nestedScroll(toolbarScrollBehavior),
        floatingActionButton = {
            MovieToolbar(
                movie = movie,
                expanded = toolbarExpanded,
                onMarkAsWatched = {
                    scaffoldState.open(WatchedItemSheetMode.New(movie.id.toExternalId().asWatchable()))
                },
            )
        },
        snackbarHostState = snackbarHostState,
        content = { innerPadding ->
            OverviewScreenComponents.MoviePage(
                innerPadding = innerPadding,
                model = model,
                totalHeight = totalHeight,
                modifier = Modifier.floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { toolbarExpanded = true },
                    onCollapse = { toolbarExpanded = false },
                ),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MovieToolbar(
    movie: TmdbMovie,
    expanded: Boolean,
    onMarkAsWatched: () -> Unit,
) {
    val navController = LocalNavController.current
    val fullProfileData = LocalFullProfileDataContext.current
    val watchableId = movie.id.toExternalId().asWatchable()
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
                IconButton(onClick = { navController.navigateToWatchedItems(movie) }) {
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
    model: MovieScreenModelState,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    val baseDetails = model.baseDetails
    val fullDetails = model.fullDetails
    val images = model.images
    val credits = model.credits
    logCompositions(LOG_TAG, "Recomposing MoviePage")
    ContentList(innerPadding, modifier) {
        section("subtitle-2", credits.map { it.directorsString ?: "" }, titlePlaceholderLines = 2) {
            val tags = fullDetails.map { details ->
                listOfNotNull(
                    details.baseDetails.year?.toString(),
                    details.runtime?.toString(),
                    details.rating?.formatted,
                ) + details.genres.map { it.name }
            }
            if (tags.isLoadingOr { it.isNotEmpty() }) {
                item("subtitle-content") {
                    LoadableContainer(
                        tags,
                        content = { TagsComposable(tags = it) },
                        placeholder = { Spacer(Modifier.height(40.dp)) },
                    )
                }
            }
        }
        paragraphSection("overview", fullDetails.map { it.tagline }, baseDetails.map { it.overview }, titlePlaceholderLines = 2)
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.map { it.cast }, totalHeight = totalHeight)
        crewSection(credits.map { it.crew }, totalHeight = totalHeight)
        // To avoid overlaps with the FAB; see FabBaselineTokens.ContainerHeight
        item("fab-spacer") { Spacer(Modifier.height(56.dp)) }
    }
}
