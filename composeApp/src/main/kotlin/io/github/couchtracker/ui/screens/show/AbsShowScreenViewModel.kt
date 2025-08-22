package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.application
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.ApiLoadableItemViewModel
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class AbsShowScreenViewModel(
    application: Application,
    val externalShowId: ExternalShowId,
    val showId: TmdbShowId,
) : ApiLoadableItemViewModel<TmdbShow>(
    application = application,
    item = AppSettings.get { Tmdb.Languages }
        .map { languages -> TmdbShow(showId, languages.current) },
) {

    data class BaseDetails(
        val name: String,
        val overview: String,
        val year: Int?,
        val backdrop: ImageModel?,
    )

    data class FullDetails(
        val baseDetails: BaseDetails,
        val createdBy: List<TmdbShowCreatedBy>,
        val createdByString: String?,
        val genres: List<TmdbGenre>,
        val originalLanguage: Bcp47Language,
        val rating: TmdbRating?,
        val tagline: String,
    )

    private val baseAndFullDetails: Flow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> = callApiWithCache(
        cachedData = { getKoin().get<TmdbBaseMemoryCache>().getShow(it)?.toBaseDetails() },
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

    private suspend fun TmdbShowDetail.toDetails(): Pair<BaseDetails, FullDetails> {
        val base = BaseDetails(
            name = name,
            overview = overview,
            year = firstAirDate?.year,
            backdrop = backdropImage?.toImageModelWithPlaceholder(),
        )
        val createdBy = createdBy.orEmpty()
        val full = FullDetails(
            baseDetails = base,
            tagline = tagline,
            rating = rating(),
            originalLanguage = language(),
            genres = genres,
            createdBy = createdBy,
            createdByString = if (createdBy.isEmpty()) {
                null
            } else {
                application.getString(R.string.show_by_creator, formatAndList(createdBy.map { it.name }))
            },
        )
        return base to full
    }

    private fun BaseTmdbShow.toBaseDetails(): BaseDetails = BaseDetails(
        name = name,
        overview = overview,
        year = firstAirDate?.year,
        backdrop = backdrop?.toImageModelWithPlaceholder(),
    )
}
