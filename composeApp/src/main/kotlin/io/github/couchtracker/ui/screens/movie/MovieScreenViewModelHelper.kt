package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.State
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbApiContext
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.credits
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.directors
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.images
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.tmdbApiContext
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.format
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.FlowToStateCollector
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.api.ApiException
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.collectFlow
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import kotlin.time.Duration

/**
 * A utility class to create your own model for a movie screen.
 */
class MovieScreenViewModelHelper(
    val application: Application,
    val scope: CoroutineScope,
    val movieId: TmdbMovieId,
    val apiContext: TmdbApiContext = tmdbApiContext(),
    val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(scope),
) {

    data class BaseDetails(
        val title: String,
        val overview: String?,
        val year: Int?,
        val backdrop: ImageModel?,
    )

    data class FullDetails(
        val baseDetails: BaseDetails,
        val genres: List<TmdbGenre>,
        val originalLanguage: Bcp47Language?,
        val rating: TmdbRating?,
        val runtime: Duration?,
        val runtimeString: String?,
        val tagline: String,
    )

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
        val directorsString: String?,
    )

    private val baseAndFullDetails: SharedFlow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> =
        apiContext.withCache(
            cacheFn = { KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().getMovie(movieId, it.apiLanguage)?.toBaseDetails() },
        ) { languages ->
            movieId.details(languages.apiLanguage).map { result ->
                result.map { details ->
                    details.toDetails()
                }
            }
        }.shareIn(scope, SharingStarted.Lazily, 1)

    fun baseDetails(): State<ApiLoadable<BaseDetails>> {
        return flowCollector.collectFlow(baseAndFullDetails.map { it.first })
    }

    fun fullDetails(): State<ApiLoadable<FullDetails>> {
        return flowCollector.collectFlow(baseAndFullDetails.map { it.second })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun colorScheme(): State<Loadable<Result<ColorScheme?, ApiException>>> {
        return flowCollector.collectFlow(
            flow = baseAndFullDetails
                .map { (base, _) -> base.mapResult { it.backdrop } }
                .distinctUntilChanged()
                .mapLatest { backdrop ->
                    backdrop.mapResult { backdrop ->
                        backdrop?.extractColorScheme(application)
                    }
                },
        )
    }

    fun images(): State<ApiLoadable<List<ImageModel>>> {
        return flowCollector.collectFlow(
            flow = apiContext { languages ->
                movieId.images(languages.toTmdbLanguagesFilter()).map { result ->
                    result.map { images ->
                        images.toImageModel(includeLogos = false)
                    }
                }
            },
        )
    }

    fun credits(): State<ApiLoadable<Credits>> {
        return flowCollector.collectFlow(
            flow = apiContext { languages ->
                movieId.credits(languages.apiLanguage).map { result ->
                    result.map { credits ->
                        val directors = credits.crew.directors()
                        Credits(
                            cast = credits.cast.toCastPortraitModel(),
                            crew = credits.crew.toCrewCompactListItemModel(application),
                            directorsString = if (directors.isEmpty()) {
                                null
                            } else {
                                application.getString(R.string.movie_by_director, formatAndList(directors.map { it.name }))
                            },
                        )
                    }
                }
            },
        )
    }

    private suspend fun TmdbMovieDetail.toDetails(): Pair<BaseDetails, FullDetails> {
        val base = BaseDetails(
            title = title,
            overview = overview,
            year = releaseDate?.year,
            backdrop = backdropImage?.toImageModelWithPlaceholder(),
        )
        val runtime = runtime()
        val full = FullDetails(
            baseDetails = base,
            tagline = tagline,
            rating = rating(),
            runtime = runtime,
            runtimeString = runtime?.format(),
            originalLanguage = language(),
            genres = genres,
        )
        return base to full
    }

    private fun BaseTmdbMovie.toBaseDetails() = BaseDetails(
        title = title,
        overview = overview,
        year = releaseDate?.year,
        backdrop = backdrop?.toImageModelWithPlaceholder(),
    )

    fun retryAll() {
        scope.launch { apiContext.retryAll() }
    }
}
