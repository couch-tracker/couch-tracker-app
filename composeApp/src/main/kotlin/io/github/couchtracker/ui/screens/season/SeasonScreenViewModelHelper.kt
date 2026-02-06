package io.github.couchtracker.ui.screens.season

import android.app.Application
import androidx.compose.runtime.State
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.TmdbSeason
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.utils.FlowToStateCollector
import io.github.couchtracker.utils.api.ApiCallHelper
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.collectFlow
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * A utility class to create your own model for a season screen.
 */
class SeasonScreenViewModelHelper(
    val application: Application,
    scope: CoroutineScope,
    seasonId: TmdbSeasonId,
    val apiCallHelper: ApiCallHelper<TmdbSeason> = ApiCallHelper(
        scope = scope,
        item = AppSettings.get { Tmdb.Languages }
            .map { languages -> TmdbSeason(seasonId, languages.current) },
    ),
    val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(scope),
) {

    data class Details(
        val name: String,
        val overview: String?,
        val airDate: LocalDate?,
    )

    fun details(): State<ApiLoadable<Details>> {
        return flowCollector.collectFlow(
            flow = apiCallHelper.callApi { it.details }.map { result ->
                result.mapResult { tmdbSeasonDetails ->
                    Details(
                        name = tmdbSeasonDetails.name,
                        overview = tmdbSeasonDetails.overview,
                        airDate = tmdbSeasonDetails.airDate,
                    )
                }
            },
        )
    }
}
