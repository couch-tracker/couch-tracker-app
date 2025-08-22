package io.github.couchtracker.ui.screens.show

import android.content.Context
import android.util.Log
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import io.github.couchtracker.R
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.linearize
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.ifError
import io.github.couchtracker.utils.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatform
import kotlin.coroutines.CoroutineContext

data class ShowScreenModel(
    val id: TmdbShowId,
    val name: String,
    val overview: String,
    val year: Int?,
    val rating: TmdbRating?,
    val fullDetails: DeferredApiResult<FullDetails>,
    val credits: DeferredApiResult<Credits>,
    val images: DeferredApiResult<List<ImageModel>>,
    val backdrop: ImageModel?,
    val colorScheme: ColorScheme,
) {
    val allDeferred: Set<DeferredApiResult<*>> = setOf(credits, images)

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

@Suppress("LongParameterList")
suspend fun CoroutineScope.loadShow(
    ctx: Context,
    show: TmdbShow,
    tmdbBaseMemoryCache: TmdbBaseMemoryCache = KoinPlatform.getKoin().get(),
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<ShowScreenModel> {
    val baseDetailsMemory = tmdbBaseMemoryCache.getShow(show)

    val imagesModel = async(coroutineContext) {
        show.images.first().map { images ->
            images
                .linearize()
                .map { img -> img.toImageModel(TmdbImageType.BACKDROP) }
        }
    }
    val creditsModel = async(coroutineContext) {
        show.aggregateCredits.first().map { credits ->
            ShowScreenModel.Credits(
                cast = credits.cast.toCastPortraitModel(ctx),
                crew = credits.crew.toCrewCompactListItemModel(ctx),
            )
        }
    }
    val fullDetails = async(coroutineContext) { show.details.first() }
    val fullDetailsModel = async(coroutineContext) {
        fullDetails.await().map { details ->
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
        fullDetails.await().map { details ->
            details.toBaseShow(show.languages.apiLanguage)
        }.ifError { return Result.Error(it) }
    }
    val backdrop = async(coroutineContext) {
        baseDetails.backdrop.prepareAndExtractColorScheme(ctx = ctx, fallbackColorScheme = ColorSchemes.Show)
    }
    val ret = ShowScreenModel(
        id = show.id,
        name = baseDetails.name,
        overview = baseDetails.overview,
        year = baseDetails.firstAirDate?.year,
        rating = baseDetails.rating(),
        fullDetails = fullDetailsModel,
        credits = creditsModel,
        backdrop = backdrop.await().first,
        images = imagesModel,
        colorScheme = backdrop.await().second,
    )
    return Result.Value(ret)
}
