package io.github.couchtracker.ui.screens.movie

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbGenre
import coil.imageLoader
import coil.request.ImageRequest
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.ui.screens.main.MOVIE_COLOR_SCHEME
import io.github.couchtracker.utils.Loadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class MovieScreenModel(
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

suspend fun loadMovie(
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
