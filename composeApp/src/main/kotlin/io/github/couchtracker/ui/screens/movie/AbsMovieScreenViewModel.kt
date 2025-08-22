package io.github.couchtracker.ui.screens.movie

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.movie.ExternalMovieId
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.BaseTmdbMovie
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.ApiLoadableItemViewModel
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

abstract class AbsMovieScreenViewModel(
    application: Application,
    val externalMovieId: ExternalMovieId,
    val movieId: TmdbMovieId,
) : ApiLoadableItemViewModel<TmdbMovie>(
    application = application,
    item = AppSettings.get { Tmdb.Languages }
        .map { languages -> TmdbMovie(movieId, languages.current) },
) {
    data class BaseDetails(
        val title: String,
        val overview: String,
        val year: Int?,
        val backdrop: ImageModel?,
    )

    data class FullDetails(
        val baseDetails: BaseDetails,
        val genres: List<TmdbGenre>,
        val originalLanguage: Bcp47Language,
        val rating: TmdbRating?,
        val runtime: Duration?,
        val tagline: String,
    )

    private val baseAndFullDetails: Flow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> = callApiWithCache(
        cachedData = { getKoin().get<TmdbBaseMemoryCache>().getMovie(it)?.toBaseDetails() },
        fullDataFlow = {
            it.details.map { result -> result.map { details -> details.toDetails() } }
        },
    )
    val baseDetails: ApiLoadable<BaseDetails> by loadable(baseAndFullDetails.map { it.first })
    val fullDetails: ApiLoadable<FullDetails> by loadable(baseAndFullDetails.map { it.second })

    val colorScheme: ApiLoadable<ColorScheme?> by loadable(
        flow = baseAndFullDetails.map {
            it.first.mapResult { details ->
                details.backdrop?.extractColorScheme(application)
            }
        },
    )

    private suspend fun TmdbMovieDetail.toDetails(): Pair<BaseDetails, FullDetails> {
        val base = BaseDetails(
            title = title,
            overview = overview,
            year = releaseDate?.year,
            backdrop = backdropImage?.toImageModelWithPlaceholder(),
        )
        val full = FullDetails(
            baseDetails = base,
            tagline = tagline,
            rating = rating(),
            runtime = runtime(),
            originalLanguage = language(),
            genres = genres,
        )
        return base to full
    }

    private fun BaseTmdbMovie.toBaseDetails() = BaseDetails(
        title = title,
        overview = overview,
        year = releaseDate?.year,
        backdrop = backdrop?.toImageModelWithPlaceholder(),
    )
}
