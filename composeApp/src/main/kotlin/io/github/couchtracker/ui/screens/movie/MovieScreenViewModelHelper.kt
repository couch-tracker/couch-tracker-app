package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.intl.datetime.format
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
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
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import org.koin.mp.KoinPlatform
import kotlin.time.Duration

/**
 * A utility class to create your own model for a movie screen.
 */
class MovieScreenViewModelHelper(
    val application: Application,
    scope: CoroutineScope,
    val movieId: TmdbMovieId,
    val retryContext: TmdbFlowRetryContext,
) {

    data class BaseDetails(
        val title: String?,
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
        val tagline: String?,
    )

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
        val directorsString: String?,
    )

    private val baseAndFullDetails: SharedFlow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> =
        retryContext.withCache(
            cacheFn = { KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().getMovie(movieId, it.apiLanguage)?.toBaseDetails() },
        ) { languages ->
            movieId.details(languages.apiLanguage).map { result ->
                result.map { details ->
                    details.toDetails()
                }
            }
        }.shareIn(scope, SharingStarted.Lazily, 1)

    val baseDetails: Flow<ApiLoadable<BaseDetails>> = baseAndFullDetails.map { it.first }
    val fullDetails: Flow<ApiLoadable<FullDetails>> = baseAndFullDetails.map { it.second }

    @OptIn(ExperimentalCoroutinesApi::class)
    val colorScheme: Flow<ApiLoadable<ColorScheme?>> = baseAndFullDetails
        .map { (base, _) -> base.mapResult { it.backdrop } }
        .distinctUntilChanged()
        .mapLatest { backdrop ->
            backdrop.mapResult { backdrop ->
                backdrop?.extractColorScheme(application)
            }
        }

    val images: Flow<ApiLoadable<List<ImageModel>>> = retryContext { languages ->
        movieId.images(languages.toTmdbLanguagesFilter()).map { result ->
            result.map { images ->
                images.toImageModel(includeLogos = false)
            }
        }
    }

    val credits: Flow<ApiLoadable<Credits>> = retryContext { languages ->
        movieId.credits(languages.apiLanguage).map { result ->
            result.map { credits ->
                val directors = credits.crew.directors()
                Credits(
                    cast = credits.cast.toCastPortraitModel(),
                    crew = credits.crew.toCrewCompactListItemModel(application),
                    directorsString = if (directors.isEmpty()) {
                        null
                    } else {
                        application.getString(
                            R.string.movie_by_director,
                            formatAndList(directors.mapNotNull { it.name }),
                        )
                    },
                )
            }
        }
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
}
