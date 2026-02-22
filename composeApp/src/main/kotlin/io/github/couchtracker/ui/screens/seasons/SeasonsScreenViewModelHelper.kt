package io.github.couchtracker.ui.screens.seasons

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalSeasonId
import io.github.couchtracker.tmdb.TmdbApiContext
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.tmdbApiContext
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.EpisodeListItemModel
import io.github.couchtracker.ui.isWorthDisplayingAltSeasonName
import io.github.couchtracker.ui.seasonNumberToString
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.collections.map

/**
 * A utility class to create your own model for a season screen.
 */
class SeasonsScreenViewModelHelper(
    val application: Application,
    val scope: CoroutineScope,
    val showId: TmdbShowId,
    val apiContext: TmdbApiContext = tmdbApiContext(),
    val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(scope),
) {
    class ShowDetails(
        val name: String,
        val backdrop: ImageModel?,
        val seasons: List<SeasonBaseDetails>,
    )

    data class SeasonBaseDetails(
        val tmdbSeasonId: TmdbSeasonId,
        val externalId: ExternalSeasonId,
        val number: Int,
        val name: String,
        val defaultName: String,
        val overview: String?,
        val airDate: LocalDate?,
    ) {
        val displayDefaultName = isWorthDisplayingAltSeasonName(name, number, defaultName)
    }

    data class SeasonFullDetails(
        val episodes: List<Pair<ExternalEpisodeId, EpisodeListItemModel>>,
    )

    val showDetailsFlow = apiContext { languages ->
        showId.details(languages.apiLanguage).map { result ->
            result.map { details ->
                ShowDetails(
                    name = details.name,
                    backdrop = details.backdropImage?.toImageModelWithPlaceholder(),
                    seasons = details.seasons.map { season ->
                        val tmdbSeasonId = TmdbSeasonId(showId, season.seasonNumber)
                        SeasonBaseDetails(
                            tmdbSeasonId = tmdbSeasonId,
                            externalId = TmdbExternalSeasonId(tmdbSeasonId),
                            number = season.seasonNumber,
                            name = season.name,
                            defaultName = seasonNumberToString(application, season.seasonNumber),
                            overview = season.overview,
                            airDate = season.airDate,
                        )
                    },
                )
            }
        }
    }.shareIn(scope, SharingStarted.Lazily, 1)

    fun showDetails(): State<ApiLoadable<ShowDetails>> {
        return flowCollector.collectFlow(
            flow = showDetailsFlow,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun colorScheme(): State<Loadable<Result<ColorScheme?, ApiException>>> {
        return flowCollector.collectFlow(
            flow = showDetailsFlow
                .map { result -> result.mapResult { details -> details.backdrop } }
                .distinctUntilChanged()
                .mapLatest { backdrop ->
                    backdrop.mapResult { backdrop ->
                        backdrop?.extractColorScheme(application)
                    }
                },
        )
    }

    class SeasonViewModelHelper(
        val application: Application,
        val scope: CoroutineScope,
        val seasonId: TmdbSeasonId,
        val apiContext: TmdbApiContext = tmdbApiContext(),
        val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(scope),
    ) {

        fun details(): State<ApiLoadable<SeasonFullDetails>> {
            return flowCollector.collectFlow(
                flow = apiContext { languages ->
                    seasonId.details(languages.apiLanguage).map { result ->
                        result.map { tmdbSeasonDetails ->
                            SeasonFullDetails(
                                episodes = tmdbSeasonDetails.episodes.orEmpty().map { tmdbEpisode ->
                                    val id = TmdbExternalEpisodeId(TmdbEpisodeId(seasonId, tmdbEpisode.episodeNumber))
                                    id to EpisodeListItemModel.fromTmdbEpisode(application, tmdbEpisode)
                                },
                            )
                        }
                    }
                },
            )
        }
    }

    @Composable
    fun viewModelForSeason(season: TmdbSeasonId): SeasonViewModelHelper {
        val scope = rememberCoroutineScope()
        val flowCollector = flowCollector.childCollector()
        return SeasonViewModelHelper(
            application = application,
            scope = scope,
            seasonId = season,
            apiContext = apiContext,
            flowCollector = flowCollector,
        )
    }

    fun retryAll() {
        scope.launch { apiContext.retryAll() }
    }
}
