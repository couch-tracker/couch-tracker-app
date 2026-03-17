package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.collectAsLoadable
import kotlinx.coroutines.launch

class WatchedEpisodeSessionsScreenViewModel(
    application: Application,
    val showId: TmdbShowId,
    val externalShowId: ExternalShowId,
) : AndroidViewModel(application) {

    private val retryContext = tmdbFlowRetryContext()
    private val baseViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = showId,
        retryContext = retryContext,
    )

    val fullDetails by baseViewModel.fullDetails.collectAsLoadable()
    val colorScheme by baseViewModel.colorScheme.collectAsLoadable()

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
