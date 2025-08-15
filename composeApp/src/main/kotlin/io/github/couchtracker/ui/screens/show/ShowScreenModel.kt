package io.github.couchtracker.ui.screens.show

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbAggregateCredits
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import app.moviebase.tmdb.model.TmdbShowDetail
import coil3.request.ImageRequest
import io.github.couchtracker.R
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbMemoryCache
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.prepareMainImageRequest
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.CompletableApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.awaitWithTimeout
import io.github.couchtracker.utils.ifError
import io.github.couchtracker.utils.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val LOADING_TIME_CUTOFF = 25.milliseconds

data class ShowScreenModel(
    val id: TmdbShowId,
    val name: String,
    val overview: String,
    val year: Int?,
    val rating: TmdbRating?,
    val fullDetails: DeferredApiResult<FullDetails>,
    val credits: DeferredApiResult<Credits>,
    val images: DeferredApiResult<List<ImageModel>>,
    val backdrop: ImageRequest?,
    val colorScheme: Deferred<ColorScheme>,
) {
    val allDeferred: Set<DeferredApiResult<*>> = setOf(fullDetails, credits, images)

    data class FullDetails(
        val tagline: String,
        val genres: List<TmdbGenre>,
        val createdBy: List<TmdbShowCreatedBy>,
        val createdByString: String?,
    )

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
    val startingTime = TimeSource.Monotonic.markNow()
    val baseDetailsMemory = TmdbMemoryCache.getShow(show)
    val details = CompletableApiResult<TmdbShowDetail>()
    val credits = CompletableApiResult<TmdbAggregateCredits>()
    val images = CompletableApiResult<TmdbImages>()
    launch(coroutineContext) {
        show.details(cache = tmdbCache, details = details, aggregateCredits = credits, images = images)
    }

    val imagesModel = async(coroutineContext) {
        images.await().map { images ->
            images
                .linearize()
                .map { img -> img.toImageModel(TmdbImageType.BACKDROP) }
        }
    }
    val creditsModel = async(coroutineContext) {
        credits.await().map { credits ->
            ShowScreenModel.Credits(
                cast = credits.cast.toCastPortraitModel(ctx, show.language),
                crew = credits.crew.toCrewCompactListItemModel(ctx, show.language),
            )
        }
    }
    val fullDetailsModel = async(coroutineContext) {
        details.await().map { details ->
            val createdBy = details.createdBy.orEmpty()
            ShowScreenModel.FullDetails(
                tagline = details.tagline,
                genres = details.genres,
                createdBy = createdBy,
                createdByString = if (createdBy.isEmpty()) {
                    null
                } else {
                    ctx.getString(R.string.show_by_creator, formatAndList(createdBy.map { it.name }))
                },
            )
        }
    }
    val baseDetails = if (baseDetailsMemory != null) {
        baseDetailsMemory
    } else {
        Log.w("Cache miss", "Show $show not found in cache")
        details.await().map { details ->
            details.toBaseShow(show.language)
        }.ifError { return Result.Error(it) }
    }
    val colorScheme = async(coroutineContext) {
        baseDetails.backdrop?.prepareAndExtractColorScheme(ctx)?.generateColorScheme() ?: ColorSchemes.Show
    }
    val ret = ShowScreenModel(
        id = show.id,
        name = baseDetails.name,
        overview = baseDetails.overview,
        year = baseDetails.firstAirDate?.year,
        rating = baseDetails.rating(),
        fullDetails = fullDetailsModel,
        credits = creditsModel,
        backdrop = baseDetails.backdrop?.prepareMainImageRequest(ctx, width, height),
        images = imagesModel,
        colorScheme = colorScheme,
    )
    listOf(ret.fullDetails, ret.colorScheme).awaitWithTimeout(startingTime, LOADING_TIME_CUTOFF)
    return Result.Value(ret)
}
