package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.moviebase.tmdb.image.TmdbImageType
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.screens.movie.MovieScreenModelBuilder
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.mapResult
import kotlinx.coroutines.flow.map

class ShowScreenViewModel(
    application: Application,
    val externalShowId: ExternalShowId,
    val showId: TmdbShowId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = ShowScreenModelBuilder(
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

    suspend fun retryAll() {
        baseViewModel.apiCallHelper.retryAll()
    }
}
