package io.github.couchtracker.ui.screens.season

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.utils.api.ApiLoadable

class SeasonScreenViewModel(
    application: Application,
    val seasonId: TmdbSeasonId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = SeasonScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        seasonId = seasonId,
    )

    val details by baseViewModel.details()
    val showBaseDetails by baseViewModel.showViewModel.baseDetails()
    val colorScheme by baseViewModel.showViewModel.colorScheme()
    val subtitle get() = baseViewModel.subtitle(details, showBaseDetails)

    val allLoadables: List<ApiLoadable<*>> get() = baseViewModel.flowCollector.currentValues

    fun retryAll() {
        baseViewModel.retryAll()
    }
}
