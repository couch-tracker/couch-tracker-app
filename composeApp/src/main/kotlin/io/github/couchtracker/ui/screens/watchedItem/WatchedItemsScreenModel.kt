package io.github.couchtracker.ui.screens.watchedItem

import android.content.Context
import androidx.compose.material3.ColorScheme
import coil3.request.ImageRequest
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.WatchableExternalId
import io.github.couchtracker.db.profile.asWatchable
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.prepareAndExtractColorScheme
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.utils.ApiResult
import io.github.couchtracker.utils.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

data class WatchedItemsScreenModel(
    val id: WatchableExternalId,
    val itemType: WatchedItemType,
    val title: String,
    val runtime: Duration?,
    val originalLanguage: Bcp47Language,
    val backdrop: ImageRequest?,
    val colorScheme: ColorScheme,
) {

    companion object {
        suspend fun loadTmdbMovie(
            context: Context,
            tmdbCache: TmdbCache,
            movie: TmdbMovie,
            width: Int,
            height: Int,
            coroutineContext: CoroutineContext = Dispatchers.Default,
        ): ApiResult<WatchedItemsScreenModel> = coroutineScope {
            movie.details(tmdbCache).map { details ->
                val backdrop = async(coroutineContext) {
                    details.backdropImage.prepareAndExtractColorScheme(
                        ctx = context,
                        width = width,
                        height = height,
                        fallbackColorScheme = ColorSchemes.Movie,
                    )
                }
                WatchedItemsScreenModel(
                    id = movie.id.toExternalId().asWatchable(),
                    itemType = WatchedItemType.MOVIE,
                    title = details.title,
                    runtime = details.runtime(),
                    originalLanguage = Bcp47Language.of(details.originalLanguage),
                    backdrop = backdrop.await().first,
                    colorScheme = backdrop.await().second,
                )
            }
        }
    }
}
