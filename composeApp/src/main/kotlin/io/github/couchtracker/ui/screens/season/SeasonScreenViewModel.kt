package io.github.couchtracker.ui.screens.season

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.season.ExternalSeasonId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.utils.FlowToStateCollector
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull

class SeasonScreenViewModel(
    application: Application,
    val externalSeasonId: ExternalSeasonId,
    val seasonId: TmdbSeasonId,
) : AndroidViewModel(
    application = application,
) {
    private val flowCollector: FlowToStateCollector<ApiLoadable<*>> = FlowToStateCollector(viewModelScope)
    private val baseViewModel = SeasonScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        seasonId = seasonId,
        flowCollector = flowCollector,
    )
    private val showViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = seasonId.showId,
        flowCollector = flowCollector,
    )

    val details by baseViewModel.details()
    val showBaseDetails by showViewModel.baseDetails()
    val colorScheme by showViewModel.colorScheme()
    val subtitle: ApiLoadable<String?>
        get() {
            return details.mapResult { season ->
                val show = showBaseDetails.resultValueOrNull()
                if (season.displayDefaultName) {
                    if (show != null) {
                        show.name + " - " + season.defaultName
                    } else {
                        season.defaultName
                    }
                } else {
                    show?.name
                }
            }
        }

    val allLoadables: List<ApiLoadable<*>> get() = flowCollector.currentValues

    fun retryAll() {
        baseViewModel.apiCallHelper.retryAll()
        showViewModel.apiCallHelper.retryAll()
    }
}
