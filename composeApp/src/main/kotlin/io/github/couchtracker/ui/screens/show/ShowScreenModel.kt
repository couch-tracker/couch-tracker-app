package io.github.couchtracker.ui.screens.show

import android.content.Context
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.image.TmdbImageType
import app.moviebase.tmdb.model.TmdbAggregateCredits
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import app.moviebase.tmdb.model.TmdbShowDetail
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
import io.github.couchtracker.utils.CompletableApiResult
import io.github.couchtracker.utils.DeferredApiResult
import io.github.couchtracker.utils.awaitAll
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.onValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

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

suspend fun loadShow(
    ctx: Context,
    tmdbCache: TmdbCache,
    show: TmdbShow,
    width: Int,
    height: Int,
    coroutineContext: CoroutineContext = Dispatchers.Default,
): ApiResult<ShowScreenModel> = coroutineScope {
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
                cast = credits.cast.toCastPortraitModel(show.language),
                crew = credits.crew.toCrewCompactListItemModel(show.language),
            )
        }
    }
    details.await().map { details ->
        val backdrop = async(coroutineContext) {
            details.backdropImage.prepareAndExtractColorScheme(
                ctx = ctx,
                width = width,
                height = height,
                fallbackColorScheme = ColorSchemes.Show,
            )
        }
        ShowScreenModel(
            id = show.id,
            name = details.name,
            tagline = details.tagline,
            overview = details.overview,
            year = details.firstAirDate?.year,
            rating = details.rating(),
            genres = details.genres,
            createdBy = details.createdBy.orEmpty(),
            credits = creditsModel,
            backdrop = backdrop.await().first,
            images = imagesModel,
            colorScheme = backdrop.await().second,
        )
    }.onValue {
        // It can be disruptive to load in content at separate times.
        // If the other content loads "fast enough", I'll wait for it.
        it.allDeferred.awaitAll(100.milliseconds)
    }
}
