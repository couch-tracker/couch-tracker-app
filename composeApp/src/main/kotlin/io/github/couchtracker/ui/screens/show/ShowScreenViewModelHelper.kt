package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.db.profile.externalids.TmdbExternalSeasonId
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.aggregateCredits
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.images
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.SeasonListItemModel
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

/**
 * A utility class to create your own model for a show screen.
 */
class ShowScreenViewModelHelper(
    val application: Application,
    scope: CoroutineScope,
    val showId: TmdbShowId,
    val retryContext: TmdbFlowRetryContext,
) {

    data class BaseDetails(
        val name: String?,
        val overview: String?,
        val year: Int?,
        val backdrop: ImageModel?,
    )

    data class FullDetails(
        val baseDetails: BaseDetails,
        val createdBy: List<TmdbShowCreatedBy>,
        val createdByString: String?,
        val genres: List<TmdbGenre>,
        val originalLanguage: Bcp47Language?,
        val rating: TmdbRating?,
        val tagline: String?,
        val seasons: List<Pair<ExternalSeasonId, SeasonListItemModel>>,
    )

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )

    private val baseAndFullDetails: SharedFlow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> =
        retryContext.withCache(
            cacheFn = { KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().getShow(showId, it.apiLanguage)?.toBaseDetails() },
        ) { languages ->
            showId.details(languages.apiLanguage).map {
                it.map { details ->
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
        showId.images(languages.toTmdbLanguagesFilter()).map {
            it.map { images ->
                images.toImageModel(includeLogos = false)
            }
        }
    }

    val credits: Flow<ApiLoadable<Credits>> = retryContext { languages ->
        showId.aggregateCredits(languages.apiLanguage).map {
            it.map { credits ->
                Credits(
                    cast = credits.cast.toCastPortraitModel(application),
                    crew = credits.crew.toCrewCompactListItemModel(application),
                )
            }
        }
    }

    private suspend fun TmdbShowDetail.toDetails(): Pair<BaseDetails, FullDetails> {
        val base = BaseDetails(
            name = name,
            overview = overview,
            year = firstAirDate?.year,
            backdrop = backdropImage?.toImageModelWithPlaceholder(),
        )
        val createdBy = createdBy.orEmpty()
        val full = FullDetails(
            baseDetails = base,
            tagline = tagline,
            rating = rating(),
            originalLanguage = language(),
            genres = genres,
            createdBy = createdBy,
            createdByString = if (createdBy.isEmpty()) {
                null
            } else {
                application.getString(R.string.show_by_creator, formatAndList(createdBy.mapNotNull { it.name }))
            },
            seasons = seasons.map { season ->
                val id = TmdbExternalSeasonId(TmdbSeasonId(showId, season.seasonNumber))
                id to SeasonListItemModel.fromTmdbSeason(application, season)
            },
        )
        return base to full
    }

    private fun BaseTmdbShow.toBaseDetails(): BaseDetails = BaseDetails(
        name = name,
        overview = overview,
        year = firstAirDate?.year,
        backdrop = backdrop?.toImageModelWithPlaceholder(),
    )
}
