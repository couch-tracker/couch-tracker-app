package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId

class WatchedEpisodeSessionsScreenViewModel(
    application: Application,
    val showId: TmdbShowId,
    val externalShowId: ExternalShowId,
) : AndroidViewModel(application) {

    private val baseViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = showId,
    )

    val fullDetails by baseViewModel.fullDetails()
    val colorScheme by baseViewModel.colorScheme()

    fun retryAll() {
        baseViewModel.retryAll()
    }
}
