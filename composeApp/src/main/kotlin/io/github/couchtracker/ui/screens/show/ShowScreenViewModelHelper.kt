package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.State
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.season.ExternalSeasonId
import io.github.couchtracker.db.profile.season.TmdbExternalSeasonId
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.extractColorScheme
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
import io.github.couchtracker.utils.FlowToStateCollector
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.api.ApiException
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.api.FlowRetryToken
import io.github.couchtracker.utils.api.callApi
import io.github.couchtracker.utils.api.callApiWithCache
import io.github.couchtracker.utils.collectFlow
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * A utility class to create your own model for a show screen.
 */
class ShowScreenViewModelHelper(
    val application: Application,
    val scope: CoroutineScope,
    val showId: TmdbShowId,
    val retryToken: FlowRetryToken = FlowRetryToken(),
    val tmdbShow: Flow<TmdbShow> = AppSettings.get { Tmdb.Languages }
        .map { languages -> TmdbShow(showId, languages.current) },
    val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(scope),
) {

    data class BaseDetails(
        val name: String,
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
        val tagline: String,
        val seasons: List<Pair<ExternalSeasonId, SeasonListItemModel>>,
    )

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )

    private val baseAndFullDetails: SharedFlow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> = tmdbShow.callApiWithCache(
        retryToken = retryToken,
        cachedData = { KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().getShow(it)?.toBaseDetails() },
        fullDataFlow = {
            it.details.map { result -> result.map { details -> details.toDetails() } }
        },
    ).shareIn(scope, SharingStarted.Lazily, 1)

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
            flow = tmdbShow.callApi(retryToken) { it.images }.map { result ->
                result.mapResult { images ->
                    images.toImageModel(includeLogos = false)
                }
            },
        )
    }

    fun credits(): State<ApiLoadable<Credits>> {
        return flowCollector.collectFlow(
            flow = tmdbShow.callApi(retryToken) { it.aggregateCredits }.map { result ->
                result.mapResult { credits ->
                    Credits(
                        cast = credits.cast.toCastPortraitModel(application),
                        crew = credits.crew.toCrewCompactListItemModel(application),
                    )
                }
            },
        )
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
                application.getString(R.string.show_by_creator, formatAndList(createdBy.map { it.name }))
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

    fun retryAll() {
        scope.launch { retryToken.retryAll() }
    }
}
