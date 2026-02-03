package io.github.couchtracker.ui.screens.watchedItem

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.screens.movie.MovieScreenViewModelHelper
import io.github.couchtracker.utils.api.ApiLoadable
import io.github.couchtracker.utils.mapResult
import kotlin.time.Duration

interface WatchedItemsScreenViewModel {

    data class Details(
        val title: String,
        val runtime: Duration?,
        val originalLanguage: Bcp47Language?,
        val backdrop: ImageModel?,
    )

    val colorScheme: ApiLoadable<ColorScheme?>
    val watchableExternalMovieId: WatchableExternalId
    val details: ApiLoadable<Details>

    fun retryAll()

    class Movie(
        application: Application,
        override val watchableExternalMovieId: WatchableExternalId.Movie,
        movieId: TmdbMovieId,
    ) : AndroidViewModel(application = application), WatchedItemsScreenViewModel {

        private val baseViewModel = MovieScreenViewModelHelper(
            application = application,
            scope = viewModelScope,
            movieId = movieId,
        )
        private val movieDetails by baseViewModel.fullDetails()
        override val details by derivedStateOf {
            movieDetails.mapResult { details ->
                Details(
                    title = details.baseDetails.title,
                    runtime = details.runtime,
                    originalLanguage = details.originalLanguage,
                    backdrop = details.baseDetails.backdrop,
                )
            }
        }
        override val colorScheme by baseViewModel.colorScheme()

        override fun retryAll() {
            baseViewModel.apiCallHelper.retryAll()
        }
    }
}
