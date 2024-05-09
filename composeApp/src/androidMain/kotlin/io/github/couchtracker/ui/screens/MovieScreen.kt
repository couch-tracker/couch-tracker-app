@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.couchtracker.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
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
import io.github.couchtracker.ui.backgroundColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

sealed interface MoviesScreenState {
    data object Loading : MoviesScreenState
    data class Error(val message: String) : MoviesScreenState
    data class Loaded(
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
        val palette: Palette?,
    ) : MoviesScreenState
}

@Composable
fun MovieScreen(movie: TmdbMovie) {
    val ctx = LocalContext.current
    val tmdbCache = koinInject<TmdbCache>()
    var screenState by remember { mutableStateOf<MoviesScreenState>(MoviesScreenState.Loading) }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        LaunchedEffect(movie) {
            screenState = loadMovie(
                ctx,
                tmdbCache,
                movie,
                width = constraints.maxWidth,
                height = constraints.maxHeight,
            )
        }

        when (val state = screenState) {
            is MoviesScreenState.Error -> Text("Error: ${state.message}")
            MoviesScreenState.Loading -> CircularProgressIndicator()

            is MoviesScreenState.Loaded -> {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
                val backgroundColor = state.palette.backgroundColor(MaterialTheme.colorScheme)

                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        MovieAppBar(state, scrollBehavior, backgroundColor)
                    },
                    content = { innerPadding ->
                        MovieScreenContent(Modifier.background(backgroundColor), innerPadding, state)
                    },
                )
            }
        }
    }
}

@Composable
private fun MovieAppBar(
    state: MoviesScreenState.Loaded,
    scrollBehavior: TopAppBarScrollBehavior,
    backgroundColor: Color,
) {
    val navController = LocalNavController.current
    Box {
        AsyncImage(
            modifier = Modifier.matchParentSize().blur(scrollBehavior.state.collapsedFraction * 8.dp),
            model = state.backdrop,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        LargeTopAppBar(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        0f to backgroundColor.copy(alpha = 0.5f),
                        1f to backgroundColor,
                    ),
                ),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            title = {
                Text(
                    state.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton({ navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        // TODO: translate
                        contentDescription = "Back",
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}

private const val BACKDROP_FILL_PERCENTAGE = 0.8
private const val BACKDROP_ASPECT_RATIO = 16f / 9

@Composable
private fun MovieScreenContent(
    modifier: Modifier,
    innerPadding: PaddingValues,
    state: MoviesScreenState.Loaded,
) {
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.director.isNotEmpty()) {
                // TODO: translate
                text("by " + state.director.joinToString { it.name }) { MaterialTheme.typography.titleMedium }
            }
            tagsComposable(
                listOfNotNull(
                    state.year?.toString(),
                    state.runtime?.toString(),
                    state.rating?.format(),
                ) + state.genres.map { it.name },
            )
            space()
            text(state.tagline, maxLines = 1) { MaterialTheme.typography.titleMedium }
            text(state.overview) { MaterialTheme.typography.bodyMedium }
            space()
            if (state.images.isNotEmpty()) {
                // TODO: translate
                text("Images", maxLines = 1) { MaterialTheme.typography.titleMedium }
                item {
                    val scale = LocalDensity.current.run { 1f / 1.dp.toPx() }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        val targetWidth = (width * BACKDROP_FILL_PERCENTAGE).toInt()
                        val targetHeight = (targetWidth / BACKDROP_ASPECT_RATIO).toInt()
                        items(state.images) { image ->
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
        }
    }
}

private fun LazyListScope.space() = item {
    // Spacing is 8.dp; to achieve a spacing of 28.dp, this needs to be 4.dp
    Spacer(Modifier.padding(4.dp))
}

private fun LazyListScope.text(text: String?, maxLines: Int = Int.MAX_VALUE, style: @Composable () -> TextStyle) {
    if (!text.isNullOrBlank()) {
        item {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = maxLines,
                style = style(),
                overflow = TextOverflow.Ellipsis,
            )
        }
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
): MoviesScreenState = coroutineScope {
    withContext(coroutineContext) {
        try {
            val credits = async { movie.credits(tmdbCache) }
            val images = async { movie.images(tmdbCache) }
            val details = movie.details(tmdbCache)
            val backdropImage = details.backdropImage
            val backdropImageRequest: ImageRequest?
            var palette: Palette? = null
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
                    palette = Palette.Builder(drawable.toBitmap()).generate()
                }
            } else {
                backdropImageRequest = null
            }
            MoviesScreenState.Loaded(
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
                palette = palette,
            )
        } catch (e: TmdbException) {
            MoviesScreenState.Error(e.message ?: "Error")
        }
    }
}
