@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.palette.graphics.Palette
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbGenre
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.back_action
import couch_tracker_app.composeapp.generated.resources.movie_by_director
import couch_tracker_app.composeapp.generated.resources.section_images
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.user.movie.ExternalMovieId
import io.github.couchtracker.db.user.movie.TmdbExternalMovieId
import io.github.couchtracker.db.user.movie.UnknownExternalMovieId
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.ui.components.BackgroundTopAppBar
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.ui.screens.main.MOVIE_COLOR_SCHEME
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.formatAndList
import io.github.couchtracker.utils.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val ARGUMENTS_MOVIE_ID = "movieId"
private const val ARGUMENTS_LANGUAGE = "language"

fun NavController.navigateToMovie(movie: TmdbMovie) {
    navigate("movie/${movie.id.toExternalId().serialize()}?language=${movie.language}")
}

fun NavGraphBuilder.movieScreen() {
    composable(
        "movie/{$ARGUMENTS_MOVIE_ID}?language={$ARGUMENTS_LANGUAGE}",
        arguments = listOf(
            navArgument(ARGUMENTS_MOVIE_ID) {
                type = NavType.StringType
            },
            navArgument(ARGUMENTS_LANGUAGE) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        // TODO: handle parse errors
        val args = requireNotNull(backStackEntry.arguments)
        val movieIdStr = requireNotNull(args.getString(ARGUMENTS_MOVIE_ID))
        when (val movieId = ExternalMovieId.parse(movieIdStr)) {
            is TmdbExternalMovieId -> {
                val languageStr = requireNotNull(args.getString(ARGUMENTS_LANGUAGE))
                val language = TmdbLanguage.parse(languageStr)
                MovieScreen(TmdbMovie(movieId.id, language))
            }

            is UnknownExternalMovieId -> TODO()
        }
    }
}

private data class MovieScreenModel(
    val title: String,
    val tagline: String,
    val overview: String,
    val year: Int?,
    val runtime: Duration?,
    val rating: TmdbRating?,
    val genres: List<TmdbGenre>,
    val director: List<TmdbCrew>,
    val images: List<TmdbFileImage>,
    val backdrop: ImageRequest?,
    val colorScheme: ColorScheme,
)

@Composable
fun MovieScreen(movie: TmdbMovie) {
    val cs = rememberCoroutineScope()
    val ctx = LocalContext.current
    val tmdbCache = koinInject<TmdbCache>()
    var screenModel by remember { mutableStateOf<Loadable<MovieScreenModel>>(Loadable.Loading) }
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
                width = constraints.maxWidth,
                height = constraints.maxHeight,
            )
        }
        LaunchedEffect(movie) {
            load()
        }

        LoadableScreen(
            data = screenModel,
            onError = { message ->
                DefaultErrorScreen(
                    errorMessage = message,
                    retry = {
                        cs.launch { load() }
                    },
                )
            },
        ) { model ->
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            MaterialTheme(colorScheme = model.colorScheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        MovieAppBar(model, scrollBehavior)
                    },
                    content = { innerPadding ->
                        MovieScreenContent(Modifier, innerPadding, model)
                    },
                )
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
                    Text(
                        model.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Res.string.back_action.str(),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    )
}

private const val BACKDROP_FILL_PERCENTAGE = 0.8
private const val BACKDROP_ASPECT_RATIO = 16f / 9

@Composable
private fun MovieScreenContent(
    modifier: Modifier,
    innerPadding: PaddingValues,
    model: MovieScreenModel,
) {
    LazyColumn(
        contentPadding = innerPadding,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (model.director.isNotEmpty()) {
            val directors = formatAndList(model.director.map { it.name })
            item {
                MovieText(Res.string.movie_by_director.str(directors), style = MaterialTheme.typography.titleMedium)
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

        item { MovieText(model.tagline, maxLines = 1, style = MaterialTheme.typography.titleMedium) }
        item { MovieText(model.overview, style = MaterialTheme.typography.bodyMedium) }
        space()
        if (model.images.isNotEmpty()) {
            item { MovieText(Res.string.section_images.str(), maxLines = 1, style = MaterialTheme.typography.titleMedium) }
            item {
                ImagesSection(model)
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                Button({}) { Text("Test button") }
                OutlinedButton({}) { Text("Test outlined button") }
                Slider(1 / 2f, {})
                TextField("Test test field", {})
                CircularProgressIndicator()
                LinearProgressIndicator()
                FloatingActionButton({}) { Icon(Icons.Filled.Favorite, null) }
                Spacer(Modifier.height(512.dp))
            }
        }
    }
}

@Composable
private fun ImagesSection(model: MovieScreenModel) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth
        val scale = LocalDensity.current.run { 1f / 1.dp.toPx() }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            val targetWidth = (width * BACKDROP_FILL_PERCENTAGE).toInt()
            val targetHeight = (targetWidth / BACKDROP_ASPECT_RATIO).toInt()
            items(model.images) { image ->
                val imgWidth = (targetHeight * image.aspectRation).toInt()
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
                            .width((imgWidth * scale).dp)
                            .height((targetHeight * scale).dp),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.space() = item {
    // Spacing is 8.dp; to achieve a spacing of 28.dp, this needs to be 4.dp
    Spacer(Modifier.padding(4.dp))
}

@Composable
private fun MovieText(text: String?, maxLines: Int = Int.MAX_VALUE, style: TextStyle) {
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
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (tag in tags) {
                    Text(tag, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private suspend fun loadMovie(
    ctx: Context,
    tmdbCache: TmdbCache,
    movie: TmdbMovie,
    width: Int,
    height: Int,
    coroutineContext: CoroutineContext = Dispatchers.Default,
): Loadable<MovieScreenModel> = coroutineScope {
    withContext(coroutineContext) {
        try {
            val credits = async { movie.credits(tmdbCache) }
            val images = async { movie.images(tmdbCache) }
            val details = movie.details(tmdbCache)
            val backdropImage = details.backdropImage
            val backdropImageRequest: ImageRequest?
            var colorScheme: ColorScheme = MOVIE_COLOR_SCHEME
            if (backdropImage != null) {
                val url = TmdbImageUrlBuilder.build(backdropImage, width, height)
                backdropImageRequest = ImageRequest.Builder(ctx)
                    // Necessary for palette generation
                    .allowHardware(false)
                    .data(url)
                    .size(width, height)
                    .build()
                val drawable = ctx.imageLoader.execute(backdropImageRequest).drawable
                if (drawable != null) {
                    colorScheme = async {
                        val bitmap = drawable.toBitmap()
                        bitmap.prepareToDraw()
                        val palette = Palette.Builder(bitmap).generate()
                        palette.generateColorScheme()
                    }.await()
                }
            } else {
                backdropImageRequest = null
            }
            Loadable.Loaded(
                MovieScreenModel(
                    title = details.title,
                    tagline = details.tagline,
                    overview = details.overview,
                    year = details.releaseDate?.year,
                    runtime = details.runtime?.minutes,
                    rating = details.rating(),
                    genres = details.genres,
                    director = credits.await().crew.filter { it.job == "Director" },
                    backdrop = backdropImageRequest,
                    images = images.await().let { imgs ->
                        (imgs.backdrops + imgs.posters).sortedByDescending { it.voteAverage }
                    },
                    colorScheme = colorScheme,
                ),
            )
        } catch (e: TmdbException) {
            // TODO: translate
            Loadable.Error(e.message ?: "Error")
        }
    }
}
