@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens.movie

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import coil3.compose.AsyncImage
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
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
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.BackgroundTopAppBar
import io.github.couchtracker.ui.components.CastPortrait
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItem
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.TagsRow
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetScaffold
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlin.math.roundToInt
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
    var screenModel by remember { mutableStateOf<Loadable<MovieScreenModel, TmdbException>>(Loadable.Loading) }

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
                        // TODO: translate
                        message = exception.message ?: "Error",
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
                    approximateVideoRuntime = model.runtime ?: 2.hours,
                ) {
                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        containerColor = Color.Transparent,
                        topBar = {
                            MovieAppBar(model, scrollBehavior)
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
private fun MovieAppBar(
    model: MovieScreenModel,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navController = LocalNavController.current
    BackgroundTopAppBar(
        scrollBehavior = scrollBehavior,
        image = { modifier ->
            AsyncImage(
                modifier = modifier,
                model = model.backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        },
        appBar = { colors ->
            LargeTopAppBar(
                colors = colors,
                title = {
                    val isExpanded = LocalTextStyle.current == MaterialTheme.typography.headlineMedium
                    Text(
                        model.title,
                        maxLines = if (isExpanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton({ navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = R.string.back_action.str(),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    )
}

private const val WIDE_COMPONENTS_FILL_PERCENTAGE = 0.75f
private const val WIDE_COMPONENTS_ASPECT_RATIO = 16f / 9
private const val COLUMN_COMPONENTS_ASPECT_RATIO = 3f / 2
private const val ITEMS_PER_COLUMN = 4

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
        if (model.director.isNotEmpty()) {
            val directors = formatAndList(model.director.map { it.name })
            item {
                MovieText(R.string.movie_by_director.str(directors))
            }
        }
        tagsComposable(
            listOfNotNull(
                model.year?.toString(),
                model.runtime?.toString(),
                model.rating?.format(),
            ) + model.genres.map { it.name },
        )
        space()

        debugWatchedItemList(fullProfileData, model.id, onEditWatchedItem)

        item { MovieText(model.tagline, maxLines = 1) }
        item { MovieText(model.overview, style = MaterialTheme.typography.bodyMedium) }
        space()

        if (model.images.isNotEmpty()) {
            item { MovieText(R.string.section_images.str(), maxLines = 1) }
            item { ImagesSection(model, totalHeight = totalHeight) }
            space()
        }

        if (model.cast.isNotEmpty()) {
            item { MovieText(R.string.section_cast.str(), maxLines = 1) }
            item { CastSection(model.cast, totalHeight = totalHeight) }
            space()
        }

        if (model.crew.isNotEmpty()) {
            item { MovieText(R.string.section_crew.str(), maxLines = 1) }
            item { CrewSection(model.crew, totalHeight = totalHeight) }
            space()
        }

        // To avoid overlaps with the FAB; see FabPrimaryTokens.ContainerHeight and Scaffold.FabSpacing
        item { Spacer(Modifier.height(56.dp + 16.dp)) }
    }
}

@Composable
private fun <T> HorizontalListSection(
    totalHeight: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    itemComposable: @Composable (T, targetHeightPx: Int) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = this.constraints.maxWidth
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            val targetHeightPx = (width * WIDE_COMPONENTS_FILL_PERCENTAGE / WIDE_COMPONENTS_ASPECT_RATIO)
                .coerceAtMost(totalHeight / 2f)
                .roundToInt()
            items(items) { item ->
                itemComposable(item, targetHeightPx)
            }
        }
    }
}

@Composable
private fun <T> HorizontalColumnsListSection(
    totalHeight: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    itemsPerColumn: Int = ITEMS_PER_COLUMN,
    itemComposable: @Composable (T) -> Unit,
) {
    HorizontalListSection(
        totalHeight = totalHeight,
        items = items.chunked(itemsPerColumn),
        modifier = modifier,
    ) { itemsInColumn, targetHeight ->
        val targetWidth = with(LocalDensity.current) {
            targetHeight.toDp() * COLUMN_COMPONENTS_ASPECT_RATIO
        }
        Column(Modifier.width(targetWidth), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (item in itemsInColumn) {
                itemComposable(item)
            }
        }
    }
}

@Composable
private fun ImagesSection(model: MovieScreenModel, totalHeight: Int) {
    HorizontalListSection(totalHeight, model.images) { image, targetHeight ->
        val imgWidth = (targetHeight * image.aspectRation).roundToInt()
        val url = TmdbImageUrlBuilder.build(
            image.filePath,
            TmdbImageType.BACKDROP,
            imgWidth,
            targetHeight,
        )
        Surface(shape = MaterialTheme.shapes.small, shadowElevation = 8.dp, tonalElevation = 8.dp) {
            AsyncImage(
                url,
                modifier = Modifier
                    .width(with(LocalDensity.current) { imgWidth.toDp() })
                    .height(with(LocalDensity.current) { targetHeight.toDp() }),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CastSection(people: List<CastPortraitModel>, totalHeight: Int) {
    HorizontalListSection(totalHeight, people, modifier = Modifier.animateContentSize()) { person, targetHeight ->
        val w = with(LocalDensity.current) {
            targetHeight.toDp() * PortraitComposableDefaults.POSTER_ASPECT_RATIO
        }
        CastPortrait(Modifier.width(w), person, onClick = null)
    }
}

@Composable
private fun CrewSection(people: List<CrewCompactListItemModel>, totalHeight: Int) {
    HorizontalColumnsListSection(
        totalHeight = totalHeight,
        items = people,
    ) { person ->
        CrewCompactListItem(person)
    }
}

private fun LazyListScope.space() = item {
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MovieText(text: String?, maxLines: Int = Int.MAX_VALUE, style: TextStyle = MaterialTheme.typography.titleMedium) {
    if (!text.isNullOrBlank()) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = maxLines,
            style = style,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LazyListScope.tagsComposable(tags: List<String>) {
    if (tags.isNotEmpty()) {
        item {
            TagsRow(tags, modifier = Modifier.padding(horizontal = 16.dp))
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
                    MovieText("Watched at ${localizedDate?.string()}")
                    MovieText(
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
