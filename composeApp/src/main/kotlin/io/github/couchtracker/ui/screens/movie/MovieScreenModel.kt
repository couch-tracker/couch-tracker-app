package io.github.couchtracker.ui.screens.movie

import android.content.Context
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
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
import io.github.couchtracker.utils.CompletableApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.awaitAll
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.onValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    val allDeferred: Set<DeferredApiResult<*>> = setOf(credits, images)

    data class Credits(
        val director: List<TmdbCrew>,
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )
}

suspend fun loadMovie(
    ctx: Context,
    tmdbCache: TmdbCache,
    movie: TmdbMovie,
    width: Int,
    height: Int,
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<MovieScreenModel> = coroutineScope {
    val details = CompletableApiResult<TmdbMovieDetail>()
    val credits = CompletableApiResult<TmdbCredits>()
    val images = CompletableApiResult<TmdbImages>()
    launch(coroutineContext) {
        movie.details(cache = tmdbCache, details = details, credits = credits, images = images)
    }

    val imagesModel = async(coroutineContext) {
        images.await().map { images ->
            images
                .linearize()
                .map { it.toImageModel(TmdbImageType.BACKDROP) }
        }
    }
    val creditsModel = async(coroutineContext) {
        credits.await().map { credits ->
            MovieScreenModel.Credits(
                director = credits.crew.directors(),
                cast = credits.cast.toCastPortraitModel(movie.language),
                crew = credits.crew.toCrewCompactListItemModel(movie.language),
            )
        }
    }
    details.await().map { details ->
        val backdrop = async(coroutineContext) {
            details.backdropImage.prepareAndExtractColorScheme(
                ctx = ctx,
                width = width,
                height = height,
                fallbackColorScheme = ColorSchemes.Movie,
            )
        }
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
            credits = creditsModel,
            backdrop = backdrop.await().first,
            images = imagesModel,
            colorScheme = backdrop.await().second,
        )
    }.onValue {
        // It can be disruptive to load in content at separate times.
        // If the other content loads "fast enough", I'll wait for it.
        it.allDeferred.awaitAll(100.milliseconds)
    }
}
