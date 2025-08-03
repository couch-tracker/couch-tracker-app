package io.github.couchtracker.ui.screens.movie

import android.content.Context
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbGenre
import coil3.request.ImageRequest
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.directors
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.awaitAll
import io.github.couchtracker.utils.runApiCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val LOG_TAG = "MovieScreenModel"

data class MovieScreenModel(
    val movie: TmdbMovie,
    val title: String,
    val tagline: String,
    val overview: String,
    val year: Int?,
    val runtime: Duration?,
    val originalLanguage: Bcp47Language,
    val rating: TmdbRating?,
    val genres: List<TmdbGenre>,
    val credits: DeferredApiResult<Credits>,
    val images: DeferredApiResult<List<ImageModel>>,
    val backdrop: ImageRequest?,
    val colorScheme: ColorScheme,
) {
    data class Credits(
        val director: List<TmdbCrew>,
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )
}

suspend fun CoroutineScope.loadMovie(
    ctx: Context,
    tmdbCache: TmdbCache,
    movie: TmdbMovie,
    width: Int,
    height: Int,
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<MovieScreenModel> {
    return runApiCatching(LOG_TAG) {
        val images = async(coroutineContext) {
            runApiCatching(LOG_TAG) {
                movie.images(tmdbCache)
                    .linearize()
                    .map { it.toImageModel(TmdbImageType.BACKDROP) }
            }
        }
        val credits = async(coroutineContext) {
            runApiCatching(LOG_TAG) {
                val credits = movie.credits(tmdbCache)
                MovieScreenModel.Credits(
                    director = credits.crew.directors(),
                    cast = credits.cast.toCastPortraitModel(movie.language),
                    crew = credits.crew.toCrewCompactListItemModel(movie.language),
                )
            }
        }
        val details = movie.details(tmdbCache)
        val backdrop = async(coroutineContext) {
            details.backdropImage.prepareAndExtractColorScheme(
                ctx = ctx,
                width = width,
                height = height,
                fallbackColorScheme = ColorSchemes.Movie,
            )
        }

        // It can be disruptive to load in content at separate times.
        // If the other content loads "fast enough", I'll wait for it.
        listOf(images, credits).awaitAll(100.milliseconds)

        MovieScreenModel(
            movie = movie,
            title = details.title,
            tagline = details.tagline,
            overview = details.overview,
            year = details.releaseDate?.year,
            runtime = details.runtime(),
            originalLanguage = details.language(),
            rating = details.rating(),
            genres = details.genres,
            credits = credits,
            backdrop = backdrop.await().first,
            images = images,
            colorScheme = backdrop.await().second,
        )
    }
}
