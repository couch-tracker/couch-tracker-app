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
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MediaScreenScaffold
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.awaitAsLoadable
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

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

fun NavController.navigateToMovie(movie: TmdbMovie) {
    navigate(MovieScreen(movie.id.toExternalId().serialize(), movie.language.serialize()))
}

@Composable
private fun Content(movie: TmdbMovie) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val tmdbCache = koinInject<TmdbCache>()
    var screenModel by remember { mutableStateOf<Loadable<MovieScreenModel, ApiException>>(Loadable.Loading) }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        val maxWidth = this.constraints.maxWidth
        val maxHeight = this.constraints.maxHeight
        suspend fun CoroutineScope.load() {
            if (screenModel is Result.Error) {
                screenModel = Loadable.Loading
            }
            screenModel = loadMovie(
                ctx,
                tmdbCache,
                movie,
                width = maxWidth,
                height = maxHeight,
            )
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

    MediaScreenScaffold(
        watchedItemSheetScaffoldState = scaffoldState,
        colorScheme = model.colorScheme,
        watchedItemType = WatchedItemType.MOVIE,
        mediaRuntime = model.runtime,
        mediaLanguages = listOf(model.originalLanguage),
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
    model: MovieScreenModel,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    val images = model.images.awaitAsLoadable()
    val credits = model.credits.awaitAsLoadable()
    ContentList(innerPadding, modifier) {
        section("subtitle") {
            val director = credits.map { it.director }
            val tags = listOfNotNull(
                model.year?.toString(),
                model.runtime?.toString(),
                model.rating?.format(),
            ) + model.genres.map { it.name }

            val hasDirector = director is Result.Value && director.value.isNotEmpty()
            val hasTags = tags.isNotEmpty()
            if (hasDirector || hasTags) {
                item("subtitle-content") {
                    AnimatedVisibility(hasDirector, enter = expandVertically()) {
                        val directors = formatAndList((director as? Result.Value)?.value?.map { it.name }.orEmpty())
                        Paragraph(R.string.movie_by_director.str(directors))
                    }
                    SpaceBetweenItems()
                    if (hasTags) {
                        TagsComposable(tags = tags)
                    }
                }
            }
        }
        paragraphSection("overview", model.tagline, model.overview)
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.map { it.cast }, totalHeight = totalHeight)
        crewSection(credits.map { it.crew }, totalHeight = totalHeight)
        // To avoid overlaps with the FAB; see FabBaselineTokens.ContainerHeight
        item("fab-spacer") { Spacer(Modifier.height(56.dp)) }
    }
}
