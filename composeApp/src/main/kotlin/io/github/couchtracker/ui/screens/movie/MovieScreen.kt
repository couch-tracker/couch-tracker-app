@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens.movie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import io.github.couchtracker.settings.LocalAppSettingsContext
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.awaitAsLoadable
import io.github.couchtracker.utils.logExecutionTime
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform

private const val LOG_TAG = "MovieScreen"

@Serializable
data class MovieScreen(val movieId: String) : Screen() {
    @Composable
    override fun content() {
        when (val movieId = ExternalMovieId.parse(movieId)) {
            is TmdbExternalMovieId -> {
                val tmdbLanguages = LocalAppSettingsContext.current.get { Tmdb.Languages }.current
                Content(TmdbMovie(movieId.id, tmdbLanguages))
            }
            is UnknownExternalMovieId -> TODO()
        }
    }
}

fun NavController.navigateToMovie(id: TmdbMovieId, preloadData: BaseTmdbMovie?) {
    if (preloadData != null) {
        KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().registerItem(preloadData)
    }
    navigate(MovieScreen(id.toExternalId().serialize()))
}

@Composable
private fun Content(movie: TmdbMovie) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var screenModel by remember { mutableStateOf<ApiLoadable<MovieScreenModel>>(Loadable.Loading) }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        val maxWidth = this.constraints.maxWidth
        val maxHeight = this.constraints.maxHeight
        suspend fun load() {
            screenModel = Loadable.Loading
            screenModel = coroutineScope.async(Dispatchers.Default) {
                logExecutionTime(LOG_TAG, "Loading movie") {
                    Loadable.Loaded(
                        coroutineScope.loadMovie(
                            ctx = ctx,
                            movie = movie,
                            width = maxWidth,
                            height = maxHeight,
                        ),
                    )
                }
            }.await()
        }
        LaunchedEffect(movie) {
            load()
        }

        LoadableScreen(
            data = screenModel,
            onError = { exception ->
                Surface {
                    DefaultErrorScreen(
                        errorMessage = exception.title.string(),
                        errorDetails = exception.details?.string(),
                        retry = {
                            coroutineScope.launch { load() }
                        },
                    )
                }
            },
        ) { model ->
            MovieScreenContent(
                model = model,
                totalHeight = constraints.maxHeight,
                reloadMovie = {
                    coroutineScope.launch { load() }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MovieScreenContent(
    model: MovieScreenModel,
    totalHeight: Int,
    reloadMovie: () -> Unit,
) {
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(FloatingToolbarExitDirection.Bottom)
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        state = model.allDeferred,
        onRetry = { reloadMovie() },
    )
    val fullDetails = model.fullDetails.awaitAsLoadable()

    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = model.colorScheme,
        watchedItemType = WatchedItemType.MOVIE,
        mediaRuntime = fullDetails.resultValueOrNull()?.runtime,
        mediaLanguages = listOfNotNull(fullDetails.resultValueOrNull()?.originalLanguage),
        title = model.title,
        backdrop = model.backdrop,
        modifier = Modifier.nestedScroll(toolbarScrollBehavior),
        floatingActionButton = {
            MovieToolbar(
                movie = model.movie,
                expanded = toolbarExpanded,
                onMarkAsWatched = {
                    scaffoldState.open(WatchedItemSheetMode.New(model.movie.id.toExternalId().asWatchable()))
                },
            )
        },
        snackbarHostState = snackbarHostState,
        content = { innerPadding ->
            OverviewScreenComponents.MoviePage(
                modifier = Modifier.floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { toolbarExpanded = true },
                    onCollapse = { toolbarExpanded = false },
                ),
                innerPadding = innerPadding,
                totalHeight = totalHeight,
                model = model,
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
    model: MovieScreenModel,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    val fullDetails = model.fullDetails.awaitAsLoadable()
    val images = model.images.awaitAsLoadable()
    val credits = model.credits.awaitAsLoadable()
    ContentList(innerPadding, modifier) {
        section("subtitle") {
            val directors = credits.mapResult { it.directorsString }.resultValueOrNull()
            val tags = fullDetails.mapResult { details ->
                listOfNotNull(
                    model.year?.toString(),
                    details.runtime?.toString(),
                    model.rating?.formatted,
                ) + details.genres.map { it.name }
            }.resultValueOrNull().orEmpty()

            val hasDirectors = directors != null
            val hasTags = tags.isNotEmpty()
            item("subtitle-content") {
                if (hasDirectors || hasTags) {
                    AnimatedVisibility(hasDirectors, enter = expandVertically()) {
                        Paragraph(directors)
                    }
                    SpaceBetweenItems()
                    AnimatedVisibility(hasTags, enter = expandVertically()) {
                        TagsComposable(tags = tags)
                    }
                }
            }
        }
        paragraphSection("overview", fullDetails.mapResult { it.tagline }.resultValueOrNull(), model.overview)
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.mapResult { it.cast }, totalHeight = totalHeight)
        crewSection(credits.mapResult { it.crew }, totalHeight = totalHeight)
        // To avoid overlaps with the FAB; see FabBaselineTokens.ContainerHeight
        item("fab-spacer") { Spacer(Modifier.height(56.dp)) }
    }
}
