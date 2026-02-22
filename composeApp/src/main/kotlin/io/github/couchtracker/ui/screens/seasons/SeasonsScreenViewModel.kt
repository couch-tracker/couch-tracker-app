package io.github.couchtracker.ui.screens.seasons

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.utils.api.ApiLoadable

class SeasonsScreenViewModel(
    application: Application,
    showId: TmdbShowId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = SeasonsScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = showId,
    )

    val showDetails by baseViewModel.showDetails()
    val colorScheme by baseViewModel.colorScheme()

    val allLoadables: List<ApiLoadable<*>> get() = baseViewModel.flowCollector.currentValues

    class SeasonViewModel(
        helper: SeasonsScreenViewModelHelper.SeasonViewModelHelper,
    ) {
        val details by helper.details()
    }

    @Composable
    fun viewModelForSeason(season: TmdbSeasonId): SeasonViewModel {
        val helper = baseViewModel.viewModelForSeason(season)
        return SeasonViewModel(helper)
    }

    fun retryAll() {
        baseViewModel.retryAll()
    }
}
