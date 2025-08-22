package io.github.couchtracker.ui.screens.watchedItem

import android.app.Application
import androidx.compose.material3.ColorScheme
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.screens.movie.AbsMovieScreenViewModel
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.mapResult
import kotlin.time.Duration

interface WatchedItemsScreenViewModel {

    data class Details(
        val title: String,
        val runtime: Duration?,
        val originalLanguage: Bcp47Language,
        val backdrop: ImageModel?,
    )

    val colorScheme: ApiLoadable<ColorScheme?>
    val watchableExternalMovieId: WatchableExternalId
    val details: ApiLoadable<Details>

    suspend fun reload()

    class Movie(
        application: Application,
        override val watchableExternalMovieId: WatchableExternalId.Movie,
        movieId: TmdbMovieId,
    ) : AbsMovieScreenViewModel(
        application = application,
        externalMovieId = watchableExternalMovieId.movieId,
        movieId = movieId,
    ),
        WatchedItemsScreenViewModel {
        override val details: ApiLoadable<Details>
            get() = super.fullDetails.mapResult { details ->
                Details(
                    title = details.baseDetails.title,
                    runtime = details.runtime,
                    originalLanguage = details.originalLanguage,
                    backdrop = details.baseDetails.backdrop,
                )
            }
    }
}
