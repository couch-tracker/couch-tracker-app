package io.github.couchtracker.ui.screens.movie

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbCredits
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbMovieDetail
import coil3.request.ImageRequest
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbMemoryCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.directors
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toBaseMovie
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
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.ifError
import io.github.couchtracker.utils.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

data class MovieScreenModel(
    val movie: TmdbMovie,
    val title: String,
    val overview: String,
    val year: Int?,
    val rating: TmdbRating?,
    val fullDetails: DeferredApiResult<FullDetails>,
    val credits: DeferredApiResult<Credits>,
    val images: DeferredApiResult<List<ImageModel>>,
    val backdrop: ImageRequest?,
    val colorScheme: ColorScheme,
) {
    val allDeferred: Set<DeferredApiResult<*>> = setOf(credits, images)

    data class FullDetails(
        val tagline: String,
        val runtime: Duration?,
        val originalLanguage: Bcp47Language,
        val genres: List<TmdbGenre>,
    )

    data class Credits(
        val directors: List<TmdbCrew>,
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
        val directorsString: String?,
    )
}

suspend fun CoroutineScope.loadMovie(
    ctx: Context,
    movie: TmdbMovie,
    width: Int,
    height: Int,
    tmdbCache: TmdbCache = KoinPlatform.getKoin().get(),
    tmdbMemoryCache: TmdbMemoryCache = KoinPlatform.getKoin().get(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<MovieScreenModel> {
    val baseDetailsMemory = tmdbMemoryCache.getMovie(movie)
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
            val directors = credits.crew.directors()
            MovieScreenModel.Credits(
                directors = directors,
                cast = credits.cast.toCastPortraitModel(movie.language),
                crew = credits.crew.toCrewCompactListItemModel(ctx, movie.language),
                directorsString = if (directors.isEmpty()) {
                    null
                } else {
                    ctx.getString(R.string.movie_by_director, formatAndList(directors.map { it.name }))
                },
            )
        }
    }
    val fullDetailsModel = async(coroutineContext) {
        details.await().map { details ->
            MovieScreenModel.FullDetails(
                tagline = details.tagline,
                runtime = details.runtime(),
                originalLanguage = details.language(),
                genres = details.genres,
            )
        }
    }
    val baseDetails = if (baseDetailsMemory != null) {
        baseDetailsMemory
    } else {
        Log.w("Cache miss", "Movie $movie not found in cache")
        details.await().map { details ->
            details.toBaseMovie(movie.language)
        }.ifError { return Result.Error(it) }
    }
    val backdrop = async(coroutineContext) {
        baseDetails.backdrop.prepareAndExtractColorScheme(
            ctx = ctx,
            width = width,
            height = height,
            fallbackColorScheme = ColorSchemes.Movie,
        )
    }
    val ret = MovieScreenModel(
        movie = movie,
        title = baseDetails.title,
        overview = baseDetails.overview,
        year = baseDetails.releaseDate?.year,
        rating = baseDetails.rating(),
        fullDetails = fullDetailsModel,
        credits = creditsModel,
        backdrop = backdrop.await().first,
        images = imagesModel,
        colorScheme = backdrop.await().second,
    )
    return Result.Value(ret)
}
