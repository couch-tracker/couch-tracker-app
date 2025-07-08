package io.github.couchtracker.ui.screens.show

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.model.TmdbFileImage
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import coil3.request.ImageRequest
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private const val LOG_TAG = "ShowScreenModel"

data class ShowScreenModel(
    val id: TmdbShowId,
    val name: String,
    val tagline: String,
    val overview: String,
    val year: Int?,
    val rating: TmdbRating?,
    val genres: List<TmdbGenre>,
    val createdBy: List<TmdbShowCreatedBy>,
    val cast: List<CastPortraitModel>,
    val crew: List<CrewCompactListItemModel>,
    val images: List<TmdbFileImage>,
    val backdrop: ImageRequest?,
    val colorScheme: ColorScheme,
)

suspend fun loadShow(
    ctx: Context,
    tmdbCache: TmdbCache,
    show: TmdbShow,
    width: Int,
    height: Int,
    coroutineContext: CoroutineContext = Dispatchers.Default,
): Loadable<ShowScreenModel, ApiException> {
    return try {
        coroutineScope {
            withContext(coroutineContext) {
                val details = show.details(tmdbCache)
                val images = async { show.images(tmdbCache) }
                val aggregateCredits = async { show.aggregateCredits(tmdbCache) }
                val backdrop = async {
                    details.backdropImage.prepareAndExtractColorScheme(
                        ctx = ctx,
                        width = width,
                        height = height,
                        fallbackColorScheme = ColorSchemes.Show,
                    )
                }
                Result.Value(
                    ShowScreenModel(
                        id = show.id,
                        name = details.name,
                        tagline = details.tagline,
                        overview = details.overview,
                        year = details.firstAirDate?.year,
                        rating = details.rating(),
                        genres = details.genres,
                        createdBy = details.createdBy.orEmpty(),
                        cast = aggregateCredits.await().cast.toCastPortraitModel(show.language, ImagePreloadOptions.DoNotPreload),
                        crew = aggregateCredits.await().crew.toCrewCompactListItemModel(show.language, ImagePreloadOptions.DoNotPreload),
                        backdrop = backdrop.await().first,
                        images = images.await().linearize(),
                        colorScheme = backdrop.await().second,
                    ),
                )
            }
        }
    } catch (e: ApiException) {
        Log.e(LOG_TAG, "Error while loading ShowScreenModel for ${show.id.toExternalId().serialize()} (${show.language})", e)
        Result.Error(e)
    }
}
