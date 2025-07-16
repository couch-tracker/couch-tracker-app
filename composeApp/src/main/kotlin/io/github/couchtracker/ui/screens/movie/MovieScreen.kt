@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.asWatchable
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.db.profile.movie.TmdbExternalMovieId
import io.github.couchtracker.db.profile.movie.UnknownExternalMovieId
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.intl.datetime.MonthSkeleton
import io.github.couchtracker.intl.datetime.Skeletons
import io.github.couchtracker.intl.datetime.TimeSkeleton
import io.github.couchtracker.intl.datetime.TimezoneSkeleton
import io.github.couchtracker.intl.datetime.YearSkeleton
import io.github.couchtracker.intl.datetime.localized
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetScaffold
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.hours

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
        suspend fun load() {
            screenModel = Loadable.Loading
            screenModel = loadMovie(
                ctx,
                tmdbCache,
                movie,
                width = this.constraints.maxWidth,
                height = this.constraints.maxHeight,
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
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            val scaffoldState = rememberWatchedItemSheetScaffoldState()
            MaterialTheme(colorScheme = model.colorScheme) {
                WatchedItemSheetScaffold(
                    scaffoldState = scaffoldState,
                    watchedItemType = WatchedItemType.MOVIE,
                    approximateMediaRuntime = model.runtime ?: 2.hours,
                    mediaLanguages = listOf(model.originalLanguage),
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        containerColor = Color.Transparent,
                        topBar = {
                            OverviewScreenComponents.Header(model.title, model.backdrop, scrollBehavior)
                        },
                        content = { innerPadding ->
                            MovieScreenContent(
                                innerPadding = innerPadding,
                                totalHeight = constraints.maxHeight,
                                model = model,
                                onEditWatchedItem = { scaffoldState.open(WatchedItemSheetMode.Edit(it)) },
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    scaffoldState.open(WatchedItemSheetMode.New(movie.id.toExternalId().asWatchable()))
                                },
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = R.string.mark_movie_as_watched.str())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieScreenContent(
    innerPadding: PaddingValues,
    model: MovieScreenModel,
    totalHeight: Int,
    onEditWatchedItem: (WatchedItemWrapper) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullProfileData = LocalFullProfileDataContext.current
    LazyColumn(
        contentPadding = innerPadding,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverviewScreenComponents.run {
            if (model.director.isNotEmpty()) {
                val directors = formatAndList(model.director.map { it.name })
                item {
                    Text(R.string.movie_by_director.str(directors))
                }
            }
            tagsComposable(
                tags = listOfNotNull(
                    model.year?.toString(),
                    model.runtime?.toString(),
                    model.rating?.format(),
                ) + model.genres.map { it.name },
            )
            space()
            debugWatchedItemList(fullProfileData, model.id, onEditWatchedItem)
            textSection(model.tagline, model.overview)
            imagesSection(model.images, totalHeight = totalHeight)
            castSection(model.cast, totalHeight = totalHeight)
            crewSection(model.crew, totalHeight = totalHeight)
            // To avoid overlaps with the FAB; see FabPrimaryTokens.ContainerHeight and Scaffold.FabSpacing
            item { Spacer(Modifier.height(56.dp + 16.dp)) }
        }
    }
}

// TODO remove and make better, https://github.com/couch-tracker/couch-tracker-app/issues/62?issue=couch-tracker%7Ccouch-tracker-app%7C96
private fun LazyListScope.debugWatchedItemList(
    data: FullProfileData,
    movie: TmdbMovieId,
    onEditWatchedItem: (WatchedItemWrapper) -> Unit,
) {
    val id = movie.toExternalId().asWatchable()
    val watchedItems = data.watchedItems.filter { it.itemId == id }

    for (item in watchedItems) {
        item {
            Row(Modifier.background(Color.Red)) {
                val localizedDate = item.watchAt?.localized(
                    timezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
                    localSkeletons = {
                        when (it) {
                            is PartialDateTime.Local.Year -> it.localized(YearSkeleton.NUMERIC)
                            is PartialDateTime.Local.YearMonth -> it.localized(YearSkeleton.NUMERIC, MonthSkeleton.WIDE)
                            is PartialDateTime.Local.Date -> it.localized(Skeletons.MEDIUM_DATE)
                            is PartialDateTime.Local.DateTime -> it.localized(Skeletons.MEDIUM_DATE + TimeSkeleton.MINUTES)
                        }
                    },
                )
                Column(Modifier.weight(1f)) {
                    OverviewScreenComponents.Text("Watched at ${localizedDate?.string()}")
                    OverviewScreenComponents.Text(
                        text = item.dimensions
                            .filterIsInstance<WatchedItemDimensionSelection.Choice>()
                            .flatMap { it.value }
                            .map { it.name.text.string() }
                            .joinToString(separator = ", "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    modifier = Modifier.wrapContentWidth(),
                    onClick = { onEditWatchedItem(item) },
                    content = { Text("Edit") },
                )
            }
        }
    }
}
