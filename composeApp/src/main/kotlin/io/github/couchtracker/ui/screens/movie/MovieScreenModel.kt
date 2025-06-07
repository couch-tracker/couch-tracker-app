package io.github.couchtracker.ui.screens.movie

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.palette.graphics.Palette
import app.moviebase.tmdb.image.TmdbImageUrlBuilder
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbVideo
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val LOG_TAG = "MovieScreenModel"

data class MovieScreenModel(
    val id: TmdbMovieId,
    val title: String,
    val tagline: String,
    val overview: String,
    val year: Int?,
    val runtime: Duration?,
    val rating: TmdbRating?,
    val genres: List<TmdbGenre>,
    val director: List<TmdbCrew>,
    val cast: List<CastPortraitModel>,
    val crew: List<CrewCompactListItemModel>,
    val images: List<TmdbFileImage>,
    val videos: List<TmdbVideo>,
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
): Loadable<MovieScreenModel, TmdbException> = coroutineScope {
    withContext(coroutineContext) {
        try {
            val credits = async { movie.credits(tmdbCache) }
            val images = async { movie.images(tmdbCache) }
            val videos = async { movie.videos(tmdbCache) }
            val details = movie.details(tmdbCache)
            val backdropImage = details.backdropImage
            val backdropImageRequest: ImageRequest?
            var colorScheme: ColorScheme = ColorSchemes.Movie
            if (backdropImage != null) {
                val url = TmdbImageUrlBuilder.build(backdropImage, width, height)
                backdropImageRequest = ImageRequest.Builder(ctx)
                    // Necessary for palette generation
                    .allowHardware(false)
                    .data(url)
                    .size(width, height)
                    .build()
                val image = ctx.imageLoader.execute(backdropImageRequest).image
                if (image != null) {
                    colorScheme = async {
                        val bitmap = image.toBitmap()
                        bitmap.prepareToDraw()
                        val palette = Palette.Builder(bitmap).generate()
                        palette.generateColorScheme()
                    }.await()
                }
            } else {
                backdropImageRequest = null
            }
            Result.Value(
                MovieScreenModel(
                    id = movie.id,
                    title = details.title,
                    tagline = details.tagline,
                    overview = details.overview,
                    year = details.releaseDate?.year,
                    runtime = details.runtime?.minutes,
                    rating = details.rating(),
                    genres = details.genres,
                    director = credits.await().crew.filter { it.job == "Director" },
                    cast = credits.await().cast.toCastPortraitModel(movie.language, ImagePreloadOptions.DoNotPreload),
                    crew = credits.await().crew.toCrewCompactListItemModel(movie.language, ImagePreloadOptions.DoNotPreload),
                    backdrop = backdropImageRequest,
                    images = images.await().let { images ->
                        (images.backdrops + images.posters).sortedByDescending { it.voteAverage }
                    },
                    videos = videos.await(),
                    colorScheme = colorScheme,
                ),
            )
        } catch (e: TmdbException) {
            Log.e(LOG_TAG, "Error while loading MovieScreenModel for ${movie.id.toExternalId().serialize()} (${movie.language})", e)
            Result.Error(e)
        }
    }
}
