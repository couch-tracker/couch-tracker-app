package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.allErrors
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import kotlinx.coroutines.launch

class MovieScreenViewModel(
    application: Application,
    val externalMovieId: ExternalMovieId,
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

    val baseDetails by baseViewModel.baseDetails.collectAsLoadable()
    val fullDetails by baseViewModel.fullDetails.collectAsLoadable()
    val colorScheme by baseViewModel.colorScheme.collectAsLoadable()
    val images by baseViewModel.images.collectAsLoadable()
    val credits by baseViewModel.credits.collectAsLoadable()

    val allErrors: List<CouchTrackerError> by derivedStateOf {
        listOf(baseDetails, fullDetails, colorScheme, images, credits).allErrors()
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
