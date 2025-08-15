package io.github.couchtracker.ui.screens.movie

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbMemoryCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.directors
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.prepareMainImageRequest
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.CompletableApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.valueOrNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val LOADING_DELAY = 0.milliseconds

@Stable
class MovieScreenModelState(
    val context: Context,
    val movie: TmdbMovie,
    val width: Int,
    val height: Int,
    val tmdbCache: TmdbCache,
    val scope: CoroutineScope,
    val coroutineContext: CoroutineContext = Dispatchers.Default,
) {

    var baseDetails by mutableStateOf<ApiLoadable<BaseDetails>>(Loadable.Loading)
        private set
    var fullDetails by mutableStateOf<ApiLoadable<FullDetails>>(Loadable.Loading)
        private set
    var images by mutableStateOf<ApiLoadable<List<ImageModel>>>(Loadable.Loading)
        private set
    var credits by mutableStateOf<ApiLoadable<Credits>>(Loadable.Loading)
        private set
    var colorScheme by mutableStateOf<ColorScheme>(ColorSchemes.Movie)
        private set

    data class BaseDetails(
        val title: String,
        val overview: String,
        val year: Int?,
        val backdrop: ImageRequest?,
    )

    data class FullDetails(
        val baseDetails: BaseDetails,
        val tagline: String,
        val rating: TmdbRating?,
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

    private fun <T : Any, R> T?.fromCacheOrCompute(
        fromCache: (T) -> R,
        compute: suspend () -> ApiResult<R>,
    ): DeferredApiResult<R> {
        return if (this != null) {
            CompletableDeferred(Result.Value(fromCache(this)))
        } else {
            scope.async(coroutineContext) {
                compute()
            }
        }
    }

    fun load() {
        val baseDetailsMemory = TmdbMemoryCache.getMovie(movie)
        val detailsApiResult = CompletableApiResult<TmdbMovieDetail>()
        val creditsApiResult = CompletableApiResult<TmdbCredits>()
        val imagesApiResult = CompletableApiResult<TmdbImages>()
        scope.launch(coroutineContext) {
            movie.details(cache = tmdbCache, details = detailsApiResult, credits = creditsApiResult, images = imagesApiResult)
        }
        scope.launch(Dispatchers.Main) {
            delay(LOADING_DELAY)// todo
            images = withContext(coroutineContext) {
                imagesApiResult.await().map { imagesApiResult ->
                    imagesApiResult
                        .linearize()
                        .map { it.toImageModel(TmdbImageType.BACKDROP) }
                }
            }
        }
        scope.launch(Dispatchers.Main) {
            delay(LOADING_DELAY)// todo
            credits = withContext(coroutineContext) {
                creditsApiResult.await().map { creditsApiResult ->
                    val directors = creditsApiResult.crew.directors()
                    Credits(
                        directors = directors,
                        cast = creditsApiResult.cast.toCastPortraitModel(movie.language),
                        crew = creditsApiResult.crew.toCrewCompactListItemModel(context, movie.language),
                        directorsString = if (directors.isEmpty()) {
                            null
                        } else {
                            context.getString(R.string.movie_by_director, formatAndList(directors.map { it.name }))
                        },
                    )
                }
            }
        }
        if (baseDetailsMemory != null) {
            baseDetails = Result.Value(baseDetailsMemory.toBaseDetails())
        }
        scope.launch(Dispatchers.Main) {
            delay(LOADING_DELAY) // todo
            val cs = withContext(coroutineContext) {
                val backdrop = baseDetailsMemory.fromCacheOrCompute(
                    // TODO: usa un if
                    fromCache = { details -> details.backdrop },
                    compute = { detailsApiResult.await().map { it.backdropImage } },
                )
                backdrop.await().map { backdrop ->
                    backdrop?.prepareAndExtractColorScheme(context)?.generateColorScheme()
                }
            }.valueOrNull()
            if (cs != null) {
                colorScheme = cs
            }
        }
        scope.launch(Dispatchers.Main) {
            delay(LOADING_DELAY)// todo
            when (val details = detailsApiResult.await()) {
                is Result.Error -> {
                    fullDetails = details
                }
                is Result.Value -> {
                    val base = withContext(coroutineContext) {
                        details.value.toBaseDetails()
                    }
                    val full = withContext(coroutineContext) {
                        Result.Value(
                            FullDetails(
                                baseDetails = base,
                                tagline = details.value.tagline,
                                rating = details.value.rating(),
                                runtime = details.value.runtime(),
                                originalLanguage = details.value.language(),
                                genres = details.value.genres,
                            ),
                        )
                    }
                    fullDetails = full
                    baseDetails = Result.Value(base)
                }
            }
        }
    }

    private fun TmdbMovieDetail.toBaseDetails(): BaseDetails = BaseDetails(
        title = title,
        overview = overview,
        year = releaseDate?.year,
        backdrop = backdropImage?.prepareMainImageRequest(context, width, height),
    )

    private fun BaseTmdbMovie.toBaseDetails(): BaseDetails = BaseDetails(
        title = title,
        overview = overview,
        year = releaseDate?.year,
        backdrop = backdrop?.prepareMainImageRequest(context, width, height),
    )
}
