package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.aggregateErrorOrNull
import io.github.couchtracker.utils.resultErrorOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieScreenViewModel(
    application: Application,
    val movieId: TmdbMovieId,
) : AndroidViewModel(
    application = application,
) {
    private val retryContext = tmdbFlowRetryContext()
    private val baseViewModel = MovieScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        movieId = movieId,
        retryContext = retryContext,
    )

    val baseDetails by baseViewModel.baseDetails.collectAsLoadable(debugLog = "baseDetails", Dispatchers.Main)
    val fullDetails by baseViewModel.fullDetails.collectAsLoadable(debugLog = "fullDetails")
    val colorScheme by baseViewModel.colorScheme.collectAsLoadable(debugLog = "colorScheme")
    val images by baseViewModel.images.collectAsLoadable(debugLog = "images")
    val credits by baseViewModel.credits.collectAsLoadable(debugLog = "credits")

    val aggregateError: CouchTrackerError? by derivedStateOf {
        listOf(baseDetails, fullDetails, colorScheme, images, credits)
            .map { it.resultErrorOrNull() }
            .aggregateErrorOrNull()
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
