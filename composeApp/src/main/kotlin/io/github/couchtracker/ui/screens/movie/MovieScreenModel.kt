package io.github.couchtracker.ui.screens.movie

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbCrew
import app.moviebase.tmdb.model.TmdbGenre
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.directors
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toBaseMovie
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.ifError
import io.github.couchtracker.utils.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
    val backdrop: ImageModel?,
    val colorScheme: Deferred<ColorScheme?>,
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
    tmdbBaseMemoryCache: TmdbBaseMemoryCache = KoinPlatform.getKoin().get(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<MovieScreenModel> {
    val baseDetailsMemory = tmdbBaseMemoryCache.getMovie(movie)

    val imagesModel = async(coroutineContext) {
        movie.images.first().map { images ->
            images
                .linearize()
                .map { it.toImageModel(TmdbImageType.BACKDROP) }
        }
    }
    val creditsModel = async(coroutineContext) {
        movie.credits.first().map { credits ->
            val directors = credits.crew.directors()
            MovieScreenModel.Credits(
                directors = directors,
                cast = credits.cast.toCastPortraitModel(),
                crew = credits.crew.toCrewCompactListItemModel(ctx),
                directorsString = if (directors.isEmpty()) {
                    null
                } else {
                    ctx.getString(R.string.movie_by_director, formatAndList(directors.map { it.name }))
                },
            )
        }
    }
    val fullDetails = async(coroutineContext) { movie.details.first() }
    val fullDetailsModel = async(coroutineContext) {
        fullDetails.await().map { details ->
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
        fullDetails.await().map { details ->
            details.toBaseMovie(movie.languages.apiLanguage)
        }.ifError { return Result.Error(it) }
    }
    val backdrop = async(coroutineContext) {
        baseDetails.backdrop?.toImageModelWithPlaceholder()
    }
    val ret = MovieScreenModel(
        movie = movie,
        title = baseDetails.title,
        overview = baseDetails.overview,
        year = baseDetails.releaseDate?.year,
        rating = baseDetails.rating(),
        fullDetails = fullDetailsModel,
        credits = creditsModel,
        backdrop = backdrop.await(),
        images = imagesModel,
        colorScheme = async { backdrop.await()?.extractColorScheme(ctx) },
    )
    return Result.Value(ret)
}
