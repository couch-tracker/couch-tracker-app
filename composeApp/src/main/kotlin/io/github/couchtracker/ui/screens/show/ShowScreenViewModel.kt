package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.utils.api.ApiLoadable

class ShowScreenViewModel(
    application: Application,
    val externalShowId: ExternalShowId,
    val showId: TmdbShowId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        movieId = showId,
    )

    val baseDetails by baseViewModel.baseDetails()
    val fullDetails by baseViewModel.fullDetails()
    val colorScheme by baseViewModel.colorScheme()
    val images by baseViewModel.images()
    val credits by baseViewModel.credits()

    val allLoadables: List<ApiLoadable<*>> get() = baseViewModel.flowCollector.currentValues

    fun retryAll() {
        baseViewModel.apiCallHelper.retryAll()
    }
}
