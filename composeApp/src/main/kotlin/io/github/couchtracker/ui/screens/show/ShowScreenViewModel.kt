package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.aggregateErrorOrNull
import io.github.couchtracker.utils.resultErrorOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShowScreenViewModel(
    application: Application,
    val showId: TmdbShowId,
) : AndroidViewModel(
    application = application,
) {
    private val retryContext = tmdbFlowRetryContext()
    private val baseViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = showId,
        retryContext = retryContext,
    )

    val baseDetails by baseViewModel.baseDetails.collectAsLoadable("baseDetails", Dispatchers.Main)
    val fullDetails by baseViewModel.fullDetails.collectAsLoadable("fullDetails")
    val colorScheme by baseViewModel.colorScheme.collectAsLoadable("colorScheme")
    val images by baseViewModel.images.collectAsLoadable("images")
    val credits by baseViewModel.credits.collectAsLoadable("credits")

    val aggregateError: CouchTrackerError? by derivedStateOf {
        listOf(baseDetails, fullDetails, colorScheme, images, credits)
            .map { it.resultErrorOrNull() }
            .aggregateErrorOrNull()
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
