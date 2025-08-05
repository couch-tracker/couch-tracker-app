package io.github.couchtracker.ui.screens.show

import android.content.Context
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
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
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.awaitAll
import io.github.couchtracker.utils.runApiCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

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
    val credits: DeferredApiResult<Credits>,
    val images: DeferredApiResult<List<ImageModel>>,
    val backdrop: ImageRequest?,
    val colorScheme: ColorScheme,
) {
    val allDeferred: Set<DeferredApiResult<*>> = setOf(credits, images)

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )
}

suspend fun CoroutineScope.loadShow(
    ctx: Context,
    tmdbCache: TmdbCache,
    show: TmdbShow,
    width: Int,
    height: Int,
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<ShowScreenModel> {
    return runApiCatching(LOG_TAG) {
        val images = async(coroutineContext) {
            runApiCatching(LOG_TAG) {
                show.images(tmdbCache)
                    .linearize()
                    .map { it.toImageModel(TmdbImageType.BACKDROP) }
            }
        }
        val credits = async(coroutineContext) {
            runApiCatching(LOG_TAG) {
                val credits = show.aggregateCredits(tmdbCache)
                ShowScreenModel.Credits(
                    cast = credits.cast.toCastPortraitModel(show.language),
                    crew = credits.crew.toCrewCompactListItemModel(show.language),
                )
            }
        }
        val details = show.details(tmdbCache)
        val backdrop = async(coroutineContext) {
            details.backdropImage.prepareAndExtractColorScheme(
                ctx = ctx,
                width = width,
                height = height,
                fallbackColorScheme = ColorSchemes.Show,
            )
        }

        // It can be disruptive to load in content at separate times.
        // If the other content loads "fast enough", I'll wait for it.
        listOf(images, credits).awaitAll(100.milliseconds)

        ShowScreenModel(
            id = show.id,
            name = details.name,
            tagline = details.tagline,
            overview = details.overview,
            year = details.firstAirDate?.year,
            rating = details.rating(),
            genres = details.genres,
            createdBy = details.createdBy.orEmpty(),
            credits = credits,
            backdrop = backdrop.await().first,
            images = images,
            colorScheme = backdrop.await().second,
        )
    }
}
