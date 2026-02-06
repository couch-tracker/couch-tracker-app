package io.github.couchtracker.ui.screens.season

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.season.ExternalSeasonId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.utils.api.ApiLoadable

class SeasonScreenViewModel(
    application: Application,
    val externalSeasonId: ExternalSeasonId,
    val seasonId: TmdbSeasonId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = SeasonScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        seasonId = seasonId,
    )
    private val showViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = seasonId.showId,
    )

    val details by baseViewModel.details()
    val showBaseDetails by showViewModel.baseDetails()
    val colorScheme by showViewModel.colorScheme()

    val allLoadables: List<ApiLoadable<*>>
        get() = baseViewModel.flowCollector.currentValues + showViewModel.flowCollector.currentValues

    fun retryAll() {
        baseViewModel.apiCallHelper.retryAll()
        showViewModel.apiCallHelper.retryAll()
    }
}
