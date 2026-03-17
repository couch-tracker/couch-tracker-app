package io.github.couchtracker.ui.screens.watchedItem

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.screens.movie.MovieScreenViewModelHelper
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.mapResult
import kotlinx.coroutines.launch
import kotlin.time.Duration

sealed interface WatchedItemsScreenViewModel {

    data class Details(
        val title: String?,
        val runtime: Duration?,
        val originalLanguage: Bcp47Language?,
        val backdrop: ImageModel?,
    )

    val colorScheme: ApiLoadable<ColorScheme?>
    val externalId: ExternalId
    val details: ApiLoadable<Details>
    val watchedItemType: WatchedItemType

    fun retryAll()

    class Movie(
        application: Application,
        movieId: TmdbMovieId,
    ) : AndroidViewModel(application = application), WatchedItemsScreenViewModel {

        override val externalId = TmdbExternalMovieId(movieId)

        private val retryContext = tmdbFlowRetryContext()
        private val baseViewModel = MovieScreenViewModelHelper(
            application = application,
            scope = viewModelScope,
            movieId = movieId,
            retryContext = retryContext,
        )
        private val movieDetails by baseViewModel.fullDetails.collectAsLoadable()
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
        override val watchedItemType get() = WatchedItemType.MOVIE
        override val colorScheme by baseViewModel.colorScheme.collectAsLoadable()

        override fun retryAll() {
            viewModelScope.launch { retryContext.retryAll() }
        }
    }
}
