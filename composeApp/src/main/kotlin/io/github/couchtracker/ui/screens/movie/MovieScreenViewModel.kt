package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.utils.ApiLoadable

class MovieScreenViewModel(
    application: Application,
    val externalMovieId: ExternalMovieId,
    val movieId: TmdbMovieId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = MovieScreenModelBuilder(
        application = application,
        scope = viewModelScope,
        movieId = movieId,
    )

    val baseDetails by baseViewModel.baseDetails()
    val fullDetails by baseViewModel.fullDetails()
    val colorScheme by baseViewModel.colorScheme()
    val images by baseViewModel.images()
    val credits by baseViewModel.credits()

    val allLoadables: List<ApiLoadable<*>> get() = baseViewModel.flowCollector.currentValues

    suspend fun retryAll() {
        baseViewModel.apiCallHelper.retryAll()
    }
}
