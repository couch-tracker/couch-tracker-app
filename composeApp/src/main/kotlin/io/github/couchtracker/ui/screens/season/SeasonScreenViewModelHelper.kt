package io.github.couchtracker.ui.screens.season

import android.app.Application
import androidx.compose.runtime.State
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbSeason
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.ui.components.EpisodeListItemModel
import io.github.couchtracker.ui.isWorthDisplayingAltSeasonName
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.ui.seasonNameSubtitle
import io.github.couchtracker.ui.seasonNumberToString
import io.github.couchtracker.utils.FlowToStateCollector
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.api.FlowRetryToken
import io.github.couchtracker.utils.api.callApi
import io.github.couchtracker.utils.collectFlow
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * A utility class to create your own model for a season screen.
 */
class SeasonScreenViewModelHelper(
    val application: Application,
    val scope: CoroutineScope,
    val seasonId: TmdbSeasonId,
    val retryToken: FlowRetryToken = FlowRetryToken(),
    val tmdbSeason: Flow<TmdbSeason> = AppSettings.get { Tmdb.Languages }
        .map { languages -> TmdbSeason(seasonId, languages.current) },
    val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(scope),
) {
    val showViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = scope,
        showId = seasonId.showId,
        flowCollector = flowCollector,
        retryToken = retryToken,
    )

    data class Details(
        val number: Int,
        val name: String,
        val defaultName: String,
        val overview: String?,
        val airDate: LocalDate?,
        val episodes: List<Pair<ExternalEpisodeId, EpisodeListItemModel>>,
    ) {
        val displayDefaultName = isWorthDisplayingAltSeasonName(name, number, defaultName)
    }

    fun details(): State<ApiLoadable<Details>> {
        return flowCollector.collectFlow(
            flow = tmdbSeason.callApi(retryToken) { it.details }.map { result ->
                result.mapResult { tmdbSeasonDetails ->
                    Details(
                        number = tmdbSeasonDetails.seasonNumber,
                        name = tmdbSeasonDetails.name,
                        defaultName = seasonNumberToString(application, tmdbSeasonDetails.seasonNumber),
                        overview = tmdbSeasonDetails.overview,
                        airDate = tmdbSeasonDetails.airDate,
                        episodes = tmdbSeasonDetails.episodes.orEmpty().map { tmdbEpisode ->
                            val id = TmdbExternalEpisodeId(TmdbEpisodeId(seasonId, tmdbEpisode.episodeNumber))
                            id to EpisodeListItemModel.fromTmdbEpisode(application, tmdbEpisode)
                        },
                    )
                }
            },
        )
    }

    fun subtitle(
        details: ApiLoadable<Details>,
        showBaseDetails: ApiLoadable<ShowScreenViewModelHelper.BaseDetails>,
    ): ApiLoadable<String?> {
        return details.mapResult { season ->
            val show = showBaseDetails.resultValueOrNull()
            seasonNameSubtitle(application, season.displayDefaultName, show?.name, season.defaultName)
        }
    }

    fun retryAll() {
        scope.launch { retryToken.retryAll() }
    }
}
